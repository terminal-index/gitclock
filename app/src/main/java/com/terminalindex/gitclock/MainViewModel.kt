package com.terminalindex.gitclock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.terminalindex.gitclock.data.ComponentId
import com.terminalindex.gitclock.data.ComponentLayout
import com.terminalindex.gitclock.data.GitClockRepository
import com.terminalindex.gitclock.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userPrefs = UserPreferences(application)
    private val repository = GitClockRepository()
    private val _screenSize = MutableStateFlow<Pair<Int, Int>>(1920 to 1080) 
    private val webServer = com.terminalindex.gitclock.server.GitClockServer(application, userPrefs, viewModelScope, _screenSize.asStateFlow())

    val username = userPrefs.username.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val token = userPrefs.token.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val oledMode = userPrefs.oledMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val batteryStyle = userPrefs.batteryStyle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val firstLaunch = userPrefs.firstLaunch.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _graphState = MutableStateFlow<List<Int>>(emptyList())
    val graphState: StateFlow<List<Int>> = _graphState

    data class StatsState(
        val prCount: Int = 0,
        val issueCount: Int = 0,
        val notificationCount: Int = 0,
        val avatarUrl: String? = null
    )
    
    private val _statsState = MutableStateFlow(StatsState())
    val statsState: StateFlow<StatsState> = _statsState
    
    private val _lastSync = MutableStateFlow<String>("Never")
    val lastSync: StateFlow<String> = _lastSync

    val isConfigured = userPrefs.sessionActive.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setOledMode(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.saveOledMode(enabled)
        }
    }
    
    fun setBatteryStyle(style: Int) {
        viewModelScope.launch {
            userPrefs.saveBatteryStyle(style)
        }
    }
    
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.saveKeepScreenOn(enabled)
        }
    }

    fun setServerEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.saveServerEnabled(enabled) }
    }
    
    fun setFirstLaunchCompleted() {
        viewModelScope.launch {
            userPrefs.setFirstLaunchCompleted()
        }
    }
    
    private val _layoutState = MutableStateFlow<Map<ComponentId, ComponentLayout>>(emptyMap())
    val layoutState: StateFlow<Map<ComponentId, ComponentLayout>> = _layoutState.asStateFlow()

    val keepScreenOn = userPrefs.keepScreenOn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isServerEnabled = userPrefs.isServerEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val gson = Gson()

    init {
        viewModelScope.launch {
            isServerEnabled.collect { enabled ->
                if (enabled) webServer.start() else webServer.stop()
            }
        }
        
        viewModelScope.launch {
            userPrefs.layoutConfig.collect { json ->
                if (!json.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<Map<ComponentId, ComponentLayout>>() {}.type
                        val map: Map<ComponentId, ComponentLayout> = gson.fromJson(json, type)
                        _layoutState.value = map
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    _layoutState.value = getDefaultLayout()
                }
            }
        }
    }

    private fun getDefaultLayout(): Map<ComponentId, ComponentLayout> {
        return mapOf(
            ComponentId.BATTERY to ComponentLayout(ComponentId.BATTERY, 0f, 0f), 
            ComponentId.CLOCK to ComponentLayout(ComponentId.CLOCK, 0f, 200f), 
            ComponentId.STATS to ComponentLayout(ComponentId.STATS, 0f, -350f), 
            ComponentId.COMMIT_BOARD to ComponentLayout(ComponentId.COMMIT_BOARD, 0f, 0f) 
        )
    }

    fun updateScreenSize(width: Int, height: Int) {
        _screenSize.value = width to height
    }

    fun updateComponent(id: ComponentId, x: Float, y: Float, scale: Float) {
        val current = _layoutState.value[id] ?: ComponentLayout(id)
        val updated = current.copy(x = x, y = y, scale = scale)
        _layoutState.value = _layoutState.value + (id to updated)
    }

    fun saveCurrentLayout() {
        viewModelScope.launch {
            val json = gson.toJson(_layoutState.value)
            userPrefs.saveLayout(json)
        }
    }

    fun saveCredentials(user: String, newToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalUser = user
            if (finalUser.isBlank() && newToken.isNotBlank()) {
                try {
                    finalUser = repository.fetchUser(newToken)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@launch
                }
            }
            
            userPrefs.saveCredentials(finalUser, newToken)
            fetchData() 
        }
    }

    fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (isConfigured.value) {
                    fetchData()
                }
                delay(60_000)
            }
        }
    }

    private suspend fun fetchData() = withContext(Dispatchers.IO) {
        try {
            val user = username.value ?: return@withContext
            val tok = token.value
            
            val gitHubData = repository.fetchContributions(user, tok)
            val days = gitHubData.contributions
            
            val cleanData = if (days.size >= 364) {
                days.takeLast(364)
            } else {
                List(364 - days.size) { com.terminalindex.gitclock.data.ContributionDay("", 0, 0) } + days
            }

            val maxCount = cleanData.maxOfOrNull { it.count } ?: 1
            val mappedIntensities = cleanData.map { day ->
                if (day.level > 0) day.level
                else {
                    if (day.count == 0) 0
                    else {
                        val ratio = day.count.toFloat() / maxCount
                        when {
                            ratio <= 0.25 -> 1
                            ratio <= 0.50 -> 2
                            ratio <= 0.75 -> 3
                            else -> 4
                        }
                    }
                }
            }
            
            _graphState.value = mappedIntensities
            _statsState.value = StatsState(
                prCount = gitHubData.prCount,
                issueCount = gitHubData.issueCount,
                notificationCount = 0,
                avatarUrl = gitHubData.avatarUrl
            )
            _lastSync.value = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        webServer.stop()
    }

    fun clearSettings() {
        viewModelScope.launch {
            userPrefs.logout()
        }
    }
}

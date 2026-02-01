package com.terminalindex.gitclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_OLED_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("oled_mode")
        val KEY_BATTERY_STYLE = androidx.datastore.preferences.core.intPreferencesKey("battery_style")
        val KEY_SESSION_ACTIVE = androidx.datastore.preferences.core.booleanPreferencesKey("session_active")
        val KEY_FIRST_LAUNCH = androidx.datastore.preferences.core.booleanPreferencesKey("first_launch")
        val KEY_KEEP_SCREEN_ON = androidx.datastore.preferences.core.booleanPreferencesKey("keep_screen_on")
        val KEY_WEB_SERVER_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("web_server_enabled")
    }

    val username: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[KEY_USERNAME] }

    val token: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[KEY_TOKEN] }
        
    val sessionActive: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_SESSION_ACTIVE] ?: false }

    val oledMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_OLED_MODE] ?: false }

    val batteryStyle: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[KEY_BATTERY_STYLE] ?: 0 } 

    val firstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_FIRST_LAUNCH] ?: true }

    val keepScreenOn: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_KEEP_SCREEN_ON] ?: true }

    suspend fun saveOledMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OLED_MODE] = enabled
        }
    }

    suspend fun saveBatteryStyle(style: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BATTERY_STYLE] = style
        }
    }
    
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH] = false
        }
    }
    
    private val KEY_LAYOUT = stringPreferencesKey("layout_config")

    val layoutConfig: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAYOUT]
    }

    suspend fun saveLayout(layoutJson: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAYOUT] = layoutJson
        }
    }
    
    suspend fun saveKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KEEP_SCREEN_ON] = enabled
        }
    }

    val isServerEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_WEB_SERVER_ENABLED] ?: false }

    suspend fun saveServerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEB_SERVER_ENABLED] = enabled
        }
    }

    suspend fun saveCredentials(username: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USERNAME] = username
            preferences[KEY_TOKEN] = token 
            preferences[KEY_SESSION_ACTIVE] = true
        }
    }
    
    suspend fun logout() {
        context.dataStore.edit { preferences -> 
            preferences[KEY_SESSION_ACTIVE] = false
        }
    }
}

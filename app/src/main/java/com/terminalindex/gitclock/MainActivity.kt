package com.terminalindex.gitclock

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.border
import com.terminalindex.gitclock.data.ComponentId
import com.terminalindex.gitclock.data.ComponentLayout
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.terminalindex.gitclock.ui.SetupScreen
import com.terminalindex.gitclock.ui.theme.GitclockTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        enableEdgeToEdge()
        
        viewModel.startPolling()

        setContent {
            GitclockTheme(dynamicColor = true) {
                val isConfigured by viewModel.isConfigured.collectAsState()
                val graphState by viewModel.graphState.collectAsState()
                val statsState by viewModel.statsState.collectAsState()
                val oledMode by viewModel.oledMode.collectAsState()
                val batteryStyle by viewModel.batteryStyle.collectAsState()
                val firstLaunch by viewModel.firstLaunch.collectAsState()
                val keepScreenOn by viewModel.keepScreenOn.collectAsState()
                val isServerEnabled by viewModel.isServerEnabled.collectAsState()
                val context = LocalContext.current
                
                LaunchedEffect(keepScreenOn) {
                    if (keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                LaunchedEffect(isConfigured) {
                    val activity = context as? Activity
                    if (isConfigured) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (oledMode && isConfigured) Color.Black else MaterialTheme.colorScheme.background
                ) {
                    Crossfade(targetState = isConfigured, label = "ScreenTransition") { configured ->
                        if (configured) {
                            GitClockApp(
                                graphData = graphState, 
                                stats = statsState,
                                username = viewModel.username.collectAsState().value ?: "",
                                oledMode = oledMode,
                                batteryStyle = batteryStyle,
                                firstLaunch = firstLaunch,
                                keepScreenOn = keepScreenOn,
                                isServerEnabled = isServerEnabled,
                                isAuthenticated = !viewModel.token.collectAsState().value.isNullOrBlank(),
                                layoutState = viewModel.layoutState.collectAsState().value,
                                onUpdateLayout = { id, x, y, s -> viewModel.updateComponent(id, x, y, s) },
                                onSaveLayout = { viewModel.saveCurrentLayout() },
                                onOpenSettings = {}, 
                                onToggleOled = { viewModel.setOledMode(it) },
                                onSetBatteryStyle = { viewModel.setBatteryStyle(it) },
                                onToggleKeepScreenOn = { viewModel.setKeepScreenOn(it) },
                                onToggleServer = { viewModel.setServerEnabled(it) },
                                onUpdateScreenSize = { w, h -> viewModel.updateScreenSize(w, h) },
                                onDismissFirstLaunch = { viewModel.setFirstLaunchCompleted() },
                                onLogout = { viewModel.clearSettings() }
                            )
                        } else {
                            SetupScreen(
                                initialUsername = viewModel.username.collectAsState().value,
                                initialToken = viewModel.token.collectAsState().value,
                                firstLaunch = firstLaunch,
                                onSave = { user, token -> 
                                    viewModel.saveCredentials(user, token)
                                },
                                onDismissFirstLaunch = { viewModel.setFirstLaunchCompleted() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GitClockApp(
    graphData: List<Int>,
    stats: MainViewModel.StatsState,
    username: String,
    oledMode: Boolean,
    batteryStyle: Int,
    firstLaunch: Boolean,
    keepScreenOn: Boolean,
    isServerEnabled: Boolean,
    isAuthenticated: Boolean,
    layoutState: Map<ComponentId, ComponentLayout>,
    onUpdateLayout: (ComponentId, Float, Float, Float) -> Unit,
    onSaveLayout: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleOled: (Boolean) -> Unit,
    onSetBatteryStyle: (Int) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleServer: (Boolean) -> Unit,
    onUpdateScreenSize: (Int, Int) -> Unit,
    onDismissFirstLaunch: () -> Unit,
    onLogout: () -> Unit
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val density = LocalDensity.current
        LaunchedEffect(maxWidth, maxHeight) {
             val w = with(density) { maxWidth.toPx().toInt() }
             val h = with(density) { maxHeight.toPx().toInt() }
             onUpdateScreenSize(w, h)
        }
        
        DraggableComponent(
            id = ComponentId.BATTERY,
            layoutState = layoutState,
            editMode = editMode,
            onUpdate = onUpdateLayout,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ConnectivityStatus(oledMode = oledMode)
                BatteryStatus(oledMode = oledMode, style = batteryStyle)
            }
        }

        DraggableComponent(
            id = ComponentId.CLOCK,
            layoutState = layoutState,
            editMode = editMode,
            onUpdate = onUpdateLayout,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
             Column(horizontalAlignment = Alignment.End) { 
                 ClockWidget(oledMode = oledMode)
             }
        }
        
        if (isAuthenticated && stats.avatarUrl != null) {
             DraggableComponent(
                id = ComponentId.STATS,
                layoutState = layoutState,
                editMode = editMode,
                onUpdate = onUpdateLayout,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItemIcon(Icons.AutoMirrored.Filled.CallSplit, stats.prCount, oledMode)
                    StatItemIcon(Icons.Filled.Adjust, stats.issueCount, oledMode)
                    StatItemIcon(Icons.Filled.Notifications, stats.notificationCount, oledMode)
                }
            }
        }

        DraggableComponent(
            id = ComponentId.COMMIT_BOARD,
            layoutState = layoutState,
            editMode = editMode,
            onUpdate = onUpdateLayout,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
             CommitHistoryBoard(
                data = graphData, 
                username = username,
                avatarUrl = stats.avatarUrl,
                oledMode = oledMode
            )
        }
        
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), 
            tonalElevation = 4.dp
        ) {
            IconButton(
                onClick = { showSettingsDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (oledMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        if (editMode) {
             Text(
                "EDIT MODE", 
                color = Color.Red, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            
            FloatingActionButton(
                onClick = { 
                    editMode = false
                    onSaveLayout() 
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save Layout")
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            oledMode = oledMode,
            batteryStyle = batteryStyle,
            keepScreenOn = keepScreenOn,
            isServerEnabled = isServerEnabled,
            editMode = editMode,
            onToggleOled = onToggleOled,
            onSetBatteryStyle = onSetBatteryStyle,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
            onToggleServer = onToggleServer,
            onToggleEditMode = { 
                editMode = it 
                if (!it) onSaveLayout()
            },
            onDismiss = { showSettingsDialog = false },
            onLogout = {
                showSettingsDialog = false
                onLogout()
            }
        )
    }
}


@Composable
fun DraggableComponent(
    id: ComponentId,
    layoutState: Map<ComponentId, ComponentLayout>,
    editMode: Boolean,
    onUpdate: (ComponentId, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val layout = layoutState[id] ?: ComponentLayout(id)
    var offsetX by remember(layout.x) { mutableFloatStateOf(layout.x) }
    var offsetY by remember(layout.y) { mutableFloatStateOf(layout.y) }
    var scale by remember(layout.scale) { mutableFloatStateOf(layout.scale) }

    val dragModifier = if (editMode) {
        Modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offsetX += pan.x
                    offsetY += pan.y
                    onUpdate(id, offsetX, offsetY, scale)
                }
            }
            .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(dragModifier)
    ) {
        content()
    }
}


@Composable
fun StatItemIcon(icon: ImageVector, count: Int, oledMode: Boolean) {
    val contentColor = if (oledMode) Color.LightGray else MaterialTheme.colorScheme.onSurface
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count", 
            fontWeight = FontWeight.Bold, 
            color = if (oledMode) Color.White else MaterialTheme.colorScheme.primary,
            fontSize = 18.sp
        )
    }
}

@Composable
fun SettingsDialog(
    oledMode: Boolean,
    batteryStyle: Int,
    keepScreenOn: Boolean,
    isServerEnabled: Boolean,
    editMode: Boolean,
    onToggleOled: (Boolean) -> Unit,
    onSetBatteryStyle: (Int) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleServer: (Boolean) -> Unit,
    onToggleEditMode: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                SettingsToggle(
                    label = "Edit Layout",
                    desc = "Drag & Drop, Pinch to Resize",
                    checked = editMode,
                    onChecked = onToggleEditMode
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingsToggle(
                    label = "Remote Config Server",
                    desc = "Enable Editor at http://IP:8080",
                    checked = isServerEnabled,
                    onChecked = onToggleServer
                )
                
                SettingsToggle(
                    label = "OLED Mode",
                    desc = "Pure black background",
                    checked = oledMode,
                    onChecked = onToggleOled
                )
                
                SettingsToggle(
                    label = "Keep Screen On",
                    desc = "Prevent sleeping",
                    checked = keepScreenOn,
                    onChecked = onToggleKeepScreenOn
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Battery Style", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StyleOption(selected = batteryStyle == 0, onClick = { onSetBatteryStyle(0) }, label = "Default")
                    StyleOption(selected = batteryStyle == 1, onClick = { onSetBatteryStyle(1) }, label = "iOS")
                    StyleOption(selected = batteryStyle == 2, onClick = { onSetBatteryStyle(2) }, label = "Circle")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onLogout) {
                Text("Logout / Reset", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun SettingsToggle(label: String, desc: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onChecked(it) })
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StyleOption(selected: Boolean, onClick: () -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BatteryStatus(modifier: Modifier = Modifier, oledMode: Boolean, style: Int) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(getBatteryLevel(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            batteryLevel = getBatteryLevel(context)
            delay(60_000)
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BatteryIcon(level = batteryLevel, oledMode = oledMode, style = style)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$batteryLevel%",
            color = if (oledMode) Color.White else MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BatteryIcon(level: Int, oledMode: Boolean, style: Int) {
    val safeLevel = level.coerceIn(0, 100)
    val color = if (oledMode) Color.White else MaterialTheme.colorScheme.onBackground
    val lowColor = if (oledMode) Color.Red else MaterialTheme.colorScheme.error
    val goodColor = if (oledMode) Color.Green else MaterialTheme.colorScheme.primary
    val fillColor = if (safeLevel <= 20) lowColor else goodColor

    Canvas(modifier = Modifier.size(width = if(style==2) 24.dp else 36.dp, height = if(style==2) 24.dp else 18.dp)) {
        when (style) {
            1 -> { 
                val strokeWidth = 1.5.dp.toPx()
                val w = size.width
                val h = size.height
                val cornerRadius = h / 2.5f
                
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.5f), 
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cornerRadius)
                )
                
                val fillW = w * (safeLevel / 100f)
                drawRoundRect(
                    color = fillColor, 
                    size = Size(fillW, h),
                    cornerRadius = CornerRadius(cornerRadius)
                )
                
                drawRoundRect(
                    color = color.copy(alpha = 0.2f),
                    size = Size(w, h),
                    style = Stroke(strokeWidth),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            }
            2 -> { 
                val strokeWidth = 2.5.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth
                
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    style = Stroke(strokeWidth),
                    radius = radius
                )
                
                drawArc(
                    color = fillColor,
                    startAngle = -90f,
                    sweepAngle = 360f * (safeLevel / 100f),
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(strokeWidth, strokeWidth)
                )
            }
            else -> { 
                val strokeWidth = 2.dp.toPx()
                val batteryWidth = size.width - 4.dp.toPx()
                val batteryHeight = size.height
                
                drawRoundRect(
                    color = color,
                    size = Size(batteryWidth, batteryHeight),
                    style = Stroke(width = strokeWidth),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                
                drawRect(
                    color = color,
                    topLeft = Offset(batteryWidth, batteryHeight * 0.25f),
                    size = Size(4.dp.toPx(), batteryHeight * 0.5f)
                )
                
                val fillWidth = (batteryWidth - strokeWidth * 2) * (safeLevel / 100f)
                if (fillWidth > 0) {
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(strokeWidth, strokeWidth),
                        size = Size(fillWidth, batteryHeight - strokeWidth * 2),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier, oledMode: Boolean) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val date = Date(currentTime)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeFormat.format(date),
            color = if (oledMode) Color.White else MaterialTheme.colorScheme.onBackground,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 64.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = dateFormat.format(date),
            color = if (oledMode) Color.LightGray else MaterialTheme.colorScheme.secondary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConnectivityStatus(oledMode: Boolean) {
    val context = LocalContext.current
    val connectivityManager = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    var isWifi by remember { mutableStateOf(false) }
    var isCellular by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            val network = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(network)
            isConnected = caps != null
            if (caps != null) {
                isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
            delay(5000)
        }
    }

    val icon = when {
        isWifi -> Icons.Default.Wifi
        isCellular -> Icons.Default.SignalCellularAlt
        else -> Icons.Default.SignalWifiOff
    }
    
    val tint = if (oledMode) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant

    Icon(
        imageVector = icon,
        contentDescription = "Connectivity",
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
fun CommitHistoryBoard(
    data: List<Int>,
    username: String,
    avatarUrl: String?,
    oledMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = username,
                color = if (oledMode) Color.White else MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val totalWeeks = 52
            val gap = 3.dp
            val density = LocalDensity.current
            val availableWidth = maxWidth - 16.dp 
            val gapPx = with(density) { gap.toPx() }
            val widthPx = with(density) { availableWidth.toPx() }
            
            val totalGapPx = gapPx * (totalWeeks - 1)
            val maxTileSizePx = (widthPx - totalGapPx) / totalWeeks
            val tileSizePx = maxTileSizePx.coerceIn(with(density){8.dp.toPx()}, with(density){24.dp.toPx()})
            val tileSize = with(density) { tileSizePx.toDp() }

            Row(
                modifier = Modifier.wrapContentSize(),
                horizontalArrangement = Arrangement.Center
            ) {
                val safeData = if (data.isEmpty()) List(52*7) { 0 } else data
                val maxDays = totalWeeks * 7
                val displayData = safeData.takeLast(maxDays)
                
                for (week in 0 until totalWeeks) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        for (day in 0 until 7) {
                            val index = (week * 7) + day
                            val intensity = displayData.getOrElse(index) { 0 }
                            CommitTile(intensity, oledMode, tileSize)
                        }
                    }
                    if (week < totalWeeks - 1) {
                         Spacer(modifier = Modifier.width(gap))
                    }
                }
            }
        }
    }
}

@Composable
fun CommitTile(intensity: Int, oledMode: Boolean, size: androidx.compose.ui.unit.Dp) {
    val color = when (intensity) {
        0 -> if (oledMode) Color(0xFF161B22) else MaterialTheme.colorScheme.surfaceVariant
        1 -> Color(0xFF0E4429) 
        2 -> Color(0xFF006D32)
        3 -> Color(0xFF26A641)
        4 -> Color(0xFF39D353) 
        else -> if (oledMode) Color(0xFF161B22) else MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(color, shape = RoundedCornerShape(1.dp))
    )
}

fun getBatteryLevel(context: Context): Int {
    return try {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (e: Exception) {
        100 
    }
}
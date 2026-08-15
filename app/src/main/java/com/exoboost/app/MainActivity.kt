package com.exoboost.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exoboost.app.core.capability.CapabilityDetector
import com.exoboost.app.core.common.Constants
import com.exoboost.app.core.datastore.ExoSettings
import com.exoboost.app.core.datastore.PreferencesManager
import com.exoboost.app.core.designsystem.ElectricBlue
import com.exoboost.app.core.designsystem.EmeraldGreen
import com.exoboost.app.core.designsystem.ExoBoostTheme
import com.exoboost.app.core.designsystem.GlassBorder
import com.exoboost.app.core.designsystem.Slate100
import com.exoboost.app.core.designsystem.Slate200
import com.exoboost.app.core.designsystem.Slate400
import com.exoboost.app.core.designsystem.Slate800
import com.exoboost.app.core.designsystem.Slate900
import com.exoboost.app.core.designsystem.Slate950
import com.exoboost.app.core.permissions.PermissionManager
import com.exoboost.app.feature.overlay.OverlayService
import com.exoboost.app.feature.profiles.data.AppProfileManager
import com.exoboost.app.feature.profiles.detector.ForegroundAppDetector
import com.exoboost.app.feature.profiles.model.AppInfo
import com.exoboost.app.feature.profiles.ui.AppsManagementScreen
import com.exoboost.app.feature.settings.ui.SettingsScreen
import com.exoboost.app.feature.toolbox.ui.ToolboxEditorScreen
import com.exoboost.app.feature.tools.audio.AudioEffectCapabilityDetector
import com.exoboost.app.feature.tools.audio.model.AudioCapabilityReport
import com.exoboost.app.feature.tools.style.StyleEngine
import com.exoboost.app.feature.tools.style.engine.ShaderProcessor
import com.exoboost.app.feature.tools.style.live.LiveStyleCaptureActivity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var appProfileManager: AppProfileManager
    private var showPermissionRationale by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission keeps the edge handle alive in background", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(this)
        appProfileManager = AppProfileManager(this)

        setContent {
            ExoBoostTheme {
                val settings by preferencesManager.settingsFlow.collectAsState(initial = ExoSettings())
                val appProfiles by appProfileManager.profilesFlow.collectAsState(initial = emptyMap())
                var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

                var hasOverlayPermission by remember { mutableStateOf(CapabilityDetector.isOverlayPermissionGranted(this)) }
                var hasUsageStatsPermission by remember { mutableStateOf(ForegroundAppDetector.hasUsageStatsPermission(this)) }
                var isServiceRunning by remember { mutableStateOf(OverlayService.isRunning) }
                val audioReport = remember { AudioEffectCapabilityDetector.detectCapabilities() }
                var selectedNavIndex by remember { mutableIntStateOf(0) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    hasOverlayPermission = CapabilityDetector.isOverlayPermissionGranted(this@MainActivity)
                    hasUsageStatsPermission = ForegroundAppDetector.hasUsageStatsPermission(this@MainActivity)
                    isServiceRunning = OverlayService.isRunning
                    installedApps = appProfileManager.getInstalledLaunchableApps()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CapabilityDetector.isNotificationPermissionGranted(this@MainActivity)) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Slate900,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedNavIndex == 0,
                                onClick = { selectedNavIndex = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                                label = { Text("Overview") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricBlue,
                                    selectedTextColor = ElectricBlue,
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate400,
                                    indicatorColor = Slate800
                                )
                            )
                            NavigationBarItem(
                                selected = selectedNavIndex == 1,
                                onClick = { selectedNavIndex = 1 },
                                icon = { Icon(Icons.Default.GridView, contentDescription = "Toolbox") },
                                label = { Text("Toolbox") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricBlue,
                                    selectedTextColor = ElectricBlue,
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate400,
                                    indicatorColor = Slate800
                                )
                            )
                            NavigationBarItem(
                                selected = selectedNavIndex == 2,
                                onClick = { selectedNavIndex = 2 },
                                icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                                label = { Text("Apps") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricBlue,
                                    selectedTextColor = ElectricBlue,
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate400,
                                    indicatorColor = Slate800
                                )
                            )
                            NavigationBarItem(
                                selected = selectedNavIndex == 3,
                                onClick = { selectedNavIndex = 3 },
                                icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricBlue,
                                    selectedTextColor = ElectricBlue,
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate400,
                                    indicatorColor = Slate800
                                )
                            )
                        }
                    },
                    containerColor = Slate950
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedNavIndex) {
                            0 -> MainSetupScreen(
                                settings = settings,
                                hasOverlayPermission = hasOverlayPermission,
                                isServiceRunning = isServiceRunning,
                                audioReport = audioReport,
                                onRequestPermission = {
                                    showPermissionRationale = true
                                },
                                onEnableExoBoost = {
                                    if (!CapabilityDetector.isOverlayPermissionGranted(this@MainActivity)) {
                                        showPermissionRationale = true
                                    } else {
                                        scope.launch {
                                            preferencesManager.setServiceEnabled(true)
                                            startOverlayService()
                                            isServiceRunning = true
                                        }
                                    }
                                },
                                onDisableExoBoost = {
                                    scope.launch {
                                        preferencesManager.setServiceEnabled(false)
                                        stopOverlayService()
                                        isServiceRunning = false
                                    }
                                },
                                onLaunchLiveFilterExperiment = {
                                    if (!CapabilityDetector.isOverlayPermissionGranted(this@MainActivity)) {
                                        showPermissionRationale = true
                                    } else {
                                        if (!OverlayService.isRunning) {
                                            startOverlayService()
                                        }
                                        val intent = Intent(this@MainActivity, LiveStyleCaptureActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                        }
                                        startActivity(intent)
                                    }
                                }
                            )
                            1 -> ToolboxEditorScreen(
                                activeToolIds = settings.activeToolIds,
                                layoutType = settings.toolboxLayoutType,
                                onMoveToolUp = { index ->
                                    if (index > 0) {
                                        val list = settings.activeToolIds.toMutableList()
                                        val item = list.removeAt(index)
                                        list.add(index - 1, item)
                                        scope.launch { preferencesManager.setActiveToolOrder(list) }
                                    }
                                },
                                onMoveToolDown = { index ->
                                    if (index < settings.activeToolIds.size - 1) {
                                        val list = settings.activeToolIds.toMutableList()
                                        val item = list.removeAt(index)
                                        list.add(index + 1, item)
                                        scope.launch { preferencesManager.setActiveToolOrder(list) }
                                    }
                                },
                                onAddTool = { toolId ->
                                    scope.launch { preferencesManager.addActiveTool(toolId) }
                                },
                                onRemoveTool = { toolId ->
                                    scope.launch { preferencesManager.removeActiveTool(toolId) }
                                },
                                onSelectLayout = { layout ->
                                    scope.launch { preferencesManager.setToolboxLayoutType(layout) }
                                },
                                onResetDefaults = {
                                    scope.launch { preferencesManager.resetToolboxToDefaults() }
                                }
                            )
                            2 -> AppsManagementScreen(
                                installedApps = installedApps,
                                profiles = appProfiles,
                                hasUsageStatsPermission = hasUsageStatsPermission,
                                onRequestUsagePermission = {
                                    startActivity(ForegroundAppDetector.createUsageAccessSettingsIntent())
                                },
                                onSaveProfile = { profile ->
                                    scope.launch { appProfileManager.saveProfile(profile) }
                                },
                                onToggleAppEnabled = { pkg, name, en ->
                                    scope.launch { appProfileManager.toggleAppEnabled(pkg, name, en) }
                                }
                            )
                            3 -> SettingsScreen(
                                settings = settings,
                                onUpdateServiceEnabled = { en ->
                                    scope.launch {
                                        preferencesManager.setServiceEnabled(en)
                                        if (en) {
                                            if (CapabilityDetector.isOverlayPermissionGranted(this@MainActivity)) {
                                                startOverlayService()
                                                isServiceRunning = true
                                            } else {
                                                showPermissionRationale = true
                                            }
                                        } else {
                                            stopOverlayService()
                                            isServiceRunning = false
                                        }
                                    }
                                },
                                onUpdateAutoStart = { auto -> scope.launch { preferencesManager.setAutoStartOnBoot(auto) } },
                                onUpdateSide = { side -> scope.launch { preferencesManager.setHandleSide(side) } },
                                onUpdateHeight = { h -> scope.launch { preferencesManager.setHandleHeightDp(h) } },
                                onUpdateWidth = { w -> scope.launch { preferencesManager.setHandleWidthDp(w) } },
                                onUpdateAlpha = { a -> scope.launch { preferencesManager.setHandleAlpha(a) } },
                                onUpdateAnimation = { anim -> scope.launch { preferencesManager.setAnimationEnabled(anim) } },
                                onUpdateHaptic = { h -> scope.launch { preferencesManager.setHapticEnabled(h) } },
                                onUpdateTransparency = { t -> scope.launch { preferencesManager.setToolboxTransparency(t) } },
                                onUpdateCornerRadius = { r -> scope.launch { preferencesManager.setToolboxCornerRadiusDp(r) } },
                                onUpdateScreenshotConfirmation = { show -> scope.launch { preferencesManager.setShowScreenshotConfirmation(show) } },
                                onUpdateVolumeBoostPercent = { p -> scope.launch { preferencesManager.setVolumeBoostPercent(p) } },
                                onUpdateMaxBoostLimit = { max -> scope.launch { preferencesManager.setMaxBoostLimit(max) } },
                                onUpdateLimiterThreshold = { th -> scope.launch { preferencesManager.setLimiterThresholdDb(th) } },
                                onResetVolumeDefaults = { scope.launch { preferencesManager.resetVolumeSettings() } },
                                onUpdateBlackScreenGesture = { g -> scope.launch { preferencesManager.setBlackScreenExitGesture(g) } },
                                onUpdateBlackScreenShowHint = { hint -> scope.launch { preferencesManager.setBlackScreenShowHint(hint) } },
                                onUpdateTriggerSensitivity = { s -> scope.launch { preferencesManager.setTriggerSensitivityPx(s) } },
                                onUpdateHideInFullscreen = { hide -> scope.launch { preferencesManager.setHideHandleInFullscreen(hide) } },
                                onNavigateToToolbox = { selectedNavIndex = 1 },
                                onNavigateToApps = { selectedNavIndex = 2 }
                            )
                        }

                        if (showPermissionRationale) {
                            OverlayPermissionRationaleDialog(
                                onConfirm = {
                                    showPermissionRationale = false
                                    startActivity(PermissionManager.createOverlaySettingsIntent(this@MainActivity))
                                },
                                onDismiss = {
                                    showPermissionRationale = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = CapabilityDetector.isOverlayPermissionGranted(this)
        if (granted && OverlayService.isRunning) {
            // Service is active
        }
    }

    private fun startOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = Constants.ACTION_START_OVERLAY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = Constants.ACTION_STOP_OVERLAY
        }
        startService(serviceIntent)
    }
}

@Composable
fun MainSetupScreen(
    settings: ExoSettings,
    hasOverlayPermission: Boolean,
    isServiceRunning: Boolean,
    audioReport: AudioCapabilityReport,
    onRequestPermission: () -> Unit,
    onEnableExoBoost: () -> Unit,
    onDisableExoBoost: () -> Unit,
    onLaunchLiveFilterExperiment: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Banner
        Column {
            Text(
                text = "ExoBoost",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Universal Edge Video Toolbox • Phase 10 Production",
                fontSize = 14.sp,
                color = Slate400
            )
        }

        // Live Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Status Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate100
                )

                // Overlay Permission Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overlay permission",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate200
                        )
                        Text(
                            text = if (hasOverlayPermission) "Permission is active" else "Required to display floating handle",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                    StatusBadge(
                        label = if (hasOverlayPermission) "Granted" else "Not granted",
                        isPositive = hasOverlayPermission,
                        onClick = if (!hasOverlayPermission) onRequestPermission else null
                    )
                }

                // Overlay Service Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overlay service",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate200
                        )
                        Text(
                            text = if (isServiceRunning) "Running in foreground" else "Stopped",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                    StatusBadge(
                        label = if (isServiceRunning) "Running" else "Stopped",
                        isPositive = isServiceRunning,
                        onClick = null
                    )
                }

                // Audio Effect Backend Probe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Booster Backend",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate200
                        )
                        Text(
                            text = audioReport.recommendedBackend.displayName,
                            fontSize = 12.sp,
                            color = if (audioReport.isSession0Supported) EmeraldGreen else Slate400
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (audioReport.isSession0Supported) EmeraldGreen.copy(alpha = 0.15f) else Slate800)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (audioReport.isSession0Supported) "Supported" else "HAL Restricted",
                            color = if (audioReport.isSession0Supported) EmeraldGreen else Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Primary Enable/Disable Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onEnableExoBoost,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    disabledContainerColor = Slate800
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = !isServiceRunning
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enable ExoBoost", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onDisableExoBoost,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate800,
                    disabledContainerColor = Slate900
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = isServiceRunning
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = if (isServiceRunning) Color(0xFFEF4444) else Slate400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Disable ExoBoost",
                    fontSize = 14.sp,
                    color = if (isServiceRunning) Slate100 else Slate400,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Phase 7 Experimental Lab Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Video Filter Lab",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EXPERIMENTAL",
                            color = Color(0xFFC084FC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Real-time GPU video style grading for external apps via MediaProjection into a floating PIP viewport.\n\n• Performance: ~50-60 FPS, ~16ms latency.\n• DRM limitation: DRM/FLAG_SECURE content renders black.",
                    fontSize = 12.sp,
                    color = Slate400,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = onLaunchLiveFilterExperiment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Live Stream PIP Filter", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Style Engine Card Preview
        StyleEngineShowcaseCard()

        // Permission Rationale Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Why is Overlay Permission Needed?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate100
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Android requires the 'Display over other apps' (SYSTEM_ALERT_WINDOW) permission to show the floating edge handle, toolbox panel, volume booster, style filters, and black screen listening overlay above other applications.\n\nExoBoost strictly respects user privacy with zero idle CPU overhead.",
                    fontSize = 13.sp,
                    color = Slate400,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
fun StyleEngineShowcaseCard() {
    val activePreset by StyleEngine.activePreset.collectAsState()
    val activeParams by StyleEngine.activeParameters.collectAsState()
    val baseSample = remember { StyleEngine.getSampleBitmap() }
    val previewBitmap = remember(activeParams) {
        ShaderProcessor.processBitmap(baseSample, activeParams)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ExoBoost Style Engine",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate100
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activePreset.displayName,
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sample Live Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            ) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "GPU-accelerated color grading with 11 presets (Cinema, Retro, Vivid, B&W, Night, Custom). Tap Style in the floating toolbox to customize parameters.",
                fontSize = 12.sp,
                color = Slate400,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    isPositive: Boolean,
    onClick: (() -> Unit)?
) {
    if (onClick != null) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
            modifier = Modifier.height(34.dp)
        ) {
            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isPositive) EmeraldGreen.copy(alpha = 0.15f) else Slate800)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isPositive) EmeraldGreen else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = if (isPositive) EmeraldGreen else Slate400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun OverlayPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Display Over Other Apps",
                color = Slate100,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "ExoBoost needs this permission to draw the edge handle, floating toolbox, volume booster, style filters, and black screen listening overlay above other applications.\n\nYour privacy is protected: no screen content is monitored or recorded.",
                color = Color(0xFFCBD5E1),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(20.dp)
    )
}

package com.exoboost.app.feature.settings.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exoboost.app.core.datastore.ExoSettings
import com.exoboost.app.core.designsystem.ElectricBlue
import com.exoboost.app.core.designsystem.EmeraldGreen
import com.exoboost.app.core.designsystem.GlassBorder
import com.exoboost.app.core.designsystem.LightBlue
import com.exoboost.app.core.designsystem.Slate100
import com.exoboost.app.core.designsystem.Slate200
import com.exoboost.app.core.designsystem.Slate400
import com.exoboost.app.core.designsystem.Slate800
import com.exoboost.app.core.designsystem.Slate900
import com.exoboost.app.feature.diagnostics.model.DeviceDiagnosticReport
import com.exoboost.app.feature.diagnostics.model.DiagnosticsProbe
import com.exoboost.app.feature.diagnostics.ui.DiagnosticsDialog
import com.exoboost.app.feature.tools.style.model.StylePresetType

@Composable
fun SettingsScreen(
    settings: ExoSettings,
    onUpdateServiceEnabled: (Boolean) -> Unit,
    onUpdateAutoStart: (Boolean) -> Unit,
    onUpdateSide: (String) -> Unit,
    onUpdateHeight: (Int) -> Unit,
    onUpdateWidth: (Int) -> Unit,
    onUpdateAlpha: (Float) -> Unit,
    onUpdateAnimation: (Boolean) -> Unit,
    onUpdateHaptic: (Boolean) -> Unit,
    onUpdateTransparency: (Float) -> Unit,
    onUpdateCornerRadius: (Int) -> Unit,
    onUpdateScreenshotConfirmation: (Boolean) -> Unit,
    onUpdateVolumeBoostPercent: (Int) -> Unit,
    onUpdateMaxBoostLimit: (Int) -> Unit,
    onUpdateLimiterThreshold: (Float) -> Unit,
    onResetVolumeDefaults: () -> Unit,
    onUpdateBlackScreenGesture: (String) -> Unit,
    onUpdateBlackScreenShowHint: (Boolean) -> Unit,
    onUpdateTriggerSensitivity: (Int) -> Unit,
    onUpdateHideInFullscreen: (Boolean) -> Unit,
    onNavigateToToolbox: () -> Unit,
    onNavigateToApps: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLimitationsDialog by remember { mutableStateOf(false) }

    val diagnosticReport = remember { DiagnosticsProbe.generateReport(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Banner
        Column {
            Text(
                text = "ExoBoost Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100
            )
            Text(
                text = "Production Configuration, Edge Handle, Audio & Diagnostics",
                fontSize = 13.sp,
                color = Slate400
            )
        }

        // Live Diagnostics Quick Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "System Diagnostics",
                            color = Slate100,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hardware & Capability Probe",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { showDiagnosticsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Inspect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // SECTION: GENERAL
        SettingsSectionHeader(title = "GENERAL", icon = Icons.Default.Tune)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Enable ExoBoost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable ExoBoost Service", color = Slate200, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Start overlay handle across apps", color = Slate400, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.isServiceEnabled,
                        onCheckedChange = onUpdateServiceEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
                    )
                }

                // Start on Boot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Automatically on Boot", color = Slate200, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Restore handle on device startup", color = Slate400, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.autoStartOnBoot,
                        onCheckedChange = onUpdateAutoStart,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
                    )
                }

                // Haptic Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haptic Feedback", color = Slate200, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Vibrate on handle drag & button click", color = Slate400, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.isHapticEnabled,
                        onCheckedChange = onUpdateHaptic,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
                    )
                }

                // Panel Animations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Panel Animations", color = Slate200, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Smooth slide & scale when unfolding", color = Slate400, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.isAnimationEnabled,
                        onCheckedChange = onUpdateAnimation,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
                    )
                }
            }
        }

        // SECTION: EDGE HANDLE
        SettingsSectionHeader(title = "EDGE HANDLE", icon = Icons.Default.TouchApp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Side
                Text("Placement Edge", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onUpdateSide("LEFT") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.handleSide == "LEFT") ElectricBlue else Slate800
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Left Edge")
                    }
                    Button(
                        onClick = { onUpdateSide("RIGHT") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.handleSide == "RIGHT") ElectricBlue else Slate800
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Right Edge")
                    }
                }

                // Handle Height
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Handle Height", color = Slate200, fontSize = 13.sp)
                        Text("${settings.handleHeightDp} dp", color = Slate400, fontSize = 13.sp)
                    }
                    Slider(
                        value = settings.handleHeightDp.toFloat(),
                        onValueChange = { onUpdateHeight(it.toInt()) },
                        valueRange = 40f..140f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }

                // Handle Width
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Handle Width", color = Slate200, fontSize = 13.sp)
                        Text("${settings.handleWidthDp} dp", color = Slate400, fontSize = 13.sp)
                    }
                    Slider(
                        value = settings.handleWidthDp.toFloat(),
                        onValueChange = { onUpdateWidth(it.toInt()) },
                        valueRange = 4f..16f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }

                // Handle Opacity
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Handle Opacity", color = Slate200, fontSize = 13.sp)
                        Text("${(settings.handleAlpha * 100).toInt()}%", color = Slate400, fontSize = 13.sp)
                    }
                    Slider(
                        value = settings.handleAlpha,
                        onValueChange = { onUpdateAlpha(it) },
                        valueRange = 0.15f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }

                // Trigger Sensitivity
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Trigger Swipe Sensitivity", color = Slate200, fontSize = 13.sp)
                        Text("${settings.triggerSensitivityPx} px", color = Slate400, fontSize = 13.sp)
                    }
                    Slider(
                        value = settings.triggerSensitivityPx.toFloat(),
                        onValueChange = { onUpdateTriggerSensitivity(it.toInt()) },
                        valueRange = 25f..80f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }

                // Hide in Fullscreen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Handle in Fullscreen", color = Slate200, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Auto-dismiss during immersive video", color = Slate400, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.hideHandleInFullscreen,
                        onCheckedChange = onUpdateHideInFullscreen,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
                    )
                }
            }
        }

        // SECTION: TOOLBOX APPEARANCE & CUSTOMIZATION
        SettingsSectionHeader(title = "TOOLBOX & LAYOUT", icon = Icons.Default.GridView)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Transparency
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Panel Opacity", color = Slate200, fontSize = 13.sp)
                        Text("${(settings.toolboxTransparency * 100).toInt()}%", color = Slate400, fontSize = 13.sp)
                    }
                    Slider(
                        value = settings.toolboxTransparency,
                        onValueChange = { onUpdateTransparency(it) },
                        valueRange = 0.50f..0.98f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }

                // Corner Radius
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Corner Radius", color = Slate200, fontSize = 13.sp)
                        Text("${settings.toolboxCornerRadiusDp} dp", color = Slate400, fontSize = 13.sp)
                    }
                    Slider(
                        value = settings.toolboxCornerRadiusDp.toFloat(),
                        onValueChange = { onUpdateCornerRadius(it.toInt()) },
                        valueRange = 16f..36f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }

                // Action Link to Toolbox Tab
                Button(
                    onClick = onNavigateToToolbox,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reorder Tools & Choose Grid Layout (${settings.toolboxLayoutType})", fontSize = 12.sp)
                }
            }
        }

        // SECTION: BLACK SCREEN AUDIO MODE
        SettingsSectionHeader(title = "BLACK SCREEN AUDIO MODE", icon = Icons.Default.Bedtime)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Exit Gesture", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val gestures = listOf(
                        "DOUBLE_TAP" to "Double Tap",
                        "SWIPE" to "Swipe",
                        "BOTH" to "Both"
                    )
                    gestures.forEach { (key, label) ->
                        val isSelected = settings.blackScreenExitGesture == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF8B5CF6) else Slate800)
                                .clickable { onUpdateBlackScreenGesture(key) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Slate200,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Show Hint Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Exit Hint", color = Slate200, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Displays subtle unlock guide on touch", color = Slate400, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.blackScreenShowHint,
                        onCheckedChange = onUpdateBlackScreenShowHint,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8B5CF6))
                    )
                }

                // Disclosure Note
                Text(
                    text = "Note: Black Screen Mode applies an opaque #000000 overlay with minimum display brightness (0.01f) to keep audio streaming without pocket clicks. It does not power off the physical display controller.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // SECTION: VOLUME BOOST & LIMITER
        SettingsSectionHeader(title = "VOLUME BOOST & LIMITER", icon = Icons.AutoMirrored.Filled.VolumeUp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Default Boost
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Default Boost Level", color = Slate200, fontSize = 13.sp)
                        Text("${settings.volumeBoostPercent}%", color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.volumeBoostPercent.toFloat(),
                        onValueChange = { onUpdateVolumeBoostPercent(it.toInt()) },
                        valueRange = 100f..settings.maxBoostLimit.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = EmeraldGreen, activeTrackColor = EmeraldGreen)
                    )
                }

                // Max Limit
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Maximum Boost Cap", color = Slate200, fontSize = 13.sp)
                        Text("${settings.maxBoostLimit}%", color = Color(0xFFF59E0B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.maxBoostLimit.toFloat(),
                        onValueChange = { onUpdateMaxBoostLimit(it.toInt()) },
                        valueRange = 150f..300f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                    )
                }

                // Limiter Threshold
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Limiter Threshold", color = Slate200, fontSize = 13.sp)
                        Text("${String.format("%.1f", settings.limiterThresholdDb)} dB", color = Slate400, fontSize = 13.sp)
                    }
                    Slider(
                        value = settings.limiterThresholdDb,
                        onValueChange = { onUpdateLimiterThreshold(it) },
                        valueRange = -6.0f..0.0f,
                        colors = SliderDefaults.colors(thumbColor = Slate200, activeTrackColor = Slate200)
                    )
                }

                // Reset Button
                OutlinedButton(
                    onClick = onResetVolumeDefaults,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Audio Boost Settings", fontSize = 11.sp)
                }
            }
        }

        // SECTION: ABOUT, PRIVACY & LICENSES
        SettingsSectionHeader(title = "ABOUT & DISCLOSURES", icon = Icons.Default.Info)

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
                    Text("ExoBoost Core Version", color = Slate200, fontSize = 14.sp)
                    Text("v1.0.0 (Release API 33)", color = ElectricBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Privacy Statement", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { showLimitationsDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Android Disclosures", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = { showLicensesDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Source Licenses & Architecture", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Modal Dialogs
    if (showDiagnosticsDialog) {
        DiagnosticsDialog(report = diagnosticReport, onDismiss = { showDiagnosticsDialog = false })
    }

    if (showPrivacyDialog) {
        PrivacyStatementDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showLimitationsDialog) {
        TechnicalLimitationsDialog(onDismiss = { showLimitationsDialog = false })
    }

    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Slate100,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun PrivacyStatementDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Guarantee", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "ExoBoost is designed with a strict privacy-first architecture:\n\n" +
                        "• 100% On-Device: All processing, shaders, and configurations execute entirely on your phone.\n" +
                        "• Zero Telemetry: No analytics, tracking, or user data collection.\n" +
                        "• No Hidden Screen Access: Screen capture occurs only upon explicit user tap.\n" +
                        "• No Accessibility Hijacking: Standard Android APIs are used exclusively.",
                color = Slate200,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                Text("Close")
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun TechnicalLimitationsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Known Android Limitations", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "ExoBoost strictly respects Android security & HAL architecture:\n\n" +
                        "1. DRM / FLAG_SECURE: Protected video streams (e.g. Netflix, banking apps) render black when captured via MediaProjection.\n\n" +
                        "2. Volume Session 0: Audio gain amplification is subject to OEM hardware abstraction layers (HAL). Where session 0 effects are restricted by vendor drivers, ExoBoost reports this honestly.\n\n" +
                        "3. Cross-App View Rewriting: Android security forbids modifying another app's native View hierarchy without root.",
                color = Slate200,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                Text("Understood")
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Source & Architecture", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "ExoBoost is built using modern open-source technologies:\n\n" +
                        "• Jetpack Compose & Material 3 (Apache 2.0)\n" +
                        "• AndroidX Core & DataStore Preferences (Apache 2.0)\n" +
                        "• Kotlin Coroutines & Serialization (Apache 2.0)\n" +
                        "• Original Glassmorphism UI inspired by Xiaomi MIUI/HyperOS interaction models without proprietary assets.",
                color = Slate200,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                Text("Close")
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(20.dp)
    )
}

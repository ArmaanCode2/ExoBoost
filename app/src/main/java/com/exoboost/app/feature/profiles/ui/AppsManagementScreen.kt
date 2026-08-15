package com.exoboost.app.feature.profiles.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exoboost.app.core.designsystem.ElectricBlue
import com.exoboost.app.core.designsystem.EmeraldGreen
import com.exoboost.app.core.designsystem.GlassBorder
import com.exoboost.app.core.designsystem.Slate100
import com.exoboost.app.core.designsystem.Slate200
import com.exoboost.app.core.designsystem.Slate400
import com.exoboost.app.core.designsystem.Slate800
import com.exoboost.app.core.designsystem.Slate900
import com.exoboost.app.feature.profiles.model.AppInfo
import com.exoboost.app.feature.profiles.model.AppProfile
import com.exoboost.app.feature.tools.style.model.StylePresetType
import com.exoboost.app.feature.toolbox.model.ToolRegistry

@Composable
fun AppsManagementScreen(
    installedApps: List<AppInfo>,
    profiles: Map<String, AppProfile>,
    hasUsageStatsPermission: Boolean,
    onRequestUsagePermission: () -> Unit,
    onSaveProfile: (AppProfile) -> Unit,
    onToggleAppEnabled: (String, String, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedAppForEdit by remember { mutableStateOf<AppInfo?>(null) }

    val filteredApps = remember(installedApps, profiles, searchQuery, selectedFilter) {
        installedApps.filter { app ->
            val matchesSearch = app.displayName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "CONFIGURED" -> profiles.containsKey(app.packageName)
                "ENABLED" -> profiles[app.packageName]?.isEnabled ?: true
                "DISABLED" -> profiles[app.packageName]?.isEnabled == false
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title Banner
        Column {
            Text(
                text = "Per-App Profiles",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100
            )
            Text(
                text = "Configure custom toolbox actions, style, and volume per application",
                fontSize = 13.sp,
                color = Slate400
            )
        }

        // Usage Stats Permission Banner if not granted
        if (!hasUsageStatsPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Usage Access Required",
                            color = Slate100,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Enables automatic detection when you switch apps",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onRequestUsagePermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Grant", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed applications...", color = Slate400, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf(
                "ALL" to "All Apps (${installedApps.size})",
                "ENABLED" to "Enabled",
                "DISABLED" to "Disabled",
                "CONFIGURED" to "Custom Profiles (${profiles.size})"
            )
            items(filterOptions) { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ElectricBlue else Slate900)
                        .border(0.5.dp, if (isSelected) ElectricBlue else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Slate400,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // App List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val profile = profiles[app.packageName] ?: AppProfile.createDefault(app.packageName, app.displayName)

                AppProfileRow(
                    app = app,
                    profile = profile,
                    onClick = { selectedAppForEdit = app },
                    onToggleEnabled = { enabled ->
                        onToggleAppEnabled(app.packageName, app.displayName, enabled)
                    }
                )
            }
        }
    }

    // Modal Profile Editor Dialog
    selectedAppForEdit?.let { app ->
        val currentProfile = profiles[app.packageName] ?: AppProfile.createDefault(app.packageName, app.displayName)

        AppProfileEditorDialog(
            app = app,
            initialProfile = currentProfile,
            onSave = { updated ->
                onSaveProfile(updated)
                selectedAppForEdit = null
            },
            onDismiss = { selectedAppForEdit = null }
        )
    }
}

@Composable
fun AppProfileRow(
    app: AppInfo,
    profile: AppProfile,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Letter Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (profile.isEnabled) ElectricBlue.copy(alpha = 0.2f) else Slate800),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.displayName.firstOrNull()?.uppercase() ?: "A",
                        color = if (profile.isEnabled) ElectricBlue else Slate400,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.displayName,
                        color = if (profile.isEnabled) Slate100 else Slate400,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (profile.isEnabled) {
                            "${profile.stylePresetId.replaceFirstChar { it.uppercase() }} • ${profile.volumeBoostPercent}% Boost • ${profile.enabledToolIds.size} Tools"
                        } else {
                            "Toolbox Disabled"
                        },
                        color = if (profile.isEnabled) EmeraldGreen else Slate400,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = profile.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
            )
        }
    }
}

@Composable
fun AppProfileEditorDialog(
    app: AppInfo,
    initialProfile: AppProfile,
    onSave: (AppProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(initialProfile.isEnabled) }
    var enabledToolIds by remember { mutableStateOf(initialProfile.enabledToolIds) }
    var volumeBoostPercent by remember { mutableStateOf(initialProfile.volumeBoostPercent) }
    var stylePresetId by remember { mutableStateOf(initialProfile.stylePresetId) }

    val allTools = remember { ToolRegistry.getDefaultActions() }
    val stylePresets = remember { StylePresetType.values() }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = app.displayName, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = app.packageName, color = Slate400, fontSize = 11.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Enable ExoBoost switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Toolbox in App", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
                    )
                }

                if (isEnabled) {
                    // Default Style Preset
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Default Style Preset", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(stylePresets) { preset ->
                                val isSelected = stylePresetId.equals(preset.id, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFF59E0B) else Slate800)
                                        .clickable { stylePresetId = preset.id }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        color = if (isSelected) Color.Black else Slate200,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Default Volume Boost Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Default Volume Boost", color = Slate200, fontSize = 13.sp)
                            Text("$volumeBoostPercent%", color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = volumeBoostPercent.toFloat(),
                            onValueChange = { volumeBoostPercent = it.toInt() },
                            valueRange = 100f..300f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = EmeraldGreen, activeTrackColor = EmeraldGreen)
                        )
                    }

                    // Active Tools in this App
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Active Toolbox Tools", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        allTools.forEach { tool ->
                            val isToolActive = enabledToolIds.contains(tool.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val updated = enabledToolIds.toMutableSet()
                                        if (isToolActive) updated.remove(tool.id) else updated.add(tool.id)
                                        enabledToolIds = updated
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = tool.title, color = Slate200, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = isToolActive,
                                    onCheckedChange = { checked ->
                                        val updated = enabledToolIds.toMutableSet()
                                        if (checked) updated.add(tool.id) else updated.remove(tool.id)
                                        enabledToolIds = updated
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        AppProfile(
                            packageName = app.packageName,
                            displayName = app.displayName,
                            isEnabled = isEnabled,
                            enabledToolIds = enabledToolIds,
                            volumeBoostPercent = volumeBoostPercent,
                            stylePresetId = stylePresetId
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Profile")
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
        shape = RoundedCornerShape(22.dp)
    )
}

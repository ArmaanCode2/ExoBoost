package com.exoboost.app.feature.profiles.model

import com.exoboost.app.feature.toolbox.model.ToolRegistry

data class AppInfo(
    val packageName: String,
    val displayName: String,
    val isSystemApp: Boolean = false
)

data class AppProfile(
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val enabledToolIds: Set<String> = setOf(
        ToolRegistry.ID_RECORD,
        ToolRegistry.ID_SCREENSHOT,
        ToolRegistry.ID_SCREEN_OFF,
        ToolRegistry.ID_VOLUME_BOOST,
        ToolRegistry.ID_STYLE,
        ToolRegistry.ID_AUDIO,
        ToolRegistry.ID_BRIGHTNESS,
        ToolRegistry.ID_SETTINGS
    ),
    val volumeBoostPercent: Int = 100,
    val stylePresetId: String = "original",
    val handleSide: String? = null // null means inherit global preference
) {
    companion object {
        fun createDefault(packageName: String, displayName: String): AppProfile {
            return when {
                packageName.contains("youtube", ignoreCase = true) -> AppProfile(
                    packageName = packageName,
                    displayName = displayName,
                    isEnabled = true,
                    enabledToolIds = setOf(
                        ToolRegistry.ID_RECORD,
                        ToolRegistry.ID_SCREENSHOT,
                        ToolRegistry.ID_SCREEN_OFF,
                        ToolRegistry.ID_VOLUME_BOOST,
                        ToolRegistry.ID_STYLE,
                        ToolRegistry.ID_SETTINGS
                    ),
                    volumeBoostPercent = 125,
                    stylePresetId = "cinema"
                )
                packageName.contains("spotify", ignoreCase = true) || packageName.contains("music", ignoreCase = true) -> AppProfile(
                    packageName = packageName,
                    displayName = displayName,
                    isEnabled = true,
                    enabledToolIds = setOf(
                        ToolRegistry.ID_SCREEN_OFF,
                        ToolRegistry.ID_VOLUME_BOOST,
                        ToolRegistry.ID_AUDIO,
                        ToolRegistry.ID_SETTINGS
                    ),
                    volumeBoostPercent = 150,
                    stylePresetId = "original"
                )
                packageName.contains("vlc", ignoreCase = true) || packageName.contains("player", ignoreCase = true) -> AppProfile(
                    packageName = packageName,
                    displayName = displayName,
                    isEnabled = true,
                    enabledToolIds = setOf(
                        ToolRegistry.ID_RECORD,
                        ToolRegistry.ID_SCREENSHOT,
                        ToolRegistry.ID_SCREEN_OFF,
                        ToolRegistry.ID_VOLUME_BOOST,
                        ToolRegistry.ID_STYLE,
                        ToolRegistry.ID_SETTINGS
                    ),
                    volumeBoostPercent = 150,
                    stylePresetId = "cinema"
                )
                packageName.contains("whatsapp", ignoreCase = true) || packageName.contains("telegram", ignoreCase = true) -> AppProfile(
                    packageName = packageName,
                    displayName = displayName,
                    isEnabled = false // Disabled by default on messaging apps
                )
                else -> AppProfile(
                    packageName = packageName,
                    displayName = displayName,
                    isEnabled = true
                )
            }
        }
    }
}

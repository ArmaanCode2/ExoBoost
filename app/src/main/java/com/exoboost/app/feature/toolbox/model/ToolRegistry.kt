package com.exoboost.app.feature.toolbox.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam

object ToolRegistry {

    const val ID_SCREENSHOT = "tool_screenshot"
    const val ID_RECORD = "tool_record"
    const val ID_SCREEN_OFF = "tool_screen_off"
    const val ID_VOLUME_BOOST = "tool_volume_boost"
    const val ID_STYLE = "tool_style"
    const val ID_AUDIO = "tool_audio"
    const val ID_BRIGHTNESS = "tool_brightness"
    const val ID_VOLUME = "tool_volume"
    const val ID_ORIENTATION = "tool_orientation"
    const val ID_CAST = "tool_cast"
    const val ID_TIMER = "tool_timer"
    const val ID_SETTINGS = "tool_settings"
    const val ID_MORE = "tool_settings" // Backward compatibility alias

    val allTools: List<ToolAction> = listOf(
        ToolAction(
            id = ID_SCREENSHOT,
            title = "Screenshot",
            icon = Icons.Default.CameraAlt,
            order = 0,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Clean screen capture without overlays"
        ),
        ToolAction(
            id = ID_RECORD,
            title = "Record",
            icon = Icons.Default.Videocam,
            order = 1,
            availability = ToolAvailability.PREVIEW,
            badge = "Phase 7",
            description = "Screen recording with internal audio"
        ),
        ToolAction(
            id = ID_SCREEN_OFF,
            title = "Screen Off",
            icon = Icons.Default.Bedtime,
            order = 2,
            availability = ToolAvailability.AVAILABLE,
            badge = "Audio",
            description = "Black-screen background audio listening"
        ),
        ToolAction(
            id = ID_VOLUME_BOOST,
            title = "Volume Boost",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            order = 3,
            availability = ToolAvailability.AVAILABLE,
            badge = "+dB",
            description = "Universal audio session 0 booster"
        ),
        ToolAction(
            id = ID_STYLE,
            title = "Style",
            icon = Icons.Default.Palette,
            order = 4,
            availability = ToolAvailability.AVAILABLE,
            badge = "GPU",
            description = "GPU color grading and style filters"
        ),
        ToolAction(
            id = ID_AUDIO,
            title = "Audio",
            icon = Icons.Default.GraphicEq,
            order = 5,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Audio equalizer and clarity controls"
        ),
        ToolAction(
            id = ID_BRIGHTNESS,
            title = "Brightness",
            icon = Icons.Default.Brightness6,
            order = 6,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Display brightness quick control"
        ),
        ToolAction(
            id = ID_VOLUME,
            title = "Volume",
            icon = Icons.AutoMirrored.Filled.VolumeDown,
            order = 7,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Media playback volume slider"
        ),
        ToolAction(
            id = ID_ORIENTATION,
            title = "Orientation",
            icon = Icons.Default.ScreenRotation,
            order = 8,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Lock / toggle portrait & landscape"
        ),
        ToolAction(
            id = ID_CAST,
            title = "Cast",
            icon = Icons.Default.Cast,
            order = 9,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Wireless display and screen projection"
        ),
        ToolAction(
            id = ID_TIMER,
            title = "Timer",
            icon = Icons.Default.Timer,
            order = 10,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Sleep timer for audio/video playback"
        ),
        ToolAction(
            id = ID_SETTINGS,
            title = "Settings",
            icon = Icons.Default.Settings,
            order = 11,
            availability = ToolAvailability.AVAILABLE,
            badge = null,
            description = "Configure ExoBoost toolbox & profiles"
        )
    )

    fun getDefaultActions(): List<ToolAction> = allTools

    fun getToolById(id: String): ToolAction? {
        return allTools.find { it.id == id }
    }

    val defaultActiveIds: List<String> = listOf(
        ID_SCREENSHOT,
        ID_RECORD,
        ID_SCREEN_OFF,
        ID_VOLUME_BOOST,
        ID_STYLE,
        ID_AUDIO,
        ID_BRIGHTNESS,
        ID_SETTINGS
    )
}

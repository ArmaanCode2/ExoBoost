package com.exoboost.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.exoboost.app.core.common.Constants
import com.exoboost.app.feature.toolbox.model.ToolRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "exoboost_settings")

data class ExoSettings(
    val isServiceEnabled: Boolean = false,
    val autoStartOnBoot: Boolean = true,
    val handleSide: String = Constants.DEFAULT_HANDLE_SIDE,
    val handleYPercent: Float = Constants.DEFAULT_HANDLE_Y_PERCENT,
    val handleWidthDp: Int = Constants.DEFAULT_HANDLE_WIDTH_DP,
    val handleHeightDp: Int = Constants.DEFAULT_HANDLE_HEIGHT_DP,
    val handleAlpha: Float = Constants.DEFAULT_HANDLE_ALPHA,
    val isHapticEnabled: Boolean = Constants.DEFAULT_HAPTIC_ENABLED,
    val isAnimationEnabled: Boolean = true,
    val toolboxTransparency: Float = 0.85f,
    val toolboxCornerRadiusDp: Int = 26,
    val showScreenshotConfirmation: Boolean = true,
    val volumeBoostPercent: Int = 100,
    val volumeBoostEnabled: Boolean = false,
    val maxBoostLimit: Int = 300,
    val limiterThresholdDb: Float = -1.0f,
    val blackScreenExitGesture: String = "BOTH", // "DOUBLE_TAP", "SWIPE", "BOTH"
    val blackScreenShowHint: Boolean = true,
    val triggerSensitivityPx: Int = 50,
    val hideHandleInFullscreen: Boolean = false,
    val themeAppearance: String = "AMOLED",
    val toolboxLayoutType: String = "4_COLUMN",
    val activeToolIds: List<String> = ToolRegistry.defaultActiveIds
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val HANDLE_SIDE = stringPreferencesKey("handle_side")
        val HANDLE_Y_PERCENT = floatPreferencesKey("handle_y_percent")
        val HANDLE_WIDTH_DP = intPreferencesKey("handle_width_dp")
        val HANDLE_HEIGHT_DP = intPreferencesKey("handle_height_dp")
        val HANDLE_ALPHA = floatPreferencesKey("handle_alpha")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
        val TOOLBOX_TRANSPARENCY = floatPreferencesKey("toolbox_transparency")
        val TOOLBOX_CORNER_RADIUS_DP = intPreferencesKey("toolbox_corner_radius_dp")
        val SHOW_SCREENSHOT_CONFIRMATION = booleanPreferencesKey("show_screenshot_confirmation")
        val VOLUME_BOOST_PERCENT = intPreferencesKey("volume_boost_percent")
        val VOLUME_BOOST_ENABLED = booleanPreferencesKey("volume_boost_enabled")
        val MAX_BOOST_LIMIT = intPreferencesKey("max_boost_limit")
        val LIMITER_THRESHOLD_DB = floatPreferencesKey("limiter_threshold_db")
        val BLACK_SCREEN_EXIT_GESTURE = stringPreferencesKey("black_screen_exit_gesture")
        val BLACK_SCREEN_SHOW_HINT = booleanPreferencesKey("black_screen_show_hint")
        val TRIGGER_SENSITIVITY_PX = intPreferencesKey("trigger_sensitivity_px")
        val HIDE_HANDLE_IN_FULLSCREEN = booleanPreferencesKey("hide_handle_in_fullscreen")
        val THEME_APPEARANCE = stringPreferencesKey("theme_appearance")
        val TOOLBOX_LAYOUT_TYPE = stringPreferencesKey("toolbox_layout_type")
        val ACTIVE_TOOLS_ORDER = stringPreferencesKey("active_tools_order")
    }

    val settingsFlow: Flow<ExoSettings> = context.dataStore.data.map { prefs ->
        val savedOrderStr = prefs[Keys.ACTIVE_TOOLS_ORDER]
        val activeTools = if (!savedOrderStr.isNullOrEmpty()) {
            savedOrderStr.split(",").filter { it.isNotEmpty() }
        } else {
            ToolRegistry.defaultActiveIds
        }

        ExoSettings(
            isServiceEnabled = prefs[Keys.SERVICE_ENABLED] ?: false,
            autoStartOnBoot = prefs[Keys.AUTO_START_ON_BOOT] ?: true,
            handleSide = prefs[Keys.HANDLE_SIDE] ?: Constants.DEFAULT_HANDLE_SIDE,
            handleYPercent = prefs[Keys.HANDLE_Y_PERCENT] ?: Constants.DEFAULT_HANDLE_Y_PERCENT,
            handleWidthDp = prefs[Keys.HANDLE_WIDTH_DP] ?: Constants.DEFAULT_HANDLE_WIDTH_DP,
            handleHeightDp = prefs[Keys.HANDLE_HEIGHT_DP] ?: Constants.DEFAULT_HANDLE_HEIGHT_DP,
            handleAlpha = prefs[Keys.HANDLE_ALPHA] ?: Constants.DEFAULT_HANDLE_ALPHA,
            isHapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: Constants.DEFAULT_HAPTIC_ENABLED,
            isAnimationEnabled = prefs[Keys.ANIMATION_ENABLED] ?: true,
            toolboxTransparency = prefs[Keys.TOOLBOX_TRANSPARENCY] ?: 0.85f,
            toolboxCornerRadiusDp = prefs[Keys.TOOLBOX_CORNER_RADIUS_DP] ?: 26,
            showScreenshotConfirmation = prefs[Keys.SHOW_SCREENSHOT_CONFIRMATION] ?: true,
            volumeBoostPercent = prefs[Keys.VOLUME_BOOST_PERCENT] ?: 100,
            volumeBoostEnabled = prefs[Keys.VOLUME_BOOST_ENABLED] ?: false,
            maxBoostLimit = prefs[Keys.MAX_BOOST_LIMIT] ?: 300,
            limiterThresholdDb = prefs[Keys.LIMITER_THRESHOLD_DB] ?: -1.0f,
            blackScreenExitGesture = prefs[Keys.BLACK_SCREEN_EXIT_GESTURE] ?: "BOTH",
            blackScreenShowHint = prefs[Keys.BLACK_SCREEN_SHOW_HINT] ?: true,
            triggerSensitivityPx = prefs[Keys.TRIGGER_SENSITIVITY_PX] ?: 50,
            hideHandleInFullscreen = prefs[Keys.HIDE_HANDLE_IN_FULLSCREEN] ?: false,
            themeAppearance = prefs[Keys.THEME_APPEARANCE] ?: "AMOLED",
            toolboxLayoutType = prefs[Keys.TOOLBOX_LAYOUT_TYPE] ?: "4_COLUMN",
            activeToolIds = activeTools
        )
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SERVICE_ENABLED] = enabled }
    }

    suspend fun setAutoStartOnBoot(autoStart: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START_ON_BOOT] = autoStart }
    }

    suspend fun setHandleSide(side: String) {
        context.dataStore.edit { it[Keys.HANDLE_SIDE] = side }
    }

    suspend fun setHandleYPercent(percent: Float) {
        context.dataStore.edit { it[Keys.HANDLE_Y_PERCENT] = percent.coerceIn(0.05f, 0.95f) }
    }

    suspend fun setHandleWidthDp(width: Int) {
        context.dataStore.edit { it[Keys.HANDLE_WIDTH_DP] = width.coerceIn(4, 20) }
    }

    suspend fun setHandleHeightDp(height: Int) {
        context.dataStore.edit { it[Keys.HANDLE_HEIGHT_DP] = height.coerceIn(30, 160) }
    }

    suspend fun setHandleAlpha(alpha: Float) {
        context.dataStore.edit { it[Keys.HANDLE_ALPHA] = alpha.coerceIn(0.1f, 1.0f) }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANIMATION_ENABLED] = enabled }
    }

    suspend fun setToolboxTransparency(transparency: Float) {
        context.dataStore.edit { it[Keys.TOOLBOX_TRANSPARENCY] = transparency.coerceIn(0.40f, 0.98f) }
    }

    suspend fun setToolboxCornerRadiusDp(radius: Int) {
        context.dataStore.edit { it[Keys.TOOLBOX_CORNER_RADIUS_DP] = radius.coerceIn(12, 36) }
    }

    suspend fun setShowScreenshotConfirmation(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SCREENSHOT_CONFIRMATION] = show }
    }

    suspend fun setVolumeBoostPercent(percent: Int) {
        context.dataStore.edit { it[Keys.VOLUME_BOOST_PERCENT] = percent.coerceIn(100, 300) }
    }

    suspend fun setVolumeBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOLUME_BOOST_ENABLED] = enabled }
    }

    suspend fun setMaxBoostLimit(limit: Int) {
        context.dataStore.edit { it[Keys.MAX_BOOST_LIMIT] = limit.coerceIn(150, 300) }
    }

    suspend fun setLimiterThresholdDb(db: Float) {
        context.dataStore.edit { it[Keys.LIMITER_THRESHOLD_DB] = db.coerceIn(-6.0f, 0.0f) }
    }

    suspend fun setBlackScreenExitGesture(gesture: String) {
        context.dataStore.edit { it[Keys.BLACK_SCREEN_EXIT_GESTURE] = gesture }
    }

    suspend fun setBlackScreenShowHint(showHint: Boolean) {
        context.dataStore.edit { it[Keys.BLACK_SCREEN_SHOW_HINT] = showHint }
    }

    suspend fun setTriggerSensitivityPx(sensitivity: Int) {
        context.dataStore.edit { it[Keys.TRIGGER_SENSITIVITY_PX] = sensitivity.coerceIn(20, 100) }
    }

    suspend fun setHideHandleInFullscreen(hide: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_HANDLE_IN_FULLSCREEN] = hide }
    }

    suspend fun setThemeAppearance(appearance: String) {
        context.dataStore.edit { it[Keys.THEME_APPEARANCE] = appearance }
    }

    suspend fun setToolboxLayoutType(layout: String) {
        context.dataStore.edit { it[Keys.TOOLBOX_LAYOUT_TYPE] = layout }
    }

    suspend fun setActiveToolOrder(toolIds: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_TOOLS_ORDER] = toolIds.distinct().joinToString(",")
        }
    }

    suspend fun addActiveTool(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.ACTIVE_TOOLS_ORDER]?.split(",") ?: ToolRegistry.defaultActiveIds).toMutableList()
            if (!current.contains(toolId)) {
                current.add(toolId)
                prefs[Keys.ACTIVE_TOOLS_ORDER] = current.joinToString(",")
            }
        }
    }

    suspend fun removeActiveTool(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.ACTIVE_TOOLS_ORDER]?.split(",") ?: ToolRegistry.defaultActiveIds).toMutableList()
            current.remove(toolId)
            prefs[Keys.ACTIVE_TOOLS_ORDER] = current.joinToString(",")
        }
    }

    suspend fun resetToolboxToDefaults() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_TOOLS_ORDER] = ToolRegistry.defaultActiveIds.joinToString(",")
            prefs[Keys.TOOLBOX_LAYOUT_TYPE] = "4_COLUMN"
        }
    }

    suspend fun resetVolumeSettings() {
        context.dataStore.edit { prefs ->
            prefs[Keys.VOLUME_BOOST_PERCENT] = 100
            prefs[Keys.MAX_BOOST_LIMIT] = 300
            prefs[Keys.LIMITER_THRESHOLD_DB] = -1.0f
        }
    }
}

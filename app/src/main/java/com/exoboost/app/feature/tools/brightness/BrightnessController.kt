package com.exoboost.app.feature.tools.brightness

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object BrightnessController {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var writeJob: Job? = null
    private var initialBrightness: Int? = null

    fun canWriteSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    fun createWriteSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getSystemBrightness(context: Context): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (_: Exception) {
            128
        }
    }

    fun getBrightnessPercent(context: Context): Int {
        val raw = getSystemBrightness(context)
        return ((raw.toFloat() / 255f) * 100).toInt().coerceIn(1, 100)
    }

    fun isAutoBrightness(context: Context): Boolean {
        return try {
            val mode = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
            mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (_: Exception) {
            false
        }
    }

    fun recordInitialBrightness(context: Context) {
        if (initialBrightness == null) {
            initialBrightness = getSystemBrightness(context)
        }
    }

    fun restoreInitialBrightness(context: Context): Boolean {
        val initial = initialBrightness ?: return false
        return if (canWriteSettings(context)) {
            setSystemBrightness(context, initial, immediate = true)
            true
        } else false
    }

    fun setBrightnessPercent(context: Context, percent: Int) {
        val rawValue = ((percent.coerceIn(1, 100) / 100f) * 255).toInt().coerceIn(1, 255)
        setSystemBrightness(context, rawValue, immediate = false)
    }

    fun setSystemBrightness(context: Context, rawValue: Int, immediate: Boolean = false) {
        if (!canWriteSettings(context)) return

        recordInitialBrightness(context)

        writeJob?.cancel()
        if (immediate) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    rawValue.coerceIn(1, 255)
                )
            } catch (_: Exception) {}
        } else {
            writeJob = scope.launch {
                delay(40) // 40ms debounce to prevent high-frequency IPC writes during slider drag
                try {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        rawValue.coerceIn(1, 255)
                    )
                } catch (_: Exception) {}
            }
        }
    }
}

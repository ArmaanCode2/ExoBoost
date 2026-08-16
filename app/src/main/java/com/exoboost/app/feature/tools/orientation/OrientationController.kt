package com.exoboost.app.feature.tools.orientation

import android.content.Context
import android.provider.Settings
import android.widget.Toast
import com.exoboost.app.feature.toolbox.model.ToolActionResult
import com.exoboost.app.feature.tools.brightness.BrightnessController

object OrientationController {

    fun isAutoRotationEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun toggleAutoRotation(context: Context): ToolActionResult {
        if (!BrightnessController.canWriteSettings(context)) {
            val intent = BrightnessController.createWriteSettingsIntent(context)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {}
            return ToolActionResult.PermissionRequired("Modify System Settings", intent)
        }

        return try {
            val current = isAutoRotationEnabled(context)
            val next = if (current) 0 else 1
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                next
            )
            val msg = if (next == 1) "Auto-rotation enabled" else "Rotation locked"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            ToolActionResult.Success(msg)
        } catch (e: Exception) {
            val err = "Failed to toggle rotation: ${e.message}"
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            ToolActionResult.Failed(err)
        }
    }
}

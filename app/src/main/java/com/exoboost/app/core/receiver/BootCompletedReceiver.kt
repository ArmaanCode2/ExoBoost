package com.exoboost.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.exoboost.app.core.capability.CapabilityDetector
import com.exoboost.app.core.common.Constants
import com.exoboost.app.core.datastore.PreferencesManager
import com.exoboost.app.feature.overlay.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val preferencesManager = PreferencesManager(context)
            CoroutineScope(Dispatchers.IO).launch {
                val settings = preferencesManager.settingsFlow.first()
                if (settings.isServiceEnabled && settings.autoStartOnBoot && CapabilityDetector.isOverlayPermissionGranted(context)) {
                    val serviceIntent = Intent(context, OverlayService::class.java).apply {
                        action = Constants.ACTION_START_OVERLAY
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}

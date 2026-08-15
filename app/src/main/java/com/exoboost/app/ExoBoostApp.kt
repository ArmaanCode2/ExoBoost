package com.exoboost.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.exoboost.app.core.common.Constants

class ExoBoostApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val overlayChannel = NotificationChannel(
                Constants.CHANNEL_OVERLAY_SERVICE,
                getString(R.string.overlay_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_service_channel_desc)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(overlayChannel)
        }
    }

    companion object {
        lateinit var instance: ExoBoostApp
            private set
    }
}

package com.exoboost.app.feature.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.exoboost.app.MainActivity
import com.exoboost.app.R
import com.exoboost.app.feature.diagnostics.model.PipelineDiagnostics
import com.exoboost.app.feature.diagnostics.model.RuntimeLog
import com.exoboost.app.feature.overlay.SidebarOverlayController
import com.exoboost.app.feature.service.model.ServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExoBoostService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var sidebarOverlayController: SidebarOverlayController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        RuntimeLog.log("EXOBOOST_SERVICE_CREATED")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                RuntimeLog.log("EXOBOOST_SERVICE_START_ACTION")
                _serviceState.value = ServiceState.RUNNING

                startForegroundSession()

                sidebarOverlayController = SidebarOverlayController.getInstance(this).also {
                    it.onServiceStarted()
                }

                PipelineDiagnostics.update {
                    it.copy(
                        isServiceInstanceAlive = true,
                        isOverlayControllerAlive = true
                    )
                }
            }
            ACTION_STOP -> {
                RuntimeLog.log("EXOBOOST_SERVICE_STOP_ACTION")
                handleStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ExoBoost Sidebar Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows while ExoBoost universal sidebar is active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundSession() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (_: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ExoBoostService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ExoBoost")
            .setContentText("Sidebar is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop ExoBoost", stopPendingIntent)
            .build()
    }

    private fun handleStop() {
        _serviceState.value = ServiceState.STOPPING
        sidebarOverlayController?.onServiceStopped()
        sidebarOverlayController = null

        PipelineDiagnostics.update {
            it.copy(
                isServiceInstanceAlive = false,
                isOverlayAttached = false,
                visibilityDecision = "HIDE",
                visibilityReason = "ExoBoost service stopped"
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        _serviceState.value = ServiceState.STOPPED
        RuntimeLog.log("EXOBOOST_SERVICE_STOPPED")
    }

    override fun onDestroy() {
        super.onDestroy()
        handleStop()
        serviceScope.cancel()
        _serviceState.value = ServiceState.STOPPED
        RuntimeLog.log("EXOBOOST_SERVICE_DESTROYED")
    }

    companion object {
        const val ACTION_START = "com.exoboost.action.START_SESSION"
        const val ACTION_STOP = "com.exoboost.action.STOP_SESSION"

        private const val CHANNEL_ID = "exoboost_session_channel"
        private const val NOTIFICATION_ID = 1001

        private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

        fun start(context: Context) {
            if (_serviceState.value.isActive) return
            _serviceState.value = ServiceState.STARTING
            val intent = Intent(context, ExoBoostService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            if (_serviceState.value == ServiceState.STOPPED || _serviceState.value == ServiceState.STOPPING) return
            _serviceState.value = ServiceState.STOPPING
            val intent = Intent(context, ExoBoostService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggle(context: Context) {
            if (_serviceState.value.isActive) {
                stop(context)
            } else {
                start(context)
            }
        }
    }
}

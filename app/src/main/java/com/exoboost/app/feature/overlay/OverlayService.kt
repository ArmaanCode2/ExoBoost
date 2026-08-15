package com.exoboost.app.feature.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.exoboost.app.MainActivity
import com.exoboost.app.R
import com.exoboost.app.core.capability.CapabilityDetector
import com.exoboost.app.core.common.Constants
import com.exoboost.app.core.datastore.ExoSettings
import com.exoboost.app.core.datastore.PreferencesManager
import com.exoboost.app.feature.profiles.data.AppProfileManager
import com.exoboost.app.feature.profiles.detector.ForegroundAppDetector
import com.exoboost.app.feature.profiles.model.AppProfile
import com.exoboost.app.feature.tools.audio.VolumeBoostController
import com.exoboost.app.feature.tools.audio.ui.VolumeBoostDialog
import com.exoboost.app.feature.tools.blackscreen.BlackScreenOverlayController
import com.exoboost.app.feature.tools.screenshot.ScreenshotCaptureActivity
import com.exoboost.app.feature.tools.screenshot.ScreenshotConfirmationOverlay
import com.exoboost.app.feature.tools.style.StyleEngine
import com.exoboost.app.feature.tools.style.live.LiveStreamCaptureEngine
import com.exoboost.app.feature.tools.style.live.LiveStyleCaptureActivity
import com.exoboost.app.feature.tools.style.live.LiveStylePreviewOverlay
import com.exoboost.app.feature.tools.style.model.StylePresetType
import com.exoboost.app.feature.tools.style.ui.StyleDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var windowManager: WindowManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var appProfileManager: AppProfileManager

    private var edgeHandleView: EdgeHandleView? = null
    private var floatingToolboxView: FloatingToolboxView? = null
    private var confirmationOverlay: ScreenshotConfirmationOverlay? = null
    private var blackScreenController: BlackScreenOverlayController? = null
    private var volumeBoostDialog: VolumeBoostDialog? = null
    private var styleDialog: StyleDialog? = null
    private var livePreviewOverlay: LiveStylePreviewOverlay? = null
    private var liveStreamEngine: LiveStreamCaptureEngine? = null

    val volumeBoostController by lazy { VolumeBoostController(this) }

    private var currentSettings: ExoSettings = ExoSettings()
    private var appProfiles: Map<String, AppProfile> = emptyMap()
    private var currentTopPackage: String? = null
    private var isPanelOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        preferencesManager = PreferencesManager(this)
        appProfileManager = AppProfileManager(this)

        blackScreenController = BlackScreenOverlayController(this) {
            showEdgeHandle()
        }

        observeSettings()
        observeAppProfiles()
        startForegroundAppObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!CapabilityDetector.isOverlayPermissionGranted(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(Constants.NOTIFICATION_ID_OVERLAY, createForegroundNotification())

        when (intent?.action) {
            Constants.ACTION_START_OVERLAY -> {
                showEdgeHandle()
            }
            Constants.ACTION_STOP_OVERLAY -> {
                removeOverlayViews()
                stopSelf()
            }
            else -> {
                showEdgeHandle()
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (edgeHandleView != null && !isPanelOpen && blackScreenController?.isBlackScreenActive != true && livePreviewOverlay == null) {
            updateHandleLayoutParams()
        }
    }

    private fun createForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_OVERLAY_SERVICE)
            .setContentTitle(getString(R.string.overlay_service_running))
            .setContentText(getString(R.string.overlay_service_running_desc))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun observeSettings() {
        serviceScope.launch {
            preferencesManager.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                if (!settings.isServiceEnabled) {
                    removeOverlayViews()
                    stopSelf()
                } else {
                    if (edgeHandleView != null && !isPanelOpen && blackScreenController?.isBlackScreenActive != true && livePreviewOverlay == null) {
                        updateHandleLayoutParams()
                    } else if (edgeHandleView == null && !isPanelOpen && blackScreenController?.isBlackScreenActive != true && livePreviewOverlay == null) {
                        evaluateOverlayVisibilityForCurrentApp()
                    }
                }
            }
        }
    }

    private fun observeAppProfiles() {
        serviceScope.launch {
            appProfileManager.profilesFlow.collectLatest { profiles ->
                appProfiles = profiles
                evaluateOverlayVisibilityForCurrentApp()
            }
        }
    }

    private fun startForegroundAppObserver() {
        serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (currentSettings.isServiceEnabled && ForegroundAppDetector.hasUsageStatsPermission(this@OverlayService)) {
                    val topPkg = ForegroundAppDetector.getForegroundPackageName(this@OverlayService)
                    if (topPkg != null && topPkg != currentTopPackage) {
                        currentTopPackage = topPkg
                        launch(Dispatchers.Main) {
                            applyActiveAppProfile(topPkg)
                        }
                    }
                }
                delay(1500) // 1.5s interval to ensure negligible CPU overhead (<0.1%)
            }
        }
    }

    private fun applyActiveAppProfile(packageName: String) {
        val profile = appProfiles[packageName] ?: AppProfile.createDefault(packageName, packageName)

        if (!profile.isEnabled) {
            // App has ExoBoost disabled, hide handle smoothly
            removeEdgeHandle()
        } else {
            // App is enabled
            if (edgeHandleView == null && !isPanelOpen && blackScreenController?.isBlackScreenActive != true && livePreviewOverlay == null) {
                showEdgeHandle()
            }
        }
    }

    private fun evaluateOverlayVisibilityForCurrentApp() {
        val topPkg = currentTopPackage
        if (topPkg != null) {
            val profile = appProfiles[topPkg] ?: AppProfile.createDefault(topPkg, topPkg)
            if (!profile.isEnabled) {
                removeEdgeHandle()
                return
            }
        }
        showEdgeHandle()
    }

    private fun showEdgeHandle() {
        if (edgeHandleView != null || isPanelOpen || blackScreenController?.isBlackScreenActive == true || volumeBoostDialog != null || styleDialog != null || livePreviewOverlay != null) return
        if (!CapabilityDetector.isOverlayPermissionGranted(this)) return

        val topPkg = currentTopPackage
        val profile = if (topPkg != null) appProfiles[topPkg] ?: AppProfile.createDefault(topPkg, topPkg) else null
        if (profile?.isEnabled == false) return

        val side = profile?.handleSide ?: currentSettings.handleSide

        val handle = EdgeHandleView(
            context = this,
            onTriggerPanel = { openToolboxPanel() },
            onPositionUpdated = { newYPercent ->
                serviceScope.launch {
                    preferencesManager.setHandleYPercent(newYPercent)
                }
            }
        ).apply {
            updateAppearance(
                side = side,
                alphaFraction = currentSettings.handleAlpha,
                haptic = currentSettings.isHapticEnabled
            )
        }

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val widthPx = (currentSettings.handleWidthDp * density).toInt()
        val heightPx = (currentSettings.handleHeightDp * density).toInt()
        val yPx = (displayMetrics.heightPixels * currentSettings.handleYPercent).toInt()

        val gravity = if (side == "RIGHT") {
            Gravity.TOP or Gravity.END
        } else {
            Gravity.TOP or Gravity.START
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.x = 0
            this.y = yPx
        }

        try {
            windowManager.addView(handle, params)
            edgeHandleView = handle
        } catch (_: Exception) {}
    }

    private fun updateHandleLayoutParams() {
        val handle = edgeHandleView ?: return
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val widthPx = (currentSettings.handleWidthDp * density).toInt()
        val heightPx = (currentSettings.handleHeightDp * density).toInt()
        val yPx = (displayMetrics.heightPixels * currentSettings.handleYPercent).toInt()

        val topPkg = currentTopPackage
        val profile = if (topPkg != null) appProfiles[topPkg] ?: AppProfile.createDefault(topPkg, topPkg) else null
        val side = profile?.handleSide ?: currentSettings.handleSide

        val gravity = if (side == "RIGHT") {
            Gravity.TOP or Gravity.END
        } else {
            Gravity.TOP or Gravity.START
        }

        val params = (handle.layoutParams as? WindowManager.LayoutParams)?.apply {
            this.width = widthPx
            this.height = heightPx
            this.gravity = gravity
            this.y = yPx
        } ?: return

        handle.updateAppearance(
            side = side,
            alphaFraction = currentSettings.handleAlpha,
            haptic = currentSettings.isHapticEnabled
        )

        try {
            windowManager.updateViewLayout(handle, params)
        } catch (_: Exception) {}
    }

    private fun openToolboxPanel() {
        if (isPanelOpen || blackScreenController?.isBlackScreenActive == true || livePreviewOverlay != null) return
        removeEdgeHandle()

        val topPkg = currentTopPackage
        val profile = if (topPkg != null) appProfiles[topPkg] ?: AppProfile.createDefault(topPkg, topPkg) else null
        val side = profile?.handleSide ?: currentSettings.handleSide
        val orderedActiveTools = if (profile != null) {
            currentSettings.activeToolIds.filter { profile.enabledToolIds.contains(it) }
        } else {
            currentSettings.activeToolIds
        }

        val panel = FloatingToolboxView(
            context = this,
            handleSide = side,
            panelAlpha = currentSettings.toolboxTransparency,
            isAnimationEnabled = currentSettings.isAnimationEnabled,
            layoutType = currentSettings.toolboxLayoutType,
            activeToolIds = orderedActiveTools,
            onDismiss = { closeToolboxPanel() }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(panel, params)
            floatingToolboxView = panel
            isPanelOpen = true
        } catch (_: Exception) {}
    }

    fun triggerScreenshotFlow() {
        closeToolboxPanel()
        removeEdgeHandle()

        val intent = Intent(this, ScreenshotCaptureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        startActivity(intent)
    }

    fun activateBlackScreenMode() {
        closeToolboxPanel()
        removeEdgeHandle()
        blackScreenController?.show()
    }

    fun showVolumeBoostDialog() {
        closeToolboxPanel()
        removeEdgeHandle()

        volumeBoostDialog?.dismiss()
        val dialog = VolumeBoostDialog(
            context = this,
            controller = volumeBoostController,
            onDismissed = {
                volumeBoostDialog = null
                evaluateOverlayVisibilityForCurrentApp()
            }
        )
        dialog.show()
        volumeBoostDialog = dialog
    }

    fun showStyleDialog() {
        closeToolboxPanel()
        removeEdgeHandle()

        styleDialog?.dismiss()
        val dialog = StyleDialog(
            context = this,
            onDismissed = {
                styleDialog = null
                evaluateOverlayVisibilityForCurrentApp()
            }
        )
        dialog.show()
        styleDialog = dialog
    }

    fun triggerLiveStyleStreamFlow() {
        closeToolboxPanel()
        removeEdgeHandle()
        styleDialog?.dismiss()
        styleDialog = null

        val intent = Intent(this, LiveStyleCaptureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        startActivity(intent)
    }

    fun startLiveStyleStream(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data) ?: return

        stopLiveStyleStream()

        val preview = LiveStylePreviewOverlay(this) {
            stopLiveStyleStream()
        }
        preview.show()
        livePreviewOverlay = preview

        val engine = LiveStreamCaptureEngine(this, projection) { frame, metrics ->
            serviceScope.launch(Dispatchers.Main) {
                livePreviewOverlay?.updateFrame(frame, metrics)
            }
        }
        engine.start()
        liveStreamEngine = engine
    }

    fun stopLiveStyleStream() {
        liveStreamEngine?.stop()
        liveStreamEngine = null
        livePreviewOverlay?.dismiss()
        livePreviewOverlay = null
        evaluateOverlayVisibilityForCurrentApp()
    }

    private fun closeToolboxPanel() {
        if (!isPanelOpen) return
        floatingToolboxView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        floatingToolboxView = null
        isPanelOpen = false
        if (blackScreenController?.isBlackScreenActive != true && volumeBoostDialog == null && styleDialog == null && livePreviewOverlay == null) {
            evaluateOverlayVisibilityForCurrentApp()
        }
    }

    private fun removeEdgeHandle() {
        edgeHandleView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        edgeHandleView = null
    }

    private fun removeOverlayViews() {
        removeEdgeHandle()
        floatingToolboxView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        floatingToolboxView = null
        isPanelOpen = false
        blackScreenController?.dismiss()
        confirmationOverlay?.dismiss()
        confirmationOverlay = null
        volumeBoostDialog?.dismiss()
        volumeBoostDialog = null
        styleDialog?.dismiss()
        styleDialog = null
        stopLiveStyleStream()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (instance == this) {
            instance = null
        }
        serviceScope.cancel()
        volumeBoostController.release()
        removeOverlayViews()
    }

    companion object {
        var isRunning: Boolean = false
            private set

        var instance: OverlayService? = null
            private set

        fun restoreOverlayHandle() {
            instance?.evaluateOverlayVisibilityForCurrentApp()
        }

        fun showScreenshotConfirmation(uri: Uri, bitmap: Bitmap?) {
            instance?.let { service ->
                service.confirmationOverlay?.dismiss()
                val overlay = ScreenshotConfirmationOverlay(
                    context = service,
                    uri = uri,
                    previewBitmap = bitmap,
                    onDismissed = {
                        service.confirmationOverlay = null
                    }
                )
                overlay.show()
                service.confirmationOverlay = overlay
            }
        }
    }
}

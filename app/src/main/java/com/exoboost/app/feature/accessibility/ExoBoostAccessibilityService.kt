package com.exoboost.app.feature.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import com.exoboost.app.core.capability.LauncherHomeDetector
import com.exoboost.app.feature.diagnostics.model.PipelineDiagnostics
import com.exoboost.app.feature.diagnostics.model.RuntimeLog
import com.exoboost.app.feature.overlay.SidebarOverlayController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExoBoostAccessibilityService : AccessibilityService() {

    private var sidebarOverlayController: SidebarOverlayController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        RuntimeLog.log("SERVICE_CONNECTED")

        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.notificationTimeout = 50
            this.serviceInfo = info
        } catch (e: Exception) {
            RuntimeLog.error("SERVICE_CONFIG_FAILED exception=${e.message}", e)
        }

        sidebarOverlayController = SidebarOverlayController.getInstance(this)

        PipelineDiagnostics.update {
            it.copy(
                isA11yConnected = true,
                isA11yPermissionEnabled = true,
                isServiceInstanceAlive = true,
                isOverlayControllerAlive = true,
                lastServiceError = "none"
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventPkg = event.packageName?.toString()
        val eventTypeStr = try {
            AccessibilityEvent.eventTypeToString(event.eventType)
        } catch (_: Exception) {
            "EVENT_${event.eventType}"
        }

        RuntimeLog.log("ACCESSIBILITY_EVENT package=$eventPkg type=$eventTypeStr")

        PipelineDiagnostics.update {
            it.copy(
                lastEventType = eventTypeStr,
                lastEventPackage = eventPkg ?: "None",
                isA11yConnected = true,
                isServiceInstanceAlive = true,
                isOverlayControllerAlive = true
            )
        }

        if (eventPkg.isNullOrEmpty()) return

        // Filter out transient system UI overlays and IMEs (keyboards, volume bars, system toasts)
        // so that they do not overwrite the active foreground application package.
        if (LauncherHomeDetector.isTransientSystemUiOrIme(eventPkg)) {
            return
        }

        // Active application or launcher transition
        val controller = sidebarOverlayController ?: SidebarOverlayController.getInstance(this).also {
            sidebarOverlayController = it
        }
        controller.updateActivePackage(eventPkg)
    }

    override fun onInterrupt() {
        RuntimeLog.log("SERVICE_INTERRUPTED")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        sidebarOverlayController?.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (instance == this) {
            instance = null
        }
        sidebarOverlayController = null

        PipelineDiagnostics.update {
            it.copy(
                isA11yConnected = false,
                isServiceInstanceAlive = false
            )
        }
        RuntimeLog.log("SERVICE_DESTROYED")
    }

    fun takeNativeScreenshot(
        onSuccess: (android.graphics.Bitmap) -> Unit,
        onError: (Int) -> Unit
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        try {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)?.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                            hardwareBuffer.close()
                            if (bitmap != null) {
                                onSuccess(bitmap)
                            } else {
                                onError(ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR)
                            }
                        } catch (e: Exception) {
                            onError(ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        onError(errorCode)
                    }
                }
            )
        } else {
            onError(-1)
        }
    }

    companion object {
        var isRunning: Boolean = false
            private set

        var instance: ExoBoostAccessibilityService? = null
            private set

        fun getOverlayController(): SidebarOverlayController? {
            return instance?.sidebarOverlayController ?: SidebarOverlayController.instance
        }
    }
}

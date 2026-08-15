package com.exoboost.app.feature.tools.screenshot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.exoboost.app.core.datastore.PreferencesManager
import com.exoboost.app.feature.overlay.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotCaptureActivity : ComponentActivity() {

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            processScreenCapture(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screenshot capture cancelled", Toast.LENGTH_SHORT).show()
            OverlayService.restoreOverlayHandle()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch projection prompt
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun processScreenCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        if (mediaProjection == null) {
            Toast.makeText(this, "Failed to initialize screen capture", Toast.LENGTH_SHORT).show()
            OverlayService.restoreOverlayHandle()
            finish()
            return
        }

        // Small delay to ensure any transient system dialog is completely dismissed before frame acquisition
        Handler(Looper.getMainLooper()).postDelayed({
            CoroutineScope(Dispatchers.Main).launch {
                val bitmap: Bitmap? = withContext(Dispatchers.Default) {
                    ScreenCaptureEngine.captureFrame(this@ScreenshotCaptureActivity, mediaProjection)
                }
                try {
                    mediaProjection.stop()
                } catch (_: Exception) {}

                if (bitmap != null) {
                    val uri: Uri? = MediaStoreScreenshotSaver.saveScreenshot(this@ScreenshotCaptureActivity, bitmap)
                    OverlayService.restoreOverlayHandle()

                    if (uri != null) {
                        val prefs = PreferencesManager(this@ScreenshotCaptureActivity)
                        val settings = prefs.settingsFlow.first()
                        if (settings.showScreenshotConfirmation) {
                            OverlayService.showScreenshotConfirmation(uri, bitmap)
                        } else {
                            Toast.makeText(this@ScreenshotCaptureActivity, "Screenshot saved to Pictures/Screenshots", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ScreenshotCaptureActivity, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    OverlayService.restoreOverlayHandle()
                    Toast.makeText(this@ScreenshotCaptureActivity, "Screenshot capture timed out", Toast.LENGTH_SHORT).show()
                }

                finish()
            }
        }, 120)
    }
}

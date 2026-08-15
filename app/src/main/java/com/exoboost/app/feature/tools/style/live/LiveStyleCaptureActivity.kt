package com.exoboost.app.feature.tools.style.live

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.exoboost.app.feature.overlay.OverlayService

class LiveStyleCaptureActivity : ComponentActivity() {

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            OverlayService.instance?.startLiveStyleStream(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Live style filter requires screen capture consent", Toast.LENGTH_SHORT).show()
            OverlayService.restoreOverlayHandle()
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}

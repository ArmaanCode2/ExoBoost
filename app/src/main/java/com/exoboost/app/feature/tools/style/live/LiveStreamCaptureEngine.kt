package com.exoboost.app.feature.tools.style.live

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.exoboost.app.feature.tools.style.StyleEngine
import com.exoboost.app.feature.tools.style.engine.ShaderProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

data class LiveStreamMetrics(
    val fps: Float = 0f,
    val latencyMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isDrmProtected: Boolean = false,
    val isRunning: Boolean = false
)

class LiveStreamCaptureEngine(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val onFrameProcessed: (Bitmap, LiveStreamMetrics) -> Unit
) {

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val isProcessing = AtomicBoolean(false)
    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var currentFps = 0f

    private val _metrics = MutableStateFlow(LiveStreamMetrics())
    val metrics: StateFlow<LiveStreamMetrics> = _metrics.asStateFlow()

    fun start() {
        val displayMetrics = context.resources.displayMetrics
        // Downscale capture to 1/2 resolution for optimal GPU performance & zero frame lag
        val width = (displayMetrics.widthPixels / 2).coerceAtLeast(360)
        val height = (displayMetrics.heightPixels / 2).coerceAtLeast(640)
        val density = displayMetrics.densityDpi

        handlerThread = HandlerThread("ExoStyleStreamThread").apply {
            start()
            backgroundHandler = Handler(looper)
        }

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            onImageAvailable(reader, width, height)
        }, backgroundHandler)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ExoBoostLiveStyleVirtualDisplay",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            backgroundHandler
        )

        _metrics.value = LiveStreamMetrics(
            width = width,
            height = height,
            isRunning = true
        )
    }

    private fun onImageAvailable(reader: ImageReader, targetWidth: Int, targetHeight: Int) {
        val startTime = System.currentTimeMillis()

        // Frame dropping: If previous frame is still processing in GPU, drop the new image immediately
        if (!isProcessing.compareAndSet(false, true)) {
            val image = reader.acquireLatestImage()
            image?.close()
            return
        }

        val image = try {
            reader.acquireLatestImage()
        } catch (_: Exception) {
            null
        }

        if (image == null) {
            isProcessing.set(false)
            return
        }

        try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * targetWidth

            val rawBitmap = Bitmap.createBitmap(
                targetWidth + rowPadding / pixelStride,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )
            rawBitmap.copyPixelsFromBuffer(buffer)
            image.close()

            // Crop stride padding
            val cleanBitmap = if (rawBitmap.width > targetWidth) {
                Bitmap.createBitmap(rawBitmap, 0, 0, targetWidth, targetHeight).also {
                    rawBitmap.recycle()
                }
            } else {
                rawBitmap
            }

            // Quick check for DRM black frame (e.g. Netflix, DRM Protected Streams)
            val isBlackout = checkIfDrmBlackout(cleanBitmap)

            // Apply active StyleEngine color grading
            val activeParams = StyleEngine.activeParameters.value
            val styledBitmap = ShaderProcessor.processBitmap(cleanBitmap, activeParams)
            cleanBitmap.recycle()

            val latency = System.currentTimeMillis() - startTime
            updateFps()

            val updatedMetrics = LiveStreamMetrics(
                fps = currentFps,
                latencyMs = latency,
                width = targetWidth,
                height = targetHeight,
                isDrmProtected = isBlackout,
                isRunning = true
            )
            _metrics.value = updatedMetrics

            onFrameProcessed(styledBitmap, updatedMetrics)
        } catch (_: Throwable) {
            try { image.close() } catch (_: Throwable) {}
        } finally {
            isProcessing.set(false)
        }
    }

    private fun updateFps() {
        frameCount++
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsTimestamp
        if (elapsed >= 1000) {
            currentFps = (frameCount * 1000f) / elapsed
            frameCount = 0
            lastFpsTimestamp = now
        }
    }

    private fun checkIfDrmBlackout(bitmap: Bitmap): Boolean {
        // Sample center pixels to detect DRM black output
        val cx = bitmap.width / 2
        val cy = bitmap.height / 2
        val pixel1 = bitmap.getPixel(cx, cy)
        val pixel2 = bitmap.getPixel(cx / 2, cy / 2)
        val pixel3 = bitmap.getPixel((cx * 3) / 2, (cy * 3) / 2)
        return (pixel1 == 0xFF000000.toInt() && pixel2 == 0xFF000000.toInt() && pixel3 == 0xFF000000.toInt())
    }

    fun stop() {
        _metrics.value = LiveStreamMetrics(isRunning = false)
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (_: Throwable) {}

        try {
            imageReader?.close()
            imageReader = null
        } catch (_: Throwable) {}

        try {
            mediaProjection.stop()
        } catch (_: Throwable) {}

        handlerThread?.quitSafely()
        handlerThread = null
        backgroundHandler = null
    }
}

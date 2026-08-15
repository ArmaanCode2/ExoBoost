package com.exoboost.app.feature.tools.screenshot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object ScreenCaptureEngine {

    @SuppressLint("WrongConstant")
    suspend fun captureFrame(
        context: Context,
        mediaProjection: MediaProjection
    ): Bitmap? = withContext(Dispatchers.Default) {
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null

        try {
            val bitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
                val handler = Handler(Looper.getMainLooper())
                var captured = false

                // Timeout fallback after 1500ms
                val timeoutRunnable = Runnable {
                    if (!captured && continuation.isActive) {
                        captured = true
                        continuation.resume(null)
                    }
                }
                handler.postDelayed(timeoutRunnable, 1500)

                imageReader.setOnImageAvailableListener({ reader ->
                    if (captured) return@setOnImageAvailableListener
                    val image: Image? = try {
                        reader.acquireLatestImage()
                    } catch (_: Exception) {
                        null
                    }

                    if (image != null) {
                        captured = true
                        handler.removeCallbacks(timeoutRunnable)
                        val bmp = extractBitmapFromImage(image, width, height)
                        image.close()
                        if (continuation.isActive) {
                            continuation.resume(bmp)
                        }
                    }
                }, handler)

                virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ExoBoostScreenshot",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    handler
                )
            }

            return@withContext bitmap
        } finally {
            virtualDisplay?.release()
            imageReader.close()
        }
    }

    private fun extractBitmapFromImage(image: Image, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * targetWidth

            val bitmap = Bitmap.createBitmap(
                targetWidth + rowPadding / pixelStride,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding == 0) {
                bitmap
            } else {
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, targetWidth, targetHeight)
                bitmap.recycle()
                cropped
            }
        } catch (e: Exception) {
            null
        }
    }
}

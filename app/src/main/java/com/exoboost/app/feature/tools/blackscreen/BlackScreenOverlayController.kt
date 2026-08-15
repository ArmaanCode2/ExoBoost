package com.exoboost.app.feature.tools.blackscreen

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs

class BlackScreenOverlayController(
    private val context: Context,
    private val onDismissed: () -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayContainer: FrameLayout? = null
    private var hintTextView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    var isBlackScreenActive: Boolean = false
        private set

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isBlackScreenActive) return

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
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenBrightness = 0.01f // Dims backlight/display to lowest level possible

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val frame = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK) // Pure #000000 black surface
            isClickable = true
            isFocusable = true
        }

        val hint = TextView(context).apply {
            text = "Double-tap or swipe to exit Black Screen"
            setTextColor(0x44FFFFFF) // 27% faint white
            textSize = 12f
            gravity = Gravity.CENTER
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 140
            }
            layoutParams = lp
        }
        frame.addView(hint)
        hintTextView = hint

        // Fade out hint text after 3 seconds
        handler.postDelayed({
            hintTextView?.let { tv ->
                ObjectAnimator.ofFloat(tv, View.ALPHA, tv.alpha, 0f).apply {
                    duration = 1000
                    start()
                }
            }
        }, 3000)

        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                dismiss()
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    val deltaY = e1.y - e2.y
                    val deltaX = abs(e1.x - e2.x)
                    // Swipe up or broad horizontal swipe
                    if (deltaY > 120 || deltaX > 180) {
                        dismiss()
                        return true
                    }
                }
                return false
            }
        })

        frame.setOnTouchListener { _, event ->
            // Wake up hint text briefly if user touches screen
            if (hintTextView != null && hintTextView!!.alpha < 0.1f) {
                hintTextView!!.alpha = 0.6f
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    hintTextView?.let { tv ->
                        ObjectAnimator.ofFloat(tv, View.ALPHA, tv.alpha, 0f).apply {
                            duration = 1000
                            start()
                        }
                    }
                }, 2500)
            }
            gestureDetector.onTouchEvent(event)
            true // Intercept touches to prevent accidental background app interaction while in pocket
        }

        try {
            windowManager.addView(frame, params)
            overlayContainer = frame
            isBlackScreenActive = true
        } catch (_: Exception) {}
    }

    fun dismiss() {
        if (!isBlackScreenActive) return
        handler.removeCallbacksAndMessages(null)
        overlayContainer?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        overlayContainer = null
        hintTextView = null
        isBlackScreenActive = false
        onDismissed()
    }
}

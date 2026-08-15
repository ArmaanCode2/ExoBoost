package com.exoboost.app.feature.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class EdgeHandleView(
    context: Context,
    private val onTriggerPanel: () -> Unit,
    private val onPositionUpdated: (Float) -> Unit
) : View(context) {

    private val baseColor = 0xFFFFFFFF.toInt()
    private var currentAlpha = 0.75f
    private var isPressedState = false

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((currentAlpha * 255).toInt(), 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x2238BDF8.toInt() // Soft light blue accent glow
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val pillRect = RectF()
    private var initialTouchY = 0f
    private var initialTouchX = 0f
    private var initialParamY = 0
    private var isDragging = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val swipeThreshold = 50f

    var isHapticEnabled: Boolean = true
    var handleSide: String = "RIGHT"

    private var scaleMultiplier = 1.0f

    fun updateAppearance(side: String, alphaFraction: Float, haptic: Boolean) {
        handleSide = side
        currentAlpha = alphaFraction
        isHapticEnabled = haptic
        val alphaInt = (currentAlpha * 255).toInt().coerceIn(20, 255)
        pillPaint.color = Color.argb(alphaInt, 255, 255, 255)
        invalidate()
    }

    private fun animatePress(pressed: Boolean) {
        isPressedState = pressed
        val targetScale = if (pressed) 1.25f else 1.0f
        val targetAlpha = if (pressed) 0.95f else currentAlpha

        ValueAnimator.ofFloat(scaleMultiplier, targetScale).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                scaleMultiplier = animator.animatedValue as Float
                val animatedAlpha = (targetAlpha * 255).toInt().coerceIn(20, 255)
                pillPaint.color = Color.argb(animatedAlpha, 255, 255, 255)
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        val drawW = w * scaleMultiplier
        val drawH = h
        val radius = drawW / 2f

        // Position pill flush against screen edge
        if (handleSide == "RIGHT") {
            pillRect.set(w - drawW, 0f, w, drawH)
        } else {
            pillRect.set(0f, 0f, drawW, drawH)
        }

        if (isPressedState) {
            canvas.drawRoundRect(pillRect, radius, radius, glowPaint)
        }
        canvas.drawRoundRect(pillRect, radius, radius, pillPaint)
        canvas.drawRoundRect(pillRect, radius, radius, borderPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val wmParams = layoutParams as? WindowManager.LayoutParams ?: return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialParamY = wmParams.y
                isDragging = false
                animatePress(true)
                triggerHaptic(VibrationEffect.EFFECT_TICK)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                // Check inward swipe gesture towards screen center
                val inwardSwipe = if (handleSide == "RIGHT") -deltaX else deltaX
                if (inwardSwipe > swipeThreshold && abs(deltaY) < 80f) {
                    animatePress(false)
                    triggerHaptic(VibrationEffect.EFFECT_CLICK)
                    onTriggerPanel()
                    return true
                }

                // Vertical drag
                if (abs(deltaY) > touchSlop) {
                    isDragging = true
                    val displayMetrics = resources.displayMetrics
                    val maxSafeY = displayMetrics.heightPixels - height - 40
                    val minSafeY = 40
                    wmParams.y = (initialParamY + deltaY).toInt().coerceIn(minSafeY, maxSafeY)

                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    try {
                        windowManager.updateViewLayout(this, wmParams)
                    } catch (_: Exception) {}
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animatePress(false)
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (!isDragging && abs(deltaX) < touchSlop && abs(deltaY) < touchSlop) {
                    // Tap also reveals toolbox for accessibility
                    triggerHaptic(VibrationEffect.EFFECT_CLICK)
                    onTriggerPanel()
                } else if (isDragging) {
                    // Calculate and persist new vertical percentage
                    val displayMetrics = resources.displayMetrics
                    val screenHeight = displayMetrics.heightPixels.toFloat()
                    val yPercent = (wmParams.y.toFloat() / screenHeight).coerceIn(0.05f, 0.95f)
                    onPositionUpdated(yPercent)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun triggerHaptic(effectId: Int) {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(effectId))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createPredefined(effectId))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(15)
            }
        } catch (_: Exception) {}
    }
}

package com.exoboost.app.feature.tools.timer.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exoboost.app.core.designsystem.ElectricBlue
import com.exoboost.app.core.designsystem.EmeraldGreen
import com.exoboost.app.core.designsystem.GlassBorder
import com.exoboost.app.core.designsystem.Slate200
import com.exoboost.app.core.designsystem.Slate400
import com.exoboost.app.core.designsystem.Slate800
import com.exoboost.app.core.designsystem.Slate900
import com.exoboost.app.feature.overlay.OverlayLifecycleOwner
import com.exoboost.app.feature.tools.timer.SleepTimerManager
import java.util.Locale

class SleepTimerDialog(
    private val context: Context,
    private val onDismissed: () -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var containerView: FrameLayout? = null
    private val lifecycleOwner = OverlayLifecycleOwner()

    fun show() {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setContent {
                SleepTimerCard(
                    onStartTimer = { mins ->
                        SleepTimerManager.startTimer(context, mins)
                        dismiss()
                    },
                    onCancelTimer = {
                        SleepTimerManager.cancelTimer()
                    },
                    onDismiss = { dismiss() }
                )
            }
        }

        val frame = FrameLayout(context).apply {
            lifecycleOwner.attachToView(this)
            lifecycleOwner.attachToView(composeView)
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(frame, params)
            containerView = frame
        } catch (_: Exception) {}
    }

    fun dismiss() {
        containerView?.let { view ->
            try {
                lifecycleOwner.destroy()
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        containerView = null
        onDismissed()
    }
}

@Composable
fun SleepTimerCard(
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val isRunning by SleepTimerManager.isTimerRunning.collectAsState()
    val remainingSeconds by SleepTimerManager.remainingSeconds.collectAsState()

    val formattedTime = remember(remainingSeconds) {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .width(320.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
            color = Slate900.copy(alpha = 0.95f),
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Playback Sleep Timer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isRunning) {
                    // Running status
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldGreen.copy(alpha = 0.12f))
                            .border(0.5.dp, EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Pausing playback in",
                                fontSize = 12.sp,
                                color = Slate200
                            )
                            Text(
                                text = formattedTime,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldGreen
                            )
                        }
                    }

                    Button(
                        onClick = onCancelTimer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Timer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Preset Selection Buttons
                    Text(
                        text = "Automatically pause media playback after:",
                        fontSize = 12.sp,
                        color = Slate400
                    )

                    val presets = listOf(15, 30, 45, 60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate800)
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                                    .clickable { onStartTimer(mins) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Done / Dismiss
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontSize = 12.sp, color = Slate200)
                }
            }
        }
    }
}

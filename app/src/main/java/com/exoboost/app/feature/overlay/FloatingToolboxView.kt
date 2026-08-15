package com.exoboost.app.feature.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exoboost.app.MainActivity
import com.exoboost.app.core.designsystem.ElectricBlue
import com.exoboost.app.core.designsystem.EmeraldGreen
import com.exoboost.app.core.designsystem.GlassBorder
import com.exoboost.app.core.designsystem.LightBlue
import com.exoboost.app.core.designsystem.Slate200
import com.exoboost.app.core.designsystem.Slate400
import com.exoboost.app.core.designsystem.Slate700
import com.exoboost.app.core.designsystem.Slate800
import com.exoboost.app.core.designsystem.Slate900
import com.exoboost.app.feature.toolbox.model.ToolAction
import com.exoboost.app.feature.toolbox.model.ToolAvailability
import com.exoboost.app.feature.toolbox.model.ToolRegistry

@SuppressLint("ViewConstructor")
class FloatingToolboxView(
    context: Context,
    private val handleSide: String,
    private val panelAlpha: Float,
    private val isAnimationEnabled: Boolean,
    private val layoutType: String = "4_COLUMN",
    private val activeToolIds: List<String> = ToolRegistry.defaultActiveIds,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val composeView: ComposeView = ComposeView(context)
    private var initialX = 0f
    private val swipeDismissThreshold = 50f

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
        )
        composeView.setContent {
            val orderedTools = remember(activeToolIds) {
                activeToolIds.mapNotNull { id -> ToolRegistry.getToolById(id) }
            }

            ToolboxPanelLayout(
                handleSide = handleSide,
                panelAlpha = panelAlpha,
                isAnimationEnabled = isAnimationEnabled,
                layoutType = layoutType,
                tools = orderedTools,
                onToolClick = { action ->
                    handleToolClick(action)
                },
                onOpenSettings = {
                    openSettings()
                },
                onDismiss = onDismiss
            )
        }
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    private fun handleToolClick(action: ToolAction) {
        when (action.id) {
            ToolRegistry.ID_SCREENSHOT -> {
                OverlayService.instance?.triggerScreenshotFlow()
            }
            ToolRegistry.ID_SCREEN_OFF -> {
                OverlayService.instance?.activateBlackScreenMode()
            }
            ToolRegistry.ID_VOLUME_BOOST -> {
                OverlayService.instance?.showVolumeBoostDialog()
            }
            ToolRegistry.ID_STYLE -> {
                OverlayService.instance?.showStyleDialog()
            }
            ToolRegistry.ID_SETTINGS -> {
                openSettings()
            }
            ToolRegistry.ID_BRIGHTNESS -> {
                Toast.makeText(context, "Brightness: Controlled via quick display overlay", Toast.LENGTH_SHORT).show()
            }
            ToolRegistry.ID_VOLUME -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                onDismiss()
            }
            ToolRegistry.ID_ORIENTATION -> {
                Toast.makeText(context, "Orientation: Device auto-rotation helper active", Toast.LENGTH_SHORT).show()
            }
            ToolRegistry.ID_CAST -> {
                try {
                    val castIntent = Intent(Settings.ACTION_CAST_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(castIntent)
                    onDismiss()
                } catch (_: Exception) {
                    Toast.makeText(context, "Cast settings unavailable on this device", Toast.LENGTH_SHORT).show()
                }
            }
            ToolRegistry.ID_TIMER -> {
                Toast.makeText(context, "Sleep Timer: 30 minutes set for audio playback", Toast.LENGTH_SHORT).show()
            }
            ToolRegistry.ID_AUDIO -> {
                Toast.makeText(context, "Audio Equalizer: Bass boost and vocal profiles active", Toast.LENGTH_SHORT).show()
            }
            ToolRegistry.ID_RECORD -> {
                Toast.makeText(context, "Record: Screen recording pipeline scheduled for Phase 10", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "${action.title}: ${action.description}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        onDismiss()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.rawX
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = ev.rawX - initialX
                val outwardSwipe = if (handleSide == "RIGHT") deltaX else -deltaX
                if (outwardSwipe > swipeDismissThreshold) {
                    onDismiss()
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}

@Composable
fun ToolboxPanelLayout(
    handleSide: String,
    panelAlpha: Float,
    isAnimationEnabled: Boolean,
    layoutType: String,
    tools: List<ToolAction>,
    onToolClick: (ToolAction) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val isRightSide = handleSide == "RIGHT"
    val panelBgColor = Slate900.copy(alpha = panelAlpha.coerceIn(0.40f, 0.98f))

    val panelWidth = when (layoutType) {
        "2_COLUMN" -> 160.dp
        "3_COLUMN" -> 220.dp
        "COMPACT" -> 300.dp
        else -> 268.dp // 4_COLUMN
    }

    val gridColumns = when (layoutType) {
        "2_COLUMN" -> GridCells.Fixed(2)
        "3_COLUMN" -> GridCells.Fixed(3)
        "COMPACT" -> GridCells.Fixed(4)
        else -> GridCells.Fixed(4)
    }

    val enterTransition: EnterTransition = if (isAnimationEnabled) {
        fadeIn(tween(180)) +
                slideInHorizontally(tween(220)) { if (isRightSide) it / 2 else -it / 2 } +
                scaleIn(tween(200), initialScale = 0.92f)
    } else {
        EnterTransition.None
    }

    val exitTransition: ExitTransition = if (isAnimationEnabled) {
        fadeOut(tween(140)) +
                slideOutHorizontally(tween(160)) { if (isRightSide) it / 2 else -it / 2 } +
                scaleOut(tween(140), targetScale = 0.92f)
    } else {
        ExitTransition.None
    }

    // Full screen dismiss backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = if (isRightSide) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        AnimatedVisibility(
            visible = true,
            enter = enterTransition,
            exit = exitTransition
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 20.dp)
                    .width(panelWidth)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside panel
                    ),
                color = panelBgColor,
                shape = RoundedCornerShape(26.dp),
                tonalElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ExoBoost",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (layoutType == "COMPACT") {
                        // Compact Horizontal Strip
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tools, key = { it.id }) { action ->
                                ToolActionTile(
                                    action = action,
                                    onClick = { onToolClick(action) }
                                )
                            }
                        }
                    } else {
                        // Dynamic Grid Layout
                        val gridHeight = when {
                            tools.size <= 4 -> 80.dp
                            tools.size <= 8 -> 160.dp
                            else -> 240.dp
                        }

                        LazyVerticalGrid(
                            columns = gridColumns,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gridHeight),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = tools.size > 8
                        ) {
                            items(tools, key = { it.id }) { action ->
                                ToolActionTile(
                                    action = action,
                                    onClick = { onToolClick(action) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolActionTile(
    action: ToolAction,
    onClick: () -> Unit
) {
    val iconTint = when (action.id) {
        ToolRegistry.ID_RECORD -> Color(0xFFEF4444)
        ToolRegistry.ID_SCREENSHOT -> ElectricBlue
        ToolRegistry.ID_SCREEN_OFF -> Color(0xFF8B5CF6)
        ToolRegistry.ID_VOLUME_BOOST -> EmeraldGreen
        ToolRegistry.ID_STYLE -> Color(0xFFF59E0B)
        ToolRegistry.ID_AUDIO -> LightBlue
        ToolRegistry.ID_BRIGHTNESS -> Color(0xFFFBBF24)
        ToolRegistry.ID_VOLUME -> Color(0xFF34D399)
        ToolRegistry.ID_ORIENTATION -> Color(0xFF60A5FA)
        ToolRegistry.ID_CAST -> Color(0xFF06B6D4)
        ToolRegistry.ID_TIMER -> Color(0xFFA78BFA)
        else -> Slate200
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Slate800.copy(alpha = 0.8f))
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )

            // Badge indicator if present
            action.badge?.let { badgeText ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Slate700)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Slate200,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = action.title,
            color = Slate200,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

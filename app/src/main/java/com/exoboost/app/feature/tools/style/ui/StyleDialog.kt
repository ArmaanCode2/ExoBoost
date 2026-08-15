package com.exoboost.app.feature.tools.style.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exoboost.app.core.designsystem.ElectricBlue
import com.exoboost.app.core.designsystem.EmeraldGreen
import com.exoboost.app.core.designsystem.GlassBorder
import com.exoboost.app.core.designsystem.Slate200
import com.exoboost.app.core.designsystem.Slate400
import com.exoboost.app.core.designsystem.Slate800
import com.exoboost.app.core.designsystem.Slate900
import com.exoboost.app.feature.overlay.OverlayService
import com.exoboost.app.feature.tools.style.StyleEngine
import com.exoboost.app.feature.tools.style.engine.ShaderProcessor
import com.exoboost.app.feature.tools.style.model.StyleParameters
import com.exoboost.app.feature.tools.style.model.StylePresetType

class StyleDialog(
    private val context: Context,
    private val onDismissed: () -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var containerView: FrameLayout? = null

    fun show() {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setContent {
                val activePreset by StyleEngine.activePreset.collectAsState()
                val activeParams by StyleEngine.activeParameters.collectAsState()

                StyleCard(
                    activePreset = activePreset,
                    activeParams = activeParams,
                    onSelectPreset = { StyleEngine.selectPreset(it) },
                    onUpdateCustomParams = { StyleEngine.updateCustomParameters(it) },
                    onLaunchLiveFilter = {
                        OverlayService.instance?.triggerLiveStyleStreamFlow()
                    },
                    onReset = { StyleEngine.reset() },
                    onDismiss = { dismiss() }
                )
            }
        }

        val frame = FrameLayout(context).apply {
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

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
            windowManager.addView(frame, params)
            containerView = frame
        } catch (_: Exception) {}
    }

    fun dismiss() {
        containerView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        containerView = null
        onDismissed()
    }
}

@Composable
fun StyleCard(
    activePreset: StylePresetType,
    activeParams: StyleParameters,
    onSelectPreset: (StylePresetType) -> Unit,
    onUpdateCustomParams: (StyleParameters) -> Unit,
    onLaunchLiveFilter: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val presets = remember { StylePresetType.values() }
    val baseSample = remember { StyleEngine.getSampleBitmap() }
    val livePreviewBitmap = remember(activeParams) {
        ShaderProcessor.processBitmap(baseSample, activeParams)
    }
    val scrollState = rememberScrollState()

    // Full screen backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .width(340.dp)
                    .height(620.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept click inside card
                    ),
                color = Slate900.copy(alpha = 0.96f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Style Engine",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "GPU Color Grading Preset",
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Live Test Preview Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = livePreviewBitmap.asImageBitmap(),
                            contentDescription = "Live style preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Active Preset Pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate900.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Preview: ${activePreset.displayName}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Experimental Live Stream Filter Button
                    Button(
                        onClick = onLaunchLiveFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live App Filter (Experimental PIP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 2-Column Preset Grid
                    Text(
                        text = "Presets",
                        color = Slate200,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = true
                    ) {
                        items(presets, key = { it.id }) { preset ->
                            val isSelected = activePreset == preset
                            val thumb = remember(preset) { StyleEngine.getPreviewForPreset(preset) }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate800.copy(alpha = 0.8f))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.5.dp,
                                        color = if (isSelected) ElectricBlue else GlassBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectPreset(preset) }
                                    .padding(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = preset.displayName,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = preset.displayName,
                                            color = if (isSelected) ElectricBlue else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = preset.description,
                                            color = Slate400,
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom Sliders if Custom preset selected
                    if (activePreset == StylePresetType.CUSTOM) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Slate800.copy(alpha = 0.7f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Custom Parameters", color = Slate200, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            // Contrast
                            Column {
                                Text("Contrast: ${String.format("%.2f", activeParams.contrast)}", color = Slate400, fontSize = 10.sp)
                                Slider(
                                    value = activeParams.contrast,
                                    onValueChange = { onUpdateCustomParams(activeParams.copy(contrast = it)) },
                                    valueRange = 0.5f..1.8f,
                                    colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                                )
                            }

                            // Saturation
                            Column {
                                Text("Saturation: ${String.format("%.2f", activeParams.saturation)}", color = Slate400, fontSize = 10.sp)
                                Slider(
                                    value = activeParams.saturation,
                                    onValueChange = { onUpdateCustomParams(activeParams.copy(saturation = it)) },
                                    valueRange = 0.0f..2.0f,
                                    colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                                )
                            }

                            // Temperature
                            Column {
                                Text("Temperature: ${String.format("%.2f", activeParams.temperature)}", color = Slate400, fontSize = 10.sp)
                                Slider(
                                    value = activeParams.temperature,
                                    onValueChange = { onUpdateCustomParams(activeParams.copy(temperature = it)) },
                                    valueRange = -1.0f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                                )
                            }

                            // Vignette
                            Column {
                                Text("Vignette: ${String.format("%.2f", activeParams.vignette)}", color = Slate400, fontSize = 10.sp)
                                Slider(
                                    value = activeParams.vignette,
                                    onValueChange = { onUpdateCustomParams(activeParams.copy(vignette = it)) },
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Slate200, activeTrackColor = Slate200)
                                )
                            }
                        }
                    }

                    // Disclaimer Note
                    Text(
                        text = "ExoBoost Style Engine processes test media and internal pipelines. Android does not permit overlay filters to directly rewrite other apps' private video streams without full screen capture projection.",
                        color = Slate400,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )

                    // Reset Button
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset to Original", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

package com.exoboost.app.feature.tools.style

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.exoboost.app.feature.tools.style.engine.ShaderProcessor
import com.exoboost.app.feature.tools.style.model.StyleParameters
import com.exoboost.app.feature.tools.style.model.StylePresetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StyleEngine {

    private val _activePreset = MutableStateFlow(StylePresetType.ORIGINAL)
    val activePreset: StateFlow<StylePresetType> = _activePreset.asStateFlow()

    private val _activeParameters = MutableStateFlow(StyleParameters())
    val activeParameters: StateFlow<StyleParameters> = _activeParameters.asStateFlow()

    private var cachedSampleBitmap: Bitmap? = null
    private val previewCache = mutableMapOf<StylePresetType, Bitmap>()

    fun selectPreset(preset: StylePresetType) {
        _activePreset.value = preset
        _activeParameters.value = preset.getParameters()
    }

    fun updateCustomParameters(params: StyleParameters) {
        _activePreset.value = StylePresetType.CUSTOM
        _activeParameters.value = params
    }

    fun reset() {
        selectPreset(StylePresetType.ORIGINAL)
    }

    /**
     * Generates or retrieves a vibrant reference scene with sky, sunset gradient, mountain, and lake.
     */
    fun getSampleBitmap(): Bitmap {
        cachedSampleBitmap?.let { return it }

        val width = 360
        val height = 240
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Sunset Sky Gradient
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height * 0.6f,
                intArrayOf(Color.rgb(25, 42, 86), Color.rgb(225, 112, 85), Color.rgb(253, 203, 110)),
                floatArrayOf(0.0f, 0.6f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.6f, skyPaint)

        // 2. Sun
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 234, 167)
        }
        canvas.drawCircle(width * 0.7f, height * 0.45f, 28f, sunPaint)

        // 3. Mountain Silhouette
        val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(45, 52, 54)
        }
        val mountainPath = Path().apply {
            moveTo(0f, height * 0.65f)
            lineTo(width * 0.25f, height * 0.35f)
            lineTo(width * 0.5f, height * 0.55f)
            lineTo(width * 0.75f, height * 0.30f)
            lineTo(width.toFloat(), height * 0.65f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(mountainPath, mountainPaint)

        // 4. Lake & Reflections
        val waterPaint = Paint().apply {
            shader = LinearGradient(
                0f, height * 0.65f, 0f, height.toFloat(),
                intArrayOf(Color.rgb(9, 132, 227), Color.rgb(44, 62, 80)),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height * 0.65f, width.toFloat(), height.toFloat(), waterPaint)

        cachedSampleBitmap = bitmap
        return bitmap
    }

    /**
     * Obtains a styled thumbnail for UI cards.
     */
    fun getPreviewForPreset(preset: StylePresetType): Bitmap {
        previewCache[preset]?.let { return it }
        val sample = getSampleBitmap()
        val processed = ShaderProcessor.processBitmap(sample, preset.getParameters())
        previewCache[preset] = processed
        return processed
    }
}

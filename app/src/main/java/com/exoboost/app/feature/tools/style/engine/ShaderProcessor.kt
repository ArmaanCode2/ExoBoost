package com.exoboost.app.feature.tools.style.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.exoboost.app.feature.tools.style.model.StyleParameters
import java.util.Random

object ShaderProcessor {

    /**
     * GLSL Fragment Shader for direct GPU Texture pipeline (OpenGL ES 2.0 / 3.0).
     * Ready for hardware surface streaming and MediaProjection texture rendering.
     */
    const val FRAGMENT_SHADER_GLSL = """
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform sampler2D sTexture;
        uniform float uBrightness;
        uniform float uContrast;
        uniform float uSaturation;
        uniform float uTemperature;
        uniform float uTint;
        uniform float uGamma;
        uniform float uVignette;
        uniform float uFilmGrain;
        uniform float uTime;

        // Pseudo-random noise for film grain
        float rand(vec2 co) {
            return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
        }

        void main() {
            vec4 color = texture2D(sTexture, vTextureCoord);
            
            // 1. Brightness
            color.rgb += uBrightness;
            
            // 2. Contrast
            color.rgb = (color.rgb - 0.5) * uContrast + 0.5;
            
            // 3. Saturation & Monochrome
            float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            color.rgb = mix(vec3(luma), color.rgb, uSaturation);
            
            // 4. Color Temperature (Amber / Blue shift)
            color.r += uTemperature * 0.15;
            color.b -= uTemperature * 0.15;
            
            // 5. Tint (Magenta / Green shift)
            color.g -= uTint * 0.12;
            color.r += uTint * 0.06;
            color.b += uTint * 0.06;
            
            // 6. Gamma correction
            if (uGamma > 0.0) {
                color.rgb = pow(max(color.rgb, vec3(0.0)), vec3(1.0 / uGamma));
            }
            
            // 7. Vignette
            if (uVignette > 0.0) {
                float dist = distance(vTextureCoord, vec2(0.5, 0.5));
                float vig = smoothstep(0.8, 0.8 - uVignette * 0.5, dist);
                color.rgb *= vig;
            }
            
            // 8. Film Grain
            if (uFilmGrain > 0.0) {
                float noise = (rand(vTextureCoord + vec2(uTime, uTime)) - 0.5) * uFilmGrain * 0.3;
                color.rgb += noise;
            }
            
            gl_FragColor = clamp(color, 0.0, 1.0);
        }
    """

    fun createColorFilter(params: StyleParameters): ColorMatrixColorFilter {
        val masterMatrix = ColorMatrix()

        // 1. Saturation / Monochrome
        val satMatrix = ColorMatrix()
        if (params.isMonochrome || params.saturation == 0.0f) {
            satMatrix.setSaturation(0.0f)
        } else {
            satMatrix.setSaturation(params.saturation)
        }
        masterMatrix.postConcat(satMatrix)

        // 2. Contrast & Brightness
        val contrast = params.contrast
        val brightness = params.brightness * 255f
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness

        val contrastBrightnessMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        masterMatrix.postConcat(contrastBrightnessMatrix)

        // 3. Color Temperature & Tint
        val tempR = params.temperature * 35f
        val tempB = -params.temperature * 35f
        val tintG = -params.tint * 25f
        val tintR = params.tint * 15f
        val tintB = params.tint * 15f

        val colorBalanceMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, tempR + tintR,
                0f, 1f, 0f, 0f, tintG,
                0f, 0f, 1f, 0f, tempB + tintB,
                0f, 0f, 0f, 1f, 0f
            )
        )
        masterMatrix.postConcat(colorBalanceMatrix)

        return ColorMatrixColorFilter(masterMatrix)
    }

    fun processBitmap(source: Bitmap, params: StyleParameters): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = createColorFilter(params)
        }

        // Draw graded base bitmap
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Apply Vignette if enabled
        if (params.vignette > 0.05f) {
            val radius = (Math.hypot(width.toDouble(), height.toDouble()) / 2).toFloat()
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width / 2f,
                    height / 2f,
                    radius,
                    intArrayOf(Color.TRANSPARENT, Color.BLACK),
                    floatArrayOf(1.0f - params.vignette * 0.6f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        }

        // Apply subtle Film Grain if enabled
        if (params.filmGrain > 0.05f) {
            val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = (params.filmGrain * 40).toInt().coerceIn(0, 100)
            }
            val random = Random(42)
            val numDots = (width * height * 0.015f * params.filmGrain).toInt()
            for (i in 0 until numDots) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                canvas.drawPoint(x, y, grainPaint)
            }
        }

        return output
    }
}

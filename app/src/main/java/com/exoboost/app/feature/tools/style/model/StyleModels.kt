package com.exoboost.app.feature.tools.style.model

data class StyleParameters(
    val brightness: Float = 0.0f,       // -1.0 to 1.0 (0 is neutral)
    val contrast: Float = 1.0f,         // 0.5 to 2.0 (1.0 is neutral)
    val saturation: Float = 1.0f,       // 0.0 to 2.0 (1.0 is neutral, 0.0 is B&W)
    val temperature: Float = 0.0f,      // -1.0 (cool/blue) to 1.0 (warm/amber)
    val tint: Float = 0.0f,             // -1.0 (green) to 1.0 (magenta)
    val gamma: Float = 1.0f,            // 0.5 to 2.0 (1.0 is neutral)
    val sharpness: Float = 0.0f,        // 0.0 to 1.0
    val vignette: Float = 0.0f,         // 0.0 to 1.0
    val filmGrain: Float = 0.0f,        // 0.0 to 1.0
    val isMonochrome: Boolean = false
)

enum class StylePresetType(
    val id: String,
    val displayName: String,
    val description: String
) {
    ORIGINAL("original", "Original", "No adjustments applied"),
    AI("ai", "AI Enhanced", "Dynamic contrast with vibrant clarity"),
    OUTDOOR("outdoor", "Outdoor", "High brightness and enhanced saturation"),
    CINEMA("cinema", "Cinema", "Moody contrast, teal-orange tones, subtle vignette"),
    RETRO("retro", "Retro", "Warm amber tone, softened contrast, film grain"),
    BW("bw", "B&W", "Complete desaturation with punchy contrast"),
    VIVID("vivid", "Vivid", "High saturation and crisp detail"),
    WARM("warm", "Warm", "Golden-hour amber color temperature"),
    COOL("cool", "Cool", "Crisp blue-tinted cool color balance"),
    NIGHT("night", "Night Mode", "Warm low-light tone, elevated black levels"),
    CUSTOM("custom", "Custom", "Fully customizable filter parameters");

    fun getParameters(): StyleParameters {
        return when (this) {
            ORIGINAL -> StyleParameters()
            AI -> StyleParameters(
                brightness = 0.05f,
                contrast = 1.18f,
                saturation = 1.25f,
                gamma = 0.95f,
                sharpness = 0.25f
            )
            OUTDOOR -> StyleParameters(
                brightness = 0.15f,
                contrast = 1.15f,
                saturation = 1.35f,
                gamma = 0.90f
            )
            CINEMA -> StyleParameters(
                brightness = -0.02f,
                contrast = 1.22f,
                saturation = 0.88f,
                temperature = -0.12f,
                tint = 0.08f,
                gamma = 1.05f,
                vignette = 0.35f
            )
            RETRO -> StyleParameters(
                brightness = 0.02f,
                contrast = 0.92f,
                saturation = 0.85f,
                temperature = 0.35f,
                tint = 0.12f,
                vignette = 0.40f,
                filmGrain = 0.30f
            )
            BW -> StyleParameters(
                contrast = 1.30f,
                saturation = 0.0f,
                gamma = 0.95f,
                isMonochrome = true
            )
            VIVID -> StyleParameters(
                brightness = 0.04f,
                contrast = 1.15f,
                saturation = 1.45f,
                gamma = 0.96f
            )
            WARM -> StyleParameters(
                temperature = 0.45f,
                tint = 0.10f,
                saturation = 1.10f
            )
            COOL -> StyleParameters(
                temperature = -0.45f,
                tint = -0.05f,
                saturation = 1.05f
            )
            NIGHT -> StyleParameters(
                brightness = 0.10f,
                contrast = 0.90f,
                saturation = 0.80f,
                temperature = 0.50f,
                gamma = 1.20f
            )
            CUSTOM -> StyleParameters()
        }
    }
}

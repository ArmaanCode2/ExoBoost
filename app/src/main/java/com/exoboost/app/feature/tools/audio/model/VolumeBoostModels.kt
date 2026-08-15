package com.exoboost.app.feature.tools.audio.model

enum class VolumeBoostBackend(val displayName: String) {
    DYNAMICS_PROCESSING("Dynamics Processing (Limiter + Gain)"),
    LOUDNESS_ENHANCER("Loudness Enhancer (Session 0)"),
    UNSUPPORTED_RESTRICTED("Restricted by Device Audio HAL")
}

data class AudioCapabilityReport(
    val hasDynamicsProcessing: Boolean,
    val hasLoudnessEnhancer: Boolean,
    val isSession0Supported: Boolean,
    val recommendedBackend: VolumeBoostBackend,
    val statusDescription: String
)

data class VolumeBoostState(
    val isEnabled: Boolean = false,
    val requestedPercentage: Int = 100,
    val appliedGainMb: Int = 0,
    val appliedGainDb: Float = 0.0f,
    val activeBackend: VolumeBoostBackend = VolumeBoostBackend.UNSUPPORTED_RESTRICTED,
    val isSupportedOnDevice: Boolean = false,
    val statusMessage: String = ""
)

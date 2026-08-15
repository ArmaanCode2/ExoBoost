package com.exoboost.app.feature.tools.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import com.exoboost.app.feature.tools.audio.model.AudioCapabilityReport
import com.exoboost.app.feature.tools.audio.model.VolumeBoostBackend

object AudioEffectCapabilityDetector {

    fun detectCapabilities(): AudioCapabilityReport {
        val availableEffects = try {
            AudioEffect.queryEffects() ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }

        var hasLoudnessDescriptor = false
        var hasDynamicsDescriptor = false

        for (desc in availableEffects) {
            if (desc.type == AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER) {
                hasLoudnessDescriptor = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                desc.type == AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING
            ) {
                hasDynamicsDescriptor = true
            }
        }

        val testLoudness = probeLoudnessEnhancerSession0()
        val testDynamics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            probeDynamicsProcessingSession0()
        } else {
            false
        }

        val recommendedBackend: VolumeBoostBackend
        val isSupported: Boolean
        val description: String

        when {
            testDynamics -> {
                recommendedBackend = VolumeBoostBackend.DYNAMICS_PROCESSING
                isSupported = true
                description = "Supported for this audio path (Backend: Dynamics Processing with Limiter)"
            }
            testLoudness -> {
                recommendedBackend = VolumeBoostBackend.LOUDNESS_ENHANCER
                isSupported = true
                description = "Supported for this audio path (Backend: Loudness Enhancer)"
            }
            else -> {
                recommendedBackend = VolumeBoostBackend.UNSUPPORTED_RESTRICTED
                isSupported = false
                description = "System-wide boost unavailable on this device. ExoBoost cannot directly process this app's audio on this device."
            }
        }

        return AudioCapabilityReport(
            hasDynamicsProcessing = hasDynamicsDescriptor || testDynamics,
            hasLoudnessEnhancer = hasLoudnessDescriptor || testLoudness,
            isSession0Supported = isSupported,
            recommendedBackend = recommendedBackend,
            statusDescription = description
        )
    }

    private fun probeLoudnessEnhancerSession0(): Boolean {
        var enhancer: LoudnessEnhancer? = null
        return try {
            enhancer = LoudnessEnhancer(0)
            val hasControl = enhancer.hasControl()
            enhancer.release()
            hasControl
        } catch (_: Throwable) {
            try { enhancer?.release() } catch (_: Throwable) {}
            false
        }
    }

    private fun probeDynamicsProcessingSession0(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        var dp: DynamicsProcessing? = null
        return try {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
                2, // channelCount (stereo)
                false, // preEqInUse
                0, // preEqBands
                false, // mbcInUse
                0, // mbcBands
                false, // postEqInUse
                0, // postEqBands
                true // limiterInUse
            ).build()
            dp = DynamicsProcessing(0, 0, config)
            val hasControl = dp.hasControl()
            dp.release()
            hasControl
        } catch (_: Throwable) {
            try { dp?.release() } catch (_: Throwable) {}
            false
        }
    }
}

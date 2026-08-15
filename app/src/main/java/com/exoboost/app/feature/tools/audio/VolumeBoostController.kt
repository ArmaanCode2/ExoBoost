package com.exoboost.app.feature.tools.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import com.exoboost.app.feature.tools.audio.model.AudioCapabilityReport
import com.exoboost.app.feature.tools.audio.model.VolumeBoostBackend
import com.exoboost.app.feature.tools.audio.model.VolumeBoostState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10

class VolumeBoostController(private val context: Context) {

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var isReceiverRegistered = false

    private val _boostState = MutableStateFlow(VolumeBoostState())
    val boostState: StateFlow<VolumeBoostState> = _boostState.asStateFlow()

    private val audioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY,
                Intent.ACTION_HEADSET_PLUG -> {
                    // Audio route changed (e.g. headphones unplugged), refresh effect state safely
                    if (_boostState.value.isEnabled) {
                        applyGain(_boostState.value.requestedPercentage)
                    }
                }
            }
        }
    }

    init {
        registerAudioRouteReceiver()
        val report = AudioEffectCapabilityDetector.detectCapabilities()
        _boostState.value = _boostState.value.copy(
            activeBackend = report.recommendedBackend,
            isSupportedOnDevice = report.isSession0Supported,
            statusMessage = report.statusDescription
        )
    }

    fun setBoostPercentage(percent: Int): VolumeBoostState {
        val clampedPercent = percent.coerceIn(100, 300)
        return if (_boostState.value.isEnabled) {
            applyGain(clampedPercent)
        } else {
            val gainDb = calculateGainDb(clampedPercent)
            val gainMb = (gainDb * 100).toInt()
            val state = _boostState.value.copy(
                requestedPercentage = clampedPercent,
                appliedGainDb = gainDb,
                appliedGainMb = gainMb
            )
            _boostState.value = state
            state
        }
    }

    fun toggleBoost(targetPercent: Int): VolumeBoostState {
        return if (_boostState.value.isEnabled) {
            disableBoost()
        } else {
            enableBoost(targetPercent)
        }
    }

    fun enableBoost(percent: Int = _boostState.value.requestedPercentage): VolumeBoostState {
        val clampedPercent = percent.coerceIn(100, 300)
        val report = AudioEffectCapabilityDetector.detectCapabilities()

        if (!report.isSession0Supported) {
            val state = _boostState.value.copy(
                isEnabled = false,
                requestedPercentage = clampedPercent,
                activeBackend = VolumeBoostBackend.UNSUPPORTED_RESTRICTED,
                isSupportedOnDevice = false,
                statusMessage = "ExoBoost cannot directly process this app's audio on this device's audio HAL."
            )
            _boostState.value = state
            return state
        }

        return applyGain(clampedPercent, report.recommendedBackend)
    }

    fun disableBoost(): VolumeBoostState {
        releaseEffects()
        val state = _boostState.value.copy(
            isEnabled = false,
            appliedGainMb = 0,
            appliedGainDb = 0.0f,
            statusMessage = "Volume Boost: Disabled (Normal 100%)"
        )
        _boostState.value = state
        return state
    }

    fun resetToDefault(): VolumeBoostState {
        return setBoostPercentage(100)
    }

    private fun applyGain(
        percent: Int,
        preferredBackend: VolumeBoostBackend? = null
    ): VolumeBoostState {
        val gainDb = calculateGainDb(percent)
        val gainMb = (gainDb * 100).toInt()
        val backend = preferredBackend ?: _boostState.value.activeBackend

        var success = false

        if (backend == VolumeBoostBackend.DYNAMICS_PROCESSING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (dynamicsProcessing == null) {
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
                    dynamicsProcessing = DynamicsProcessing(0, 0, config)
                }

                dynamicsProcessing?.apply {
                    val limiter = DynamicsProcessing.Limiter(
                        true,
                        true,
                        0,
                        1.0f, // 1ms attack
                        50.0f, // 50ms release
                        10.0f, // 10:1 compression ratio
                        -1.0f, // -1dB threshold to protect DAC
                        gainDb // post amplification gain in dB
                    )
                    setLimiterByChannelIndex(0, limiter)
                    setLimiterByChannelIndex(1, limiter)
                    enabled = true
                }
                success = true
            } catch (_: Throwable) {
                releaseEffects()
                success = false
            }
        }

        // Fallback to LoudnessEnhancer if DynamicsProcessing failed or selected
        if (!success && (backend == VolumeBoostBackend.LOUDNESS_ENHANCER || backend == VolumeBoostBackend.DYNAMICS_PROCESSING)) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(0)
                }
                loudnessEnhancer?.apply {
                    setTargetGain(gainMb)
                    enabled = true
                }
                success = true
            } catch (_: Throwable) {
                releaseEffects()
                success = false
            }
        }

        val finalState = if (success) {
            VolumeBoostState(
                isEnabled = true,
                requestedPercentage = percent,
                appliedGainMb = gainMb,
                appliedGainDb = gainDb,
                activeBackend = if (dynamicsProcessing != null) VolumeBoostBackend.DYNAMICS_PROCESSING else VolumeBoostBackend.LOUDNESS_ENHANCER,
                isSupportedOnDevice = true,
                statusMessage = "Supported for this audio path (Backend: ${if (dynamicsProcessing != null) "Dynamics Processing" else "Loudness Enhancer"})"
            )
        } else {
            VolumeBoostState(
                isEnabled = false,
                requestedPercentage = percent,
                appliedGainMb = 0,
                appliedGainDb = 0.0f,
                activeBackend = VolumeBoostBackend.UNSUPPORTED_RESTRICTED,
                isSupportedOnDevice = false,
                statusMessage = "ExoBoost cannot directly process this app's audio on this device."
            )
        }

        _boostState.value = finalState
        return finalState
    }

    private fun calculateGainDb(percent: Int): Float {
        if (percent <= 100) return 0.0f
        return (20.0 * log10(percent.toDouble() / 100.0)).toFloat()
    }

    private fun registerAudioRouteReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(Intent.ACTION_HEADSET_PLUG)
            }
            try {
                context.registerReceiver(audioReceiver, filter)
                isReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun releaseEffects() {
        try {
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
        } catch (_: Throwable) {}
        dynamicsProcessing = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Throwable) {}
        loudnessEnhancer = null
    }

    fun release() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(audioReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
        releaseEffects()
    }
}

package com.exoboost.app.feature.tools.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SleepTimerManager {

    private var countDownTimer: CountDownTimer? = null
    private val _remainingSeconds = MutableStateFlow<Long>(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow<Boolean>(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    fun startTimer(context: Context, minutes: Int) {
        cancelTimer()
        val totalMillis = minutes * 60 * 1000L

        _remainingSeconds.value = (minutes * 60).toLong()
        _isTimerRunning.value = true

        countDownTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingSeconds.value = millisUntilFinished / 1000L
            }

            override fun onFinish() {
                _remainingSeconds.value = 0L
                _isTimerRunning.value = false
                pausePlayback(context.applicationContext)
            }
        }.start()

        Toast.makeText(context, "Sleep Timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _remainingSeconds.value = 0L
        _isTimerRunning.value = false
    }

    private fun pausePlayback(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val handler = Handler(Looper.getMainLooper())

        // 1. Dispatch media key pause
        try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        } catch (_: Exception) {}

        // 2. Request transient audio focus to guarantee video apps pause
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener {}
                    .build()

                audioManager.requestAudioFocus(focusRequest)
                handler.postDelayed({
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }, 1000)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        } catch (_: Exception) {}

        Toast.makeText(context, "Playback paused by ExoBoost Sleep Timer", Toast.LENGTH_LONG).show()
    }
}

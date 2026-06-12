package com.focus.timer

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val _remainingSeconds = MutableStateFlow(1500)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private val _totalSeconds = MutableStateFlow(1500)
    val totalSeconds: StateFlow<Int> = _totalSeconds

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _intention = MutableStateFlow("")
    val intention: StateFlow<String> = _intention

    private var timerJob: Job? = null
    private val defaultFocusTime = 25 * 60

    init {
        resetTimer()
    }

    private fun triggerAlarm() {
        // Play notification sound
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(getApplication<Application>().applicationContext, notification)
        r.play()

        // Vibrate
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        _isRunning.value = true

        timerJob = viewModelScope.launch {
            try {
                while (_remainingSeconds.value > 0) {
                    delay(1000)
                    _remainingSeconds.value -= 1
                }
                triggerAlarm()
            } finally {
                _isRunning.value = false
                timerJob = null
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _isRunning.value = false
    }

    fun resetTimer() {
        stopTimer()
        _remainingSeconds.value = defaultFocusTime
        _totalSeconds.value = defaultFocusTime
    }

    fun setFocusTime(minutes: Int) {
        val newSeconds = minutes * 60
        if (newSeconds > 0) {
            _totalSeconds.value = newSeconds
            _remainingSeconds.value = newSeconds
            stopTimer()
        }
    }

    fun setIntention(newIntention: String) {
        _intention.value = newIntention
    }
}

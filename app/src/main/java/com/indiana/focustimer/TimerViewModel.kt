package com.indiana.focustimer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel @JvmOverloads constructor(
    application: Application,
    private val alarmController: FocusAlarmController = DefaultFocusAlarmController(application)
) : AndroidViewModel(application) {
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
    private var endTimeRealtime: Long = 0L

    init {
        resetTimer()
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        
        // Stop any active alarms when starting
        alarmController.stopAlarm()

        _isRunning.value = true
        val remaining = _remainingSeconds.value
        endTimeRealtime = SystemClock.elapsedRealtime() + remaining * 1000L

        // Schedule exact background alarm
        alarmController.scheduleAlarm(remaining, _intention.value)

        timerJob = viewModelScope.launch {
            try {
                while (SystemClock.elapsedRealtime() < endTimeRealtime) {
                    val rem = ((endTimeRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt()
                    _remainingSeconds.value = rem.coerceAtLeast(0)
                    delay(200) // Poll frequently to ensure zero cumulative drift
                }
                _remainingSeconds.value = 0
                alarmController.playAlarm()
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
        alarmController.stopAlarm()
        alarmController.cancelAlarm()
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

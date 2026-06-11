package com.focus.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _remainingSeconds = MutableStateFlow(1500) // Default to 25 minutes (1500 seconds)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private var timerJob: Job? = null
    private val defaultFocusTime = 25 * 60 // 25 minutes in seconds

    init {
        resetTimer()
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return // Prevent multiple timers

        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000) // Wait for 1 second
                _remainingSeconds.value -= 1
            }
            stopTimer() // Timer finished
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        stopTimer()
        _remainingSeconds.value = defaultFocusTime
    }

    fun setFocusTime(minutes: Int) {
        val newSeconds = minutes * 60
        if (newSeconds > 0) {
            _remainingSeconds.value = newSeconds
            stopTimer() // Stop any active timer and reset
        }
    }
}

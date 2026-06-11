package com.focus.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Timer
import java.util.TimerTask

class TimerViewModel {
    private val _remainingSeconds = MutableStateFlow(1500) // 25 minutes
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private var timer: Timer? = null

    fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (_remainingSeconds.value > 0) {
                    _remainingSeconds.value -= 1
                } else {
                    stopTimer()
                }
            }
        }, 1000, 1000)
    }

    fun stopTimer() {
        timer?.cancel()
        timer = null
    }
}

package com.indiana.focustimer

import android.app.Application
import android.content.Context
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
    
    private val sharedPrefs = application.getSharedPreferences("focus_timer_prefs", Context.MODE_PRIVATE)

    private val _presets = MutableStateFlow<List<Int>>(listOf(300, 1500, 3600))
    val presets: StateFlow<List<Int>> = _presets

    private val _selectedPresetIndex = MutableStateFlow(1)
    val selectedPresetIndex: StateFlow<Int> = _selectedPresetIndex

    private val _remainingSeconds = MutableStateFlow(1500)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private val _totalSeconds = MutableStateFlow(1500)
    val totalSeconds: StateFlow<Int> = _totalSeconds

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _intention = MutableStateFlow("")
    val intention: StateFlow<String> = _intention

    private var timerJob: Job? = null
    private var endTimeRealtime: Long = 0L

    init {
        loadPresets()
    }

    private fun loadPresets() {
        val p0 = sharedPrefs.getInt("preset_0", 300)      // 5 min
        val p1 = sharedPrefs.getInt("preset_1", 1500)     // 25 min
        val p2 = sharedPrefs.getInt("preset_2", 3600)     // 60 min
        _presets.value = listOf(p0, p1, p2)

        val activeIndex = sharedPrefs.getInt("active_preset_index", 1)
        _selectedPresetIndex.value = activeIndex
        
        val duration = _presets.value[activeIndex]
        _totalSeconds.value = duration
        _remainingSeconds.value = duration
    }

    fun selectPreset(index: Int) {
        if (index in 0..2) {
            stopTimer()
            _selectedPresetIndex.value = index
            sharedPrefs.edit().putInt("active_preset_index", index).apply()
            val duration = _presets.value[index]
            _totalSeconds.value = duration
            _remainingSeconds.value = duration
        }
    }

    fun saveCurrentAsPreset() {
        val activeIndex = _selectedPresetIndex.value
        val currentDuration = _totalSeconds.value
        if (activeIndex in 0..2) {
            sharedPrefs.edit().putInt("preset_$activeIndex", currentDuration).apply()
            val updatedPresets = _presets.value.toMutableList()
            updatedPresets[activeIndex] = currentDuration
            _presets.value = updatedPresets
        }
    }

    fun setCustomTime(minutes: Int, seconds: Int) {
        val newSeconds = minutes * 60 + seconds
        if (newSeconds > 0) {
            stopTimer()
            _totalSeconds.value = newSeconds
            _remainingSeconds.value = newSeconds
        }
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
        val duration = _presets.value[_selectedPresetIndex.value]
        _totalSeconds.value = duration
        _remainingSeconds.value = duration
    }

    fun setFocusTime(minutes: Int) {
        setCustomTime(minutes, 0)
    }

    fun setIntention(newIntention: String) {
        _intention.value = newIntention
    }
}


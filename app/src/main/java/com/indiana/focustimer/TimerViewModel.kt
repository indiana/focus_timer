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

enum class PomodoroStage {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

enum class Screen {
    TIMER,
    SETTINGS
}

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

    // Pomodoro Mode States
    private val _isPomodoroMode = MutableStateFlow(false)
    val isPomodoroMode: StateFlow<Boolean> = _isPomodoroMode

    private val _currentStage = MutableStateFlow(PomodoroStage.FOCUS)
    val currentStage: StateFlow<PomodoroStage> = _currentStage

    private val _pomodoroSequence = MutableStateFlow<List<PomodoroStage>>(
        listOf(
            PomodoroStage.FOCUS, PomodoroStage.SHORT_BREAK,
            PomodoroStage.FOCUS, PomodoroStage.SHORT_BREAK,
            PomodoroStage.FOCUS, PomodoroStage.SHORT_BREAK,
            PomodoroStage.FOCUS, PomodoroStage.LONG_BREAK
        )
    )
    val pomodoroSequence: StateFlow<List<PomodoroStage>> = _pomodoroSequence

    private val _currentStageIndex = MutableStateFlow(0)
    val currentStageIndex: StateFlow<Int> = _currentStageIndex

    private val _pomodoroFocusDuration = MutableStateFlow(1500)
    val pomodoroFocusDuration: StateFlow<Int> = _pomodoroFocusDuration

    private val _pomodoroShortBreakDuration = MutableStateFlow(300)
    val pomodoroShortBreakDuration: StateFlow<Int> = _pomodoroShortBreakDuration

    private val _pomodoroLongBreakDuration = MutableStateFlow(900)
    val pomodoroLongBreakDuration: StateFlow<Int> = _pomodoroLongBreakDuration

    // Sound & Vibration Settings
    private val _alarmSoundUri = MutableStateFlow<String?>("")
    val alarmSoundUri: StateFlow<String?> = _alarmSoundUri

    private val _alarmSoundName = MutableStateFlow("Default Notification Sound")
    val alarmSoundName: StateFlow<String> = _alarmSoundName

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled

    // Navigation Screen State
    private val _currentScreen = MutableStateFlow(Screen.TIMER)
    val currentScreen: StateFlow<Screen> = _currentScreen

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

        // Load Settings
        _alarmSoundUri.value = sharedPrefs.getString("alarm_sound_uri", "")
        _alarmSoundName.value = sharedPrefs.getString("alarm_sound_name", "Default Notification Sound") ?: "Default Notification Sound"
        _vibrationEnabled.value = sharedPrefs.getBoolean("vibration_enabled", true)

        // Load Pomodoro states
        val isPomodoro = sharedPrefs.getBoolean("is_pomodoro_mode", false)
        _isPomodoroMode.value = isPomodoro

        // Parse custom pomodoro sequence
        val defaultSeq = "FOCUS,SHORT_BREAK,FOCUS,SHORT_BREAK,FOCUS,SHORT_BREAK,FOCUS,LONG_BREAK"
        val seqString = sharedPrefs.getString("pomodoro_sequence", defaultSeq) ?: defaultSeq
        val parsedList = seqString.split(",").mapNotNull {
            try { PomodoroStage.valueOf(it) } catch (e: Exception) { null }
        }
        _pomodoroSequence.value = if (parsedList.isNotEmpty()) parsedList else defaultSeq.split(",").map { PomodoroStage.valueOf(it) }

        val activeIdx = sharedPrefs.getInt("pomodoro_active_index", 0)
        _currentStageIndex.value = if (activeIdx in _pomodoroSequence.value.indices) activeIdx else 0
        _currentStage.value = _pomodoroSequence.value[_currentStageIndex.value]

        _pomodoroFocusDuration.value = sharedPrefs.getInt("pomodoro_focus_duration", 1500)
        _pomodoroShortBreakDuration.value = sharedPrefs.getInt("pomodoro_short_break_duration", 300)
        _pomodoroLongBreakDuration.value = sharedPrefs.getInt("pomodoro_long_break_duration", 900)
        
        val duration = if (isPomodoro) {
            getDurationForStage(_currentStage.value)
        } else {
            _presets.value[activeIndex]
        }
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
        if (_isPomodoroMode.value) return
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
            if (_isPomodoroMode.value) {
                when (_currentStage.value) {
                    PomodoroStage.FOCUS -> {
                        _pomodoroFocusDuration.value = newSeconds
                        sharedPrefs.edit().putInt("pomodoro_focus_duration", newSeconds).apply()
                    }
                    PomodoroStage.SHORT_BREAK -> {
                        _pomodoroShortBreakDuration.value = newSeconds
                        sharedPrefs.edit().putInt("pomodoro_short_break_duration", newSeconds).apply()
                    }
                    PomodoroStage.LONG_BREAK -> {
                        _pomodoroLongBreakDuration.value = newSeconds
                        sharedPrefs.edit().putInt("pomodoro_long_break_duration", newSeconds).apply()
                    }
                }
            }
            _totalSeconds.value = newSeconds
            _remainingSeconds.value = newSeconds
        }
    }

    fun setStageDuration(stage: PomodoroStage, minutes: Int) {
        val seconds = minutes * 60
        if (seconds > 0) {
            when (stage) {
                PomodoroStage.FOCUS -> {
                    _pomodoroFocusDuration.value = seconds
                    sharedPrefs.edit().putInt("pomodoro_focus_duration", seconds).apply()
                }
                PomodoroStage.SHORT_BREAK -> {
                    _pomodoroShortBreakDuration.value = seconds
                    sharedPrefs.edit().putInt("pomodoro_short_break_duration", seconds).apply()
                }
                PomodoroStage.LONG_BREAK -> {
                    _pomodoroLongBreakDuration.value = seconds
                    sharedPrefs.edit().putInt("pomodoro_long_break_duration", seconds).apply()
                }
            }
            if (!_isRunning.value) {
                val activeStage = _pomodoroSequence.value[_currentStageIndex.value]
                if (activeStage == stage) {
                    _totalSeconds.value = seconds
                    _remainingSeconds.value = seconds
                }
            }
        }
    }

    fun setPomodoroMode(enabled: Boolean) {
        if (_isPomodoroMode.value == enabled) return
        stopTimer()
        _isPomodoroMode.value = enabled
        sharedPrefs.edit().putBoolean("is_pomodoro_mode", enabled).apply()
        
        val duration = if (enabled) {
            getDurationForStage(_currentStage.value)
        } else {
            _presets.value[_selectedPresetIndex.value]
        }
        _totalSeconds.value = duration
        _remainingSeconds.value = duration
    }

    fun getDurationForStage(stage: PomodoroStage): Int {
        return when (stage) {
            PomodoroStage.FOCUS -> _pomodoroFocusDuration.value
            PomodoroStage.SHORT_BREAK -> _pomodoroShortBreakDuration.value
            PomodoroStage.LONG_BREAK -> _pomodoroLongBreakDuration.value
        }
    }

    fun transitionPomodoroStage() {
        val seq = _pomodoroSequence.value
        val nextIdx = (_currentStageIndex.value + 1) % seq.size
        _currentStageIndex.value = nextIdx
        sharedPrefs.edit().putInt("pomodoro_active_index", nextIdx).apply()
        
        val nextStage = seq[nextIdx]
        _currentStage.value = nextStage
        
        val duration = getDurationForStage(nextStage)
        _totalSeconds.value = duration
        _remainingSeconds.value = duration
    }

    fun skipPomodoroStage() {
        stopTimer()
        transitionPomodoroStage()
    }

    fun resetPomodoroCycle() {
        stopTimer()
        _currentStageIndex.value = 0
        sharedPrefs.edit().putInt("pomodoro_active_index", 0).apply()
        
        val nextStage = _pomodoroSequence.value[0]
        _currentStage.value = nextStage
        
        val duration = getDurationForStage(nextStage)
        _totalSeconds.value = duration
        _remainingSeconds.value = duration
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        
        alarmController.stopAlarm()

        _isRunning.value = true
        val remaining = _remainingSeconds.value
        endTimeRealtime = SystemClock.elapsedRealtime() + remaining * 1000L

        val alarmLabel = if (_isPomodoroMode.value) {
            when (_currentStage.value) {
                PomodoroStage.FOCUS -> "Focus: ${_intention.value.ifBlank { "Session" }}"
                PomodoroStage.SHORT_BREAK -> "Short Break"
                PomodoroStage.LONG_BREAK -> "Long Break"
            }
        } else {
            _intention.value
        }
        alarmController.scheduleAlarm(remaining, alarmLabel)

        timerJob = viewModelScope.launch {
            try {
                while (SystemClock.elapsedRealtime() < endTimeRealtime) {
                    val rem = ((endTimeRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt()
                    _remainingSeconds.value = rem.coerceAtLeast(0)
                    delay(200)
                }
                _remainingSeconds.value = 0
                alarmController.playAlarm()
                if (_isPomodoroMode.value) {
                    transitionPomodoroStage()
                }
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
        val duration = if (_isPomodoroMode.value) {
            getDurationForStage(_currentStage.value)
        } else {
            _presets.value[_selectedPresetIndex.value]
        }
        _totalSeconds.value = duration
        _remainingSeconds.value = duration
    }

    fun setFocusTime(minutes: Int) {
        setCustomTime(minutes, 0)
    }

    fun setIntention(newIntention: String) {
        _intention.value = newIntention
    }

    // Settings Configuration Methods
    fun setAlarmSound(uri: String?, name: String) {
        sharedPrefs.edit()
            .putString("alarm_sound_uri", uri ?: "")
            .putString("alarm_sound_name", name)
            .apply()
        _alarmSoundUri.value = uri
        _alarmSoundName.value = name
    }

    fun setVibrationEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _vibrationEnabled.value = enabled
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    private fun saveSequence(seq: List<PomodoroStage>) {
        _pomodoroSequence.value = seq
        val seqString = seq.joinToString(",") { it.name }
        sharedPrefs.edit().putString("pomodoro_sequence", seqString).apply()
        
        // Coerce index
        if (_currentStageIndex.value >= seq.size) {
            _currentStageIndex.value = 0
            sharedPrefs.edit().putInt("pomodoro_active_index", 0).apply()
        }
        
        if (!_isRunning.value) {
            val activeStage = seq[_currentStageIndex.value]
            _currentStage.value = activeStage
            val duration = getDurationForStage(activeStage)
            _totalSeconds.value = duration
            _remainingSeconds.value = duration
        }
    }

    fun addStageToSequence(stage: PomodoroStage) {
        val seq = _pomodoroSequence.value.toMutableList()
        seq.add(stage)
        saveSequence(seq)
    }

    fun removeStageFromSequence(index: Int) {
        val seq = _pomodoroSequence.value.toMutableList()
        if (seq.size > 1 && index in seq.indices) {
            seq.removeAt(index)
            saveSequence(seq)
        }
    }

    fun moveStageInSequence(index: Int, up: Boolean) {
        val seq = _pomodoroSequence.value.toMutableList()
        val targetIndex = if (up) index - 1 else index + 1
        if (index in seq.indices && targetIndex in seq.indices) {
            val temp = seq[index]
            seq[index] = seq[targetIndex]
            seq[targetIndex] = temp
            saveSequence(seq)
        }
    }

    fun resetSequenceToDefault() {
        val defaultSeq = listOf(
            PomodoroStage.FOCUS, PomodoroStage.SHORT_BREAK,
            PomodoroStage.FOCUS, PomodoroStage.SHORT_BREAK,
            PomodoroStage.FOCUS, PomodoroStage.SHORT_BREAK,
            PomodoroStage.FOCUS, PomodoroStage.LONG_BREAK
        )
        saveSequence(defaultSeq)
    }
}


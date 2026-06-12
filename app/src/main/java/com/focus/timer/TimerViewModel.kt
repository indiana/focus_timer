package com.focus.timer

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
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

private const val ALARM_REQUEST_CODE = 1001

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

    private var activeRingtone: Ringtone? = null
    private var endTimeRealtime: Long = 0L

    init {
        resetTimer()
    }

    private fun triggerAlarm() {
        // Stop any active ringtone first
        activeRingtone?.stop()

        // Play notification sound
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(getApplication<Application>().applicationContext, notification)
        activeRingtone = r
        r?.play()

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

    private fun scheduleBackgroundAlarm(secondsInFuture: Int) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("task_name", _intention.value.ifBlank { "Focus session" })
        }
        
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or 
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        val triggerAtMillis = SystemClock.elapsedRealtime() + secondsInFuture * 1000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelBackgroundAlarm() {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java)
        
        val pendingIntentFlags = PendingIntent.FLAG_NO_CREATE or 
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        
        // Stop any playing alarm when starting
        activeRingtone?.stop()
        activeRingtone = null

        _isRunning.value = true
        val remaining = _remainingSeconds.value
        endTimeRealtime = SystemClock.elapsedRealtime() + remaining * 1000L

        // Schedule exact background alarm
        scheduleBackgroundAlarm(remaining)

        timerJob = viewModelScope.launch {
            try {
                while (SystemClock.elapsedRealtime() < endTimeRealtime) {
                    val rem = ((endTimeRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt()
                    _remainingSeconds.value = rem.coerceAtLeast(0)
                    delay(200) // Poll frequently to ensure zero cumulative drift
                }
                _remainingSeconds.value = 0
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
        activeRingtone?.stop()
        activeRingtone = null
        cancelBackgroundAlarm()
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

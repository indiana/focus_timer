package com.indiana.focustimer

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

interface FocusAlarmController {
    fun playAlarm()
    fun stopAlarm()
    fun scheduleAlarm(secondsInFuture: Int, intention: String)
    fun cancelAlarm()
}

class DefaultFocusAlarmController(private val application: Application) : FocusAlarmController {
    private var activeRingtone: Ringtone? = null
    private val ALARM_REQUEST_CODE = 1001

    override fun playAlarm() {
        stopAlarm()

        val context = application.applicationContext
        val sharedPrefs = context.getSharedPreferences("focus_timer_prefs", Context.MODE_PRIVATE)
        val soundUriStr = sharedPrefs.getString("alarm_sound_uri", "")

        val soundUri = if (!soundUriStr.isNullOrBlank()) {
            Uri.parse(soundUriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val r = try {
            RingtoneManager.getRingtone(context, soundUri)
        } catch (e: Exception) {
            val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, fallback)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val aa = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            r?.audioAttributes = aa
        }
        activeRingtone = r
        r?.play()

        val vibrationEnabled = sharedPrefs.getBoolean("vibration_enabled", true)
        if (vibrationEnabled) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
            }
        }
    }

    override fun stopAlarm() {
        activeRingtone?.stop()
        activeRingtone = null
    }

    override fun scheduleAlarm(secondsInFuture: Int, intention: String) {
        val context = application.applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("task_name", intention.ifBlank { "Focus session" })
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

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            if (canScheduleExact) {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    override fun cancelAlarm() {
        val context = application.applicationContext
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
}

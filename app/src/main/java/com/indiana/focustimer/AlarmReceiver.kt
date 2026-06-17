package com.indiana.focustimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("task_name") ?: "Focus session"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val sharedPrefs = context.getSharedPreferences("focus_timer_prefs", Context.MODE_PRIVATE)
        val soundUriStr = sharedPrefs.getString("alarm_sound_uri", "")
        val soundUri = if (!soundUriStr.isNullOrBlank()) {
            Uri.parse(soundUriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val vibrationEnabled = sharedPrefs.getBoolean("vibration_enabled", true)
        
        // Dynamically change channel ID when sound or vibration preferences change
        // to force Android to create a new channel (channel sound/vibration is immutable once created)
        val channelId = "focus_timer_alarm_channel_" + (soundUriStr.hashCode() + vibrationEnabled.hashCode())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Focus Timer Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when your focus session is completed"
                enableVibration(vibrationEnabled)
                if (vibrationEnabled) {
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                }
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or 
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            pendingIntentFlags
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Focus Session Complete!")
            .setContentText("Finished: $taskName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            
        if (vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 1000, 500, 1000))
        }

        notificationManager.notify(1, notificationBuilder.build())
    }
}

package com.example.gestionturnosapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gestionturnosapp.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ReminderReceiver", "Broadcast recibido")
        val title = intent.getStringExtra("TITLE") ?: context.getString(R.string.welcome)
        val message = intent.getStringExtra("MESSAGE") ?: context.getString(R.string.no_upcoming_appointments)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 1001)
        val type = intent.getStringExtra("TYPE") ?: "TURNO"
        
        val channelId = if (type == "MEDICAMENTO") {
            NotificationHelper.CHANNEL_MEDICATION
        } else {
            NotificationHelper.CHANNEL_REMINDERS
        }

        val data = mutableMapOf<String, String>()
        intent.extras?.let { extras ->
            for (key in extras.keySet()) {
                @Suppress("DEPRECATION")
                val value = extras.get(key)
                value?.toString()?.let { data[key] = it }
            }
        }

        NotificationHelper.showNotification(
            context = context,
            title = title,
            body = message,
            channelId = channelId,
            notificationId = notificationId,
            data = data
        )
    }
}

package com.example.gestionturnosapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.gestionturnosapp.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TITLE") ?: "Recordatorio"
        val message = intent.getStringExtra("MESSAGE") ?: "Cita médica próxima"
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 1001)
        val type = intent.getStringExtra("TYPE") ?: "TURNO"
        
        showNotification(context, title, message, notificationId, type)
    }

    private fun showNotification(context: Context, title: String, message: String, id: Int, type: String) {
        val channelId = if (type == "MEDICAMENTO") "medication_channel" else "turno_reminder_channel"
        val channelName = if (type == "MEDICAMENTO") "Medicación" else "Recordatorios de Citas"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (type == "MEDICAMENTO") NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                if (type == "MEDICAMENTO") {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val icon = if (type == "MEDICAMENTO") android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(if (type == "MEDICAMENTO") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}

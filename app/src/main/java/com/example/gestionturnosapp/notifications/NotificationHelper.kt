package com.example.gestionturnosapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gestionturnosapp.MainActivity
import com.example.gestionturnosapp.R

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    const val CHANNEL_GENERAL = "general_channel"
    const val CHANNEL_REMINDERS = "reminders_channel"
    const val CHANNEL_MEDICATION = "medication_channel"
    const val CHANNEL_CHAT = "chat_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_GENERAL,
                    context.getString(R.string.label_general_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones generales de la aplicación"
                },
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.label_reminders_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                    description = "Recordatorios de citas médicas"
                },
                NotificationChannel(
                    CHANNEL_MEDICATION,
                    context.getString(R.string.label_medication_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    description = "Avisos para toma de medicamentos"
                },
                NotificationChannel(
                    CHANNEL_CHAT,
                    context.getString(R.string.label_chat_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                    description = "Mensajes del asistente médico"
                }
            )
            notificationManager.createNotificationChannels(channels)
            Log.d(TAG, "Canales de notificación creados")
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_GENERAL,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt(),
        data: Map<String, String>? = null
    ) {
        Log.d(TAG, "Intentando mostrar notificación: $title - Channel: $channelId")

        if (!com.example.gestionturnosapp.data.local.PreferenceManager.areNotificationsEnabled(context)) {
            Log.w(TAG, "Notificaciones desactivadas en ajustes de la app")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data?.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Usar un icono del sistema resiliente como fallback
        val icon = when (channelId) {
            CHANNEL_MEDICATION -> android.R.drawable.ic_dialog_alert
            CHANNEL_CHAT -> android.R.drawable.stat_notify_chat
            else -> android.R.drawable.ic_dialog_info
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (channelId == CHANNEL_GENERAL) NotificationCompat.PRIORITY_DEFAULT 
                else NotificationCompat.PRIORITY_HIGH
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Verificar permiso en runtime (Opcional aquí, pero ayuda al diagnóstico)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No hay permiso POST_NOTIFICATIONS concedido")
                // No podemos hacer mucho aquí más que loguear
            }
        }

        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Notificación enviada al sistema")
    }
}

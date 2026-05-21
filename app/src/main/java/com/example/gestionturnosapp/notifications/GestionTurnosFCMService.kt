package com.example.gestionturnosapp.notifications

import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GestionTurnosFCMService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        UserManager.saveFcmToken(applicationContext, token)
        syncTokenToServer(token)
    }

    private fun syncTokenToServer(fcmToken: String) {
        scope.launch {
            try {
                val response = RetrofitClient.instance.updateFcmToken(mapOf("token" to fcmToken))
                if (response.isSuccessful) {
                    UserManager.markFcmAsSynced(applicationContext)
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error syncing token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Salud Activa"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Tienes una nueva actualización"
        val type = remoteMessage.data["type"] ?: "general"
        
        val channelId = when (type) {
            "chat" -> NotificationHelper.CHANNEL_CHAT
            "turno" -> NotificationHelper.CHANNEL_REMINDERS
            "medicamento" -> NotificationHelper.CHANNEL_MEDICATION
            else -> NotificationHelper.CHANNEL_GENERAL
        }

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = channelId,
            data = remoteMessage.data
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

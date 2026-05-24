package com.example.gestionturnosapp.notifications

import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.data.remote.ApiService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GestionTurnosFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var apiService: ApiService

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        userManager.saveFcmToken(token)
        syncTokenToServer(token)
    }

    private fun syncTokenToServer(fcmToken: String) {
        scope.launch {
            try {
                val response = apiService.updateFcmToken(mapOf("token" to fcmToken))
                if (response.isSuccessful) {
                    userManager.markFcmAsSynced()
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error syncing token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        android.util.Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")
        
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
            data = remoteMessage.data,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

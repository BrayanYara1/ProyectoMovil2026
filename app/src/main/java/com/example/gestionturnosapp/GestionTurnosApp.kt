package com.example.gestionturnosapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GestionTurnosApp : Application() {

    companion object {
        lateinit var instance: GestionTurnosApp
            private set
    }

    override fun onCreate() {
        instance = this
        super.onCreate()
        
        // Crear canales de notificación
        com.example.gestionturnosapp.notifications.NotificationHelper.createNotificationChannels(this)

        // Nota: loadLibs se movió a AppDatabase para carga perezosa y segura
    }
}

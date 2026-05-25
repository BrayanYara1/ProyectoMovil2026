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
        
        // Aplicar tema guardado inmediatamente para evitar parpadeos y problemas de navegación
        try {
            val isDark = com.example.gestionturnosapp.data.local.PreferenceManager.isDarkMode(this)
            com.example.gestionturnosapp.data.local.PreferenceManager.applyTheme(isDark)
        } catch (e: Exception) {
            android.util.Log.e("GestionTurnosApp", "Error applying theme", e)
        }

        // Inicializar SQLCipher libs lo antes posible
        // Nota: Usamos Throwable para capturar UnsatisfiedLinkError en dispositivos con arquitecturas incompatibles
        try {
            System.loadLibrary("sqlcipher")
        } catch (t: Throwable) {
            android.util.Log.e("GestionTurnosApp", "Error loading SQLCipher native library. " +
                    "The app might still work if the Room factory handles it.", t)
        }

        // Crear canales de notificación
        try {
            com.example.gestionturnosapp.notifications.NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            android.util.Log.e("GestionTurnosApp", "Error creating notification channels", e)
        }
    }
}

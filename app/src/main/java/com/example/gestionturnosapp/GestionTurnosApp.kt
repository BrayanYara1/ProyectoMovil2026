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
        val isDark = com.example.gestionturnosapp.data.local.PreferenceManager.isDarkMode(this)
        com.example.gestionturnosapp.data.local.PreferenceManager.applyTheme(isDark)

        // Inicializar SQLCipher libs lo antes posible
        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
        } catch (e: Exception) {
            android.util.Log.e("GestionTurnosApp", "Error loading SQLCipher libs", e)
        }

        // Crear canales de notificación
        com.example.gestionturnosapp.notifications.NotificationHelper.createNotificationChannels(this)
    }
}

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
        super.onCreate()
        instance = this
        
        // Crear canales de notificación
        com.example.gestionturnosapp.notifications.NotificationHelper.createNotificationChannels(this)

        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
        } catch (t: Throwable) {
            android.util.Log.e("App", "LoadLibs Error", t)
        }
    }
}

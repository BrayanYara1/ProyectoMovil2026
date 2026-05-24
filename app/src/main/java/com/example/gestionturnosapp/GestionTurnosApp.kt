package com.example.gestionturnosapp

import android.app.Application
import com.example.gestionturnosapp.data.UserManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GestionTurnosApp : Application() {

    @Inject
    lateinit var userManager: UserManager

    override fun onCreate() {
        super.onCreate()
        
        // Inicializar instancia estática para compatibilidad (Legacy)
        UserManager.init(userManager)

        // Crear canales de notificación
        com.example.gestionturnosapp.notifications.NotificationHelper.createNotificationChannels(this)

        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
        } catch (t: Throwable) {
            android.util.Log.e("App", "LoadLibs Error", t)
        }
    }
}

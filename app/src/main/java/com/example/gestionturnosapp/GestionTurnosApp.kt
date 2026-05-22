package com.example.gestionturnosapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GestionTurnosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar SQLCipher para que la DB cifrada funcione correctamente
        net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
    }
}

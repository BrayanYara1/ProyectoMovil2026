package com.example.gestionturnosapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GestionTurnosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
        } catch (t: Throwable) {
            android.util.Log.e("App", "LoadLibs Error", t)
        }
    }
}

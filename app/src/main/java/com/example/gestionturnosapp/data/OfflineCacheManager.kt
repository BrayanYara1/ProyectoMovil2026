package com.example.gestionturnosapp.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OfflineCacheManager {
    private const val PREF_NAME = "offline_cache"
    private const val KEY_TURNOS = "cached_turnos"
    private val gson = Gson()

    fun saveTurnos(context: Context, turnos: List<Turno>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(turnos)
        prefs.edit().putString(KEY_TURNOS, json).apply()
    }

    fun getCachedTurnos(context: Context): List<Turno> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TURNOS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Turno>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

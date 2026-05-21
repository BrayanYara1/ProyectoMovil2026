package com.example.gestionturnosapp.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OfflineCacheManager {
    private const val PREF_NAME = "offline_cache"
    private const val KEY_TURNOS = "cached_turnos"
    private const val KEY_MEDICAMENTOS = "cached_medicamentos"
    private const val KEY_ESTUDIOS = "cached_estudios"
    
    // Claves para datos pendientes de sincronizar
    private const val KEY_PENDING_TURNOS = "pending_turnos"
    private const val KEY_PENDING_MEDS = "pending_meds"
    private const val KEY_PENDING_ESTUDIOS = "pending_estudios"
    
    private val gson = Gson()

    // --- TURNOS ---
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

    fun addPendingTurno(context: Context, request: NuevoTurnoRequest) {
        val pending = getPendingTurnos(context).toMutableList()
        pending.add(request)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_TURNOS, gson.toJson(pending)).apply()
    }

    fun getPendingTurnos(context: Context): List<NuevoTurnoRequest> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PENDING_TURNOS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NuevoTurnoRequest>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearPendingTurnos(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().remove(KEY_PENDING_TURNOS).apply()
    }

    fun removePendingTurnos(context: Context, synced: List<NuevoTurnoRequest>) {
        val current = getPendingTurnos(context).toMutableList()
        current.removeAll(synced)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (current.isEmpty()) {
            prefs.edit().remove(KEY_PENDING_TURNOS).apply()
        } else {
            prefs.edit().putString(KEY_PENDING_TURNOS, gson.toJson(current)).apply()
        }
    }

    // --- MEDICAMENTOS ---
    fun saveMedicamentos(context: Context, meds: List<Medicamento>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(meds)
        prefs.edit().putString(KEY_MEDICAMENTOS, json).apply()
    }

    fun getCachedMedicamentos(context: Context): List<Medicamento> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEDICAMENTOS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Medicamento>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addPendingMed(context: Context, med: Medicamento) {
        val pending = getPendingMeds(context).toMutableList()
        pending.add(med)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_MEDS, gson.toJson(pending)).apply()
    }

    fun getPendingMeds(context: Context): List<Medicamento> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PENDING_MEDS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Medicamento>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearPendingMeds(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().remove(KEY_PENDING_MEDS).apply()
    }

    fun removePendingMeds(context: Context, synced: List<Medicamento>) {
        val current = getPendingMeds(context).toMutableList()
        current.removeAll(synced)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (current.isEmpty()) {
            prefs.edit().remove(KEY_PENDING_MEDS).apply()
        } else {
            prefs.edit().putString(KEY_PENDING_MEDS, gson.toJson(current)).apply()
        }
    }

    // --- ESTUDIOS ---
    fun saveEstudios(context: Context, estudios: List<EstudioMedico>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(estudios)
        prefs.edit().putString(KEY_ESTUDIOS, json).apply()
    }

    fun getCachedEstudios(context: Context): List<EstudioMedico> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ESTUDIOS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EstudioMedico>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addPendingEstudio(context: Context, estudio: EstudioMedico) {
        val pending = getPendingEstudios(context).toMutableList()
        pending.add(estudio)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_ESTUDIOS, gson.toJson(pending)).apply()
    }

    fun getPendingEstudios(context: Context): List<EstudioMedico> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PENDING_ESTUDIOS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EstudioMedico>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearPendingEstudios(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().remove(KEY_PENDING_ESTUDIOS).apply()
    }

    fun clearCache(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_TURNOS)
            .remove(KEY_MEDICAMENTOS)
            .remove(KEY_ESTUDIOS)
            .apply()
    }

    fun removePendingEstudios(context: Context, synced: List<EstudioMedico>) {
        val current = getPendingEstudios(context).toMutableList()
        current.removeAll(synced)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (current.isEmpty()) {
            prefs.edit().remove(KEY_PENDING_ESTUDIOS).apply()
        } else {
            prefs.edit().putString(KEY_PENDING_ESTUDIOS, gson.toJson(current)).apply()
        }
    }

    // --- UTIL ---
    fun isNetworkError(e: Exception): Boolean {
        val msg = e.localizedMessage ?: ""
        return msg.contains("Unable to resolve host", true) || 
               msg.contains("timeout", true) || 
               msg.contains("Failed to connect", true)
    }
}

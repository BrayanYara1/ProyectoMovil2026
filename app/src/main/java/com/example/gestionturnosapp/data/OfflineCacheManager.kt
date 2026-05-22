package com.example.gestionturnosapp.data

import android.content.Context
import com.example.gestionturnosapp.data.local.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OfflineCacheManager {
    private const val PREF_NAME = "offline_cache"
    
    // Claves para datos pendientes de sincronizar (Seguimos usando Prefs por simplicidad en colas cortas)
    private const val KEY_PENDING_TURNOS = "pending_turnos"
    private const val KEY_PENDING_MEDS = "pending_meds"
    private const val KEY_PENDING_ESTUDIOS = "pending_estudios"
    
    private val gson = Gson()

    private fun getDb(context: Context) = AppDatabase.getDatabase(context)

    // --- TURNOS (ROOM) ---
    suspend fun saveTurnos(context: Context, turnos: List<Turno>) = withContext(Dispatchers.IO) {
        val dao = getDb(context).turnoDao()
        dao.deleteAllTurnos()
        dao.insertTurnos(turnos)
    }

    suspend fun getCachedTurnos(context: Context): List<Turno> = withContext(Dispatchers.IO) {
        getDb(context).turnoDao().getAllTurnos()
    }

    // --- MEDICAMENTOS (ROOM) ---
    suspend fun saveMedicamentos(context: Context, meds: List<Medicamento>) = withContext(Dispatchers.IO) {
        val dao = getDb(context).medicamentoDao()
        dao.deleteAllMedicamentos()
        dao.insertMedicamentos(meds)
    }

    suspend fun getCachedMedicamentos(context: Context): List<Medicamento> = withContext(Dispatchers.IO) {
        getDb(context).medicamentoDao().getAllMedicamentos()
    }

    // --- ESTUDIOS (ROOM) ---
    suspend fun saveEstudios(context: Context, estudios: List<EstudioMedico>) = withContext(Dispatchers.IO) {
        val dao = getDb(context).estudioDao()
        dao.deleteAllEstudios()
        dao.insertEstudios(estudios)
    }

    suspend fun getCachedEstudios(context: Context): List<EstudioMedico> = withContext(Dispatchers.IO) {
        getDb(context).estudioDao().getAllEstudios()
    }

    // --- PENDIENTES (SHARED PREFERENCES - Colas de sincronización) ---
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

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val db = getDb(context)
        db.turnoDao().deleteAllTurnos()
        db.medicamentoDao().deleteAllMedicamentos()
        db.estudioDao().deleteAllEstudios()
    }

    fun isNetworkError(e: Exception): Boolean {
        val msg = e.localizedMessage ?: ""
        return msg.contains("Unable to resolve host", true) || 
               msg.contains("timeout", true) || 
               msg.contains("Failed to connect", true)
    }
}

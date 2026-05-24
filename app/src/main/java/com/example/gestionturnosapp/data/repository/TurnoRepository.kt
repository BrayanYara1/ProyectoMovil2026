package com.example.gestionturnosapp.data.repository

import android.content.Context
import com.example.gestionturnosapp.data.local.OfflineCacheManager
import com.example.gestionturnosapp.data.model.Turno
import com.example.gestionturnosapp.data.remote.ApiService
import com.example.gestionturnosapp.data.remote.RetrofitClient
import com.example.gestionturnosapp.data.remote.dto.NuevoTurnoRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TurnoRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {

    /**
     * Obtiene los turnos intentando primero el servidor y guardando en caché.
     * Si falla la red, devuelve los datos de la caché local.
     */
    suspend fun getTurnos(): List<Turno> {
        return try {
            val response = apiService.getTurnos()
            if (response.isSuccessful) {
                val turnos = response.body() ?: emptyList()
                OfflineCacheManager.saveTurnos(context, turnos)
                turnos
            } else {
                // Si el servidor responde error (ej 500), usamos cache
                OfflineCacheManager.getCachedTurnos(context)
            }
        } catch (e: Exception) {
            // Error de conexión: Usar cache
            OfflineCacheManager.getCachedTurnos(context)
        }
    }

    suspend fun getCachedTurnos(): List<Turno> {
        return OfflineCacheManager.getCachedTurnos(context)
    }

    suspend fun checkAvailability(fecha: String, hora: String): Boolean {
        return try {
            val response = apiService.checkAvailability(fecha, hora)
            if (response.isSuccessful) {
                val body = response.body()
                body?.get("disponible") ?: body?.get("available") ?: true
            } else {
                true 
            }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun crearTurno(request: NuevoTurnoRequest): Turno? {
        return try {
            val response = apiService.crearTurno(request)
            if (response.isSuccessful) {
                response.body()
            } else {
                throw Exception(RetrofitClient.parseError(response))
            }
        } catch (e: Exception) {
            if (OfflineCacheManager.isNetworkError(e)) {
                OfflineCacheManager.addPendingTurno(context, request)
                null
            } else {
                throw e
            }
        }
    }

    suspend fun eliminarTurno(id: String) {
        val response = apiService.eliminarTurno(id)
        if (!response.isSuccessful) {
            throw Exception(RetrofitClient.parseError(response))
        }
    }
}

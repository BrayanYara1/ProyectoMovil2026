package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.ApiService
import com.example.gestionturnosapp.network.RetrofitClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TurnoRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getTurnos(): List<Turno> {
        try {
            val response = apiService.getTurnos()
            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            } else {
                throw Exception(RetrofitClient.parseError(response))
            }
        } catch (e: Exception) {
            android.util.Log.e("TurnoRepository", "Error", e)
            throw e
        }
    }

    suspend fun checkAvailability(fecha: String, hora: String): Boolean {
        return try {
            val response = apiService.checkAvailability(fecha, hora)
            if (response.isSuccessful) {
                val body = response.body()
                body?.get("disponible") ?: body?.get("available") ?: true
            } else {
                // Si falla la red, permitimos intentar agendar y que el servidor valide el POST
                true 
            }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun crearTurno(request: NuevoTurnoRequest): Turno? {
        val response = apiService.crearTurno(request)
        if (response.isSuccessful) {
            return response.body()
        } else {
            throw Exception("Error al crear turno: ${RetrofitClient.parseError(response)}")
        }
    }

    suspend fun eliminarTurno(id: String) {
        val response = apiService.eliminarTurno(id)
        if (!response.isSuccessful) {
            throw Exception("Error al eliminar turno: ${RetrofitClient.parseError(response)}")
        }
    }
}

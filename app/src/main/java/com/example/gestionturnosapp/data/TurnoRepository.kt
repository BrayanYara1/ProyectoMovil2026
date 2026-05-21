package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class TurnoRepository {
    private val apiService = RetrofitClient.instance

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
                response.body()?.get("disponible") ?: true
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

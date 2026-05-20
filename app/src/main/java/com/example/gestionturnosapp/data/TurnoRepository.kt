package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class TurnoRepository {
    private val apiService = RetrofitClient.instance

    private fun parseError(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                // Si parece JSON, intentar parsearlo
                if (errorBody.trim().startsWith("{")) {
                    val gson = com.google.gson.Gson()
                    val map = gson.fromJson(errorBody, Map::class.java)
                    (map["mensaje"] as? String) ?: (map["message"] as? String) ?: (map["error"] as? String) ?: "Error ${response.code()}"
                } else {
                    // Si no es JSON (ej: HTML 404), devolver el mensaje de estatus
                    response.message().ifEmpty { "Error ${response.code()}" }
                }
            } else {
                "Error ${response.code()}: ${response.message()}"
            }
        } catch (e: Exception) {
            "Error ${response.code()}"
        }
    }

    suspend fun getTurnos(): List<Turno> {
        try {
            val response = apiService.getTurnos()
            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            } else {
                throw Exception(parseError(response))
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
            throw Exception("Error al crear turno: ${parseError(response)}")
        }
    }

    suspend fun eliminarTurno(id: String) {
        val response = apiService.eliminarTurno(id)
        if (!response.isSuccessful) {
            throw Exception("Error al eliminar turno: ${parseError(response)}")
        }
    }
}

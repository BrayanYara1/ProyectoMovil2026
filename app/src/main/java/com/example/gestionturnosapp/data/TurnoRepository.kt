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
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("TurnoRepository", "Error body: $errorBody")
                
                val errorMsg = if (!errorBody.isNullOrEmpty()) {
                    if (errorBody.contains("\"message\"")) {
                        errorBody.substringAfter("\"message\":\"").substringBefore("\"")
                    } else if (errorBody.contains("\"error\"")) {
                        errorBody.substringAfter("\"error\":\"").substringBefore("\"")
                    } else {
                        "Error del servidor (${response.code()})"
                    }
                } else {
                    "Error ${response.code()}: ${response.message()}"
                }
                throw Exception(errorMsg)
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            android.util.Log.e("TurnoRepository", "JSON Error", e)
            throw Exception("Respuesta del servidor inválida")
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("El servidor tarda mucho en responder. Reintenta en unos segundos.")
        } catch (e: Exception) {
            android.util.Log.e("TurnoRepository", "Unexpected Error", e)
            throw e
        }
    }

    suspend fun crearTurno(request: NuevoTurnoRequest): Turno? {
        val response = apiService.crearTurno(request)
        if (response.isSuccessful) {
            return response.body()
        } else {
            val errorMsg = try {
                val errorBody = response.errorBody()?.string()
                if (!errorBody.isNullOrEmpty()) {
                    // Intentar extraer el mensaje si es un JSON con campo "message" o "error"
                    if (errorBody.contains("\"message\"")) {
                        errorBody.substringAfter("\"message\":\"").substringBefore("\"")
                    } else if (errorBody.contains("\"error\"")) {
                        errorBody.substringAfter("\"error\":\"").substringBefore("\"")
                    } else {
                        errorBody
                    }
                } else {
                    response.message()
                }
            } catch (e: Exception) {
                response.message()
            }
            throw Exception("Error al crear turno: $errorMsg")
        }
    }

    suspend fun eliminarTurno(id: String) {
        val response = apiService.eliminarTurno(id)
        if (!response.isSuccessful) {
            throw Exception("Error al eliminar turno: ${response.message()}")
        }
    }
}

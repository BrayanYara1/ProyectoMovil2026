package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.ApiService
import com.example.gestionturnosapp.network.RetrofitClient
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService
) {

    private fun parseError(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                if (errorBody.trim().startsWith("{")) {
                    val gson = com.google.gson.Gson()
                    val map = gson.fromJson(errorBody, Map::class.java)
                    (map["mensaje"] as? String) ?: (map["message"] as? String) ?: (map["error"] as? String) ?: "Error ${response.code()}"
                } else {
                    response.message().ifEmpty { "Error ${response.code()}" }
                }
            } else {
                "Error ${response.code()}: ${response.message()}"
            }
        } catch (e: Exception) {
            "Error ${response.code()}"
        }
    }

    suspend fun getMensajes(): List<Mensaje> {
        val response = apiService.getMensajes()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception(parseError(response))
        }
    }

    suspend fun enviarMensaje(texto: String): Mensaje? {
        val response = apiService.enviarMensaje(mapOf("texto" to texto))
        if (response.isSuccessful) {
            return response.body()
        } else {
            throw Exception(parseError(response))
        }
    }
}

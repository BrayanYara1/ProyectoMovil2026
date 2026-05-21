package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient
import retrofit2.Response

class AuthRepository {
    private val apiService = RetrofitClient.instance

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

    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return apiService.login(request)
    }

    suspend fun register(request: RegisterRequest): Response<AuthResponse> {
        return apiService.register(request)
    }

    fun getErrorMessage(response: Response<*>): String {
        return parseError(response)
    }
}

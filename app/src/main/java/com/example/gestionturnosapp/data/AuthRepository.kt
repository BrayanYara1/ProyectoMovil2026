package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient
import retrofit2.Response

class AuthRepository {
    private val apiService = RetrofitClient.instance

    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return apiService.login(request)
    }

    suspend fun register(request: RegisterRequest): Response<AuthResponse> {
        return apiService.register(request)
    }

    fun getErrorMessage(response: Response<*>): String {
        return RetrofitClient.parseError(response)
    }
}

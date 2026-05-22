package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.ApiService
import com.example.gestionturnosapp.network.RetrofitClient
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {

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

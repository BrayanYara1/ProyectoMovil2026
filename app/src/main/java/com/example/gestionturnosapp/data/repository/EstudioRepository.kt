package com.example.gestionturnosapp.data.repository

import com.example.gestionturnosapp.data.model.EstudioMedico
import com.example.gestionturnosapp.data.remote.ApiService
import com.example.gestionturnosapp.data.remote.RetrofitClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstudioRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getEstudios(): List<EstudioMedico> {
        val response = apiService.getEstudios()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun agregarEstudio(estudio: EstudioMedico): EstudioMedico? {
        val response = apiService.agregarEstudio(estudio)
        if (response.isSuccessful) {
            return response.body()
        } else {
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun eliminarEstudio(id: String) {
        val response = apiService.eliminarEstudio(id)
        if (!response.isSuccessful) {
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun agregarEstudioConDetalle(estudio: EstudioMedico): retrofit2.Response<EstudioMedico> {
        return apiService.agregarEstudio(estudio)
    }
}

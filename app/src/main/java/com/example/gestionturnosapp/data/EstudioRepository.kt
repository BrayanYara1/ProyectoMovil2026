package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class EstudioRepository {
    private val apiService = RetrofitClient.instance

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

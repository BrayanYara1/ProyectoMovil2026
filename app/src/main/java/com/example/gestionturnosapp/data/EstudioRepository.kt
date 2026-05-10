package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class EstudioRepository {
    private val apiService = RetrofitClient.instance

    suspend fun getEstudios(): List<EstudioMedico> {
        return try {
            val response = apiService.getEstudios()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun agregarEstudio(estudio: EstudioMedico): EstudioMedico? {
        return try {
            val response = apiService.agregarEstudio(estudio)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun agregarEstudioConDetalle(estudio: EstudioMedico): retrofit2.Response<EstudioMedico> {
        return apiService.agregarEstudio(estudio)
    }
}

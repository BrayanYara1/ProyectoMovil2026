package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class EstudioRepository {
    private val apiService = RetrofitClient.instance

    private fun parseError(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val gson = com.google.gson.Gson()
                val map = gson.fromJson(errorBody, Map::class.java)
                (map["message"] as? String) ?: (map["error"] as? String) ?: "Error ${response.code()}"
            } else {
                "Error ${response.code()}"
            }
        } catch (e: Exception) {
            "Error ${response.code()}"
        }
    }

    suspend fun getEstudios(): List<EstudioMedico> {
        val response = apiService.getEstudios()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception(parseError(response))
        }
    }

    suspend fun agregarEstudio(estudio: EstudioMedico): EstudioMedico? {
        val response = apiService.agregarEstudio(estudio)
        if (response.isSuccessful) {
            return response.body()
        } else {
            throw Exception(parseError(response))
        }
    }

    suspend fun agregarEstudioConDetalle(estudio: EstudioMedico): retrofit2.Response<EstudioMedico> {
        return apiService.agregarEstudio(estudio)
    }
}

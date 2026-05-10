package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class MedicamentoRepository {
    private val apiService = RetrofitClient.instance

    private fun parseError(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                if (errorBody.trim().startsWith("{")) {
                    val gson = com.google.gson.Gson()
                    val map = gson.fromJson(errorBody, Map::class.java)
                    (map["message"] as? String) ?: (map["error"] as? String) ?: "Error ${response.code()}"
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

    suspend fun getMedicamentos(): List<Medicamento> {
        val response = apiService.getMedicamentos()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception(parseError(response))
        }
    }

    suspend fun agregarMedicamento(med: Medicamento): Medicamento? {
        val response = apiService.agregarMedicamento(med)
        if (response.isSuccessful) {
            return response.body()
        } else {
            throw Exception(parseError(response))
        }
    }

    suspend fun eliminarMedicamento(id: String) {
        val response = apiService.eliminarMedicamento(id)
        if (!response.isSuccessful) {
            throw Exception(parseError(response))
        }
    }
}

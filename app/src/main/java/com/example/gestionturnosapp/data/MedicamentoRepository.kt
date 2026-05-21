package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class MedicamentoRepository {
    private val apiService = RetrofitClient.instance

    suspend fun getMedicamentos(): List<Medicamento> {
        val response = apiService.getMedicamentos()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun agregarMedicamento(med: Medicamento): Medicamento? {
        val response = apiService.agregarMedicamento(med)
        if (response.isSuccessful) {
            return response.body()
        } else {
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun eliminarMedicamento(id: String) {
        val response = apiService.eliminarMedicamento(id)
        if (!response.isSuccessful) {
            throw Exception(RetrofitClient.parseError(response))
        }
    }
}

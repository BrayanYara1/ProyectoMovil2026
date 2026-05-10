package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.network.RetrofitClient

class MedicamentoRepository {
    private val apiService = RetrofitClient.instance

    suspend fun getMedicamentos(): List<Medicamento> {
        val response = apiService.getMedicamentos()
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun agregarMedicamento(med: Medicamento): Medicamento? {
        val response = apiService.agregarMedicamento(med)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }
}

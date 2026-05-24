package com.example.gestionturnosapp.data.repository

import com.example.gestionturnosapp.data.model.Medicamento
import com.example.gestionturnosapp.data.remote.ApiService
import com.example.gestionturnosapp.data.remote.RetrofitClient
import com.example.gestionturnosapp.data.remote.dto.NuevoMedicamentoRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicamentoRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getMedicamentos(): List<Medicamento> {
        val response = apiService.getMedicamentos()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun agregarMedicamento(request: NuevoMedicamentoRequest): Medicamento? {
        val response = apiService.agregarMedicamento(request)
        if (response.isSuccessful) {
            return response.body()
        } else {
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun updateMedicamento(id: String, med: Medicamento): Medicamento? {
        val response = apiService.updateMedicamento(id, med)
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

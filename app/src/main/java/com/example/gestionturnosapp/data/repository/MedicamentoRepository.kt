package com.example.gestionturnosapp.data.repository

import android.content.Context
import com.example.gestionturnosapp.data.local.OfflineCacheManager
import com.example.gestionturnosapp.data.model.Medicamento
import com.example.gestionturnosapp.data.remote.ApiService
import com.example.gestionturnosapp.data.remote.RetrofitClient
import com.example.gestionturnosapp.data.remote.dto.NuevoMedicamentoRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicamentoRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {

    suspend fun getMedicamentos(): List<Medicamento> {
        val response = try {
            apiService.getMedicamentos()
        } catch (e: Exception) {
            return OfflineCacheManager.getCachedMedicamentos(context)
        }

        return if (response.isSuccessful) {
            val meds = response.body() ?: emptyList()
            OfflineCacheManager.saveMedicamentos(context, meds)
            meds
        } else {
            if (response.code() == 401 || response.code() == 403) {
                throw Exception("SESSION_EXPIRED")
            }
            OfflineCacheManager.getCachedMedicamentos(context)
        }
    }

    suspend fun agregarMedicamento(request: NuevoMedicamentoRequest): Medicamento? {
        return try {
            val response = apiService.agregarMedicamento(request)
            if (response.isSuccessful) {
                response.body()
            } else {
                throw Exception(RetrofitClient.parseError(response))
            }
        } catch (e: Exception) {
            if (OfflineCacheManager.isNetworkError(e)) {
                // Crear objeto temporal para cache
                val tempMed = Medicamento(
                    id = "local_${System.currentTimeMillis()}",
                    nombre = request.nombre,
                    dosis = request.dosis,
                    frecuencia = request.frecuencia,
                    proximaToma = request.proximaToma,
                    notas = request.notas
                )
                OfflineCacheManager.addPendingMed(context, tempMed)
                tempMed
            } else {
                throw e
            }
        }
    }

    suspend fun updateMedicamento(id: String, med: Medicamento): Medicamento? {
        val response = apiService.updateMedicamento(id, med)
        return if (response.isSuccessful) {
            response.body()
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

    suspend fun getPendingMeds() = OfflineCacheManager.getPendingMeds(context)
    suspend fun removePendingMeds(synced: List<Medicamento>) = OfflineCacheManager.removePendingMeds(context, synced)
}

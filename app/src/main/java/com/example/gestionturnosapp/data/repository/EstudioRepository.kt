package com.example.gestionturnosapp.data.repository

import android.content.Context
import com.example.gestionturnosapp.data.local.OfflineCacheManager
import com.example.gestionturnosapp.data.model.EstudioMedico
import com.example.gestionturnosapp.data.remote.ApiService
import com.example.gestionturnosapp.data.remote.RetrofitClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstudioRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {

    suspend fun getEstudios(): List<EstudioMedico> {
        return try {
            val response = apiService.getEstudios()
            if (response.isSuccessful) {
                val estudios = response.body() ?: emptyList()
                OfflineCacheManager.saveEstudios(context, estudios)
                estudios
            } else {
                OfflineCacheManager.getCachedEstudios(context)
            }
        } catch (e: Exception) {
            OfflineCacheManager.getCachedEstudios(context)
        }
    }

    suspend fun agregarEstudio(estudio: EstudioMedico): EstudioMedico? {
        return try {
            val response = apiService.agregarEstudio(estudio)
            if (response.isSuccessful) {
                response.body()
            } else {
                throw Exception(RetrofitClient.parseError(response))
            }
        } catch (e: Exception) {
            if (OfflineCacheManager.isNetworkError(e)) {
                OfflineCacheManager.addPendingEstudio(context, estudio)
                estudio
            } else {
                throw e
            }
        }
    }

    suspend fun eliminarEstudio(id: String) {
        val response = apiService.eliminarEstudio(id)
        if (!response.isSuccessful) {
            throw Exception(RetrofitClient.parseError(response))
        }
    }
}

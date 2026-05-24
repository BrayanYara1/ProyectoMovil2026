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
        val response = try {
            apiService.getEstudios()
        } catch (e: Exception) {
            return OfflineCacheManager.getCachedEstudios(context)
        }

        return if (response.isSuccessful) {
            val estudios = response.body() ?: emptyList()
            OfflineCacheManager.saveEstudios(context, estudios)
            estudios
        } else {
            if (response.code() == 401 || response.code() == 403) {
                throw Exception("SESSION_EXPIRED")
            }
            OfflineCacheManager.getCachedEstudios(context)
        }
    }

    suspend fun agregarEstudio(estudio: EstudioMedico): EstudioMedico? {
        val response = try {
            apiService.agregarEstudio(estudio)
        } catch (e: Exception) {
            if (OfflineCacheManager.isNetworkError(e)) {
                if (!estudio.id.startsWith("pending_")) {
                    OfflineCacheManager.addPendingEstudio(context, estudio)
                }
                return estudio
            }
            throw e
        }

        if (response.isSuccessful) {
            return response.body()
        } else {
            if (response.code() == 401 || response.code() == 403) {
                throw Exception("SESSION_EXPIRED")
            }
            throw Exception(RetrofitClient.parseError(response))
        }
    }

    suspend fun eliminarEstudio(id: String) {
        val response = apiService.eliminarEstudio(id)
        if (!response.isSuccessful) {
            if (response.code() == 401 || response.code() == 403) {
                throw Exception("SESSION_EXPIRED")
            }
            throw Exception(RetrofitClient.parseError(response))
        }
    }
}

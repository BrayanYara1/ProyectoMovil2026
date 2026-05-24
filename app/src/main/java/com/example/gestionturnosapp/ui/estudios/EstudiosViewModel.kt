package com.example.gestionturnosapp.ui.estudios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MediatorLiveData
import com.example.gestionturnosapp.data.model.EstudioMedico
import com.example.gestionturnosapp.data.repository.EstudioRepository
import com.example.gestionturnosapp.data.local.OfflineCacheManager
import com.example.gestionturnosapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EstudiosViewModel @Inject constructor(
    application: Application,
    private val repository: EstudioRepository,
) : AndroidViewModel(application) {

    private val _allEstudios = MutableLiveData<List<EstudioMedico>>(emptyList())
    private val _startDate = MutableLiveData<String?>(null)
    private val _endDate = MutableLiveData<String?>(null)
    private val _searchQuery = MutableLiveData("")

    private val _estudiosResource = MediatorLiveData<Resource<List<EstudioMedico>>>().apply {
        val observer = { _: Any? ->
            val list = _allEstudios.value ?: emptyList()
            val start = _startDate.value
            val end = _endDate.value
            val query = _searchQuery.value?.lowercase() ?: ""

            val filtered = list.filter { estudio ->
                val matchesDate = ((start == null || estudio.fecha >= start) && (end == null || estudio.fecha <= end))
                val matchesQuery = query.isEmpty() || 
                                  estudio.titulo.lowercase().contains(query) || 
                                  estudio.tipo.lowercase().contains(query)
                
                matchesDate && matchesQuery
            }
            this.value = Resource.Success(filtered)
        }
        addSource(_allEstudios, observer)
        addSource(_startDate, observer)
        addSource(_endDate, observer)
        addSource(_searchQuery, observer)
    }
    val estudios: LiveData<Resource<List<EstudioMedico>>> = _estudiosResource

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDateFilter(start: String?, end: String?) {
        _startDate.value = start
        _endDate.value = end
    }

    private val _createResource = MutableLiveData<Resource<EstudioMedico>>(Resource.Idle)
    val createResource: LiveData<Resource<EstudioMedico>> = _createResource

    fun resetCreateState() {
        _createResource.value = Resource.Idle
    }

    fun loadEstudios() {
        viewModelScope.launch {
            _estudiosResource.value = Resource.Loading
            
            // Offline Cache First
            val cached = OfflineCacheManager.getCachedEstudios(getApplication())
            if (cached.isNotEmpty()) {
                _allEstudios.value = cached
                _estudiosResource.value = Resource.Success(cached)
            }

            try {
                val list = repository.getEstudios()
                _allEstudios.value = list
                
                // Save to Cache
                OfflineCacheManager.saveEstudios(getApplication(), list)
            } catch (e: Exception) {
                if (_allEstudios.value.isNullOrEmpty()) {
                    _estudiosResource.value = Resource.Error(e.localizedMessage ?: "Error al cargar")
                }
            }
        }
    }

    fun agregarEstudio(titulo: String, fecha: String, tipo: String, resultado: String, photoUrl: String? = null) {
        viewModelScope.launch {
            _createResource.value = Resource.Loading
            // Generar un ID local único para evitar conflictos en el DiffUtil si hay varios pendientes
            val localId = "pending_${System.currentTimeMillis()}"
            val nuevo = EstudioMedico(localId, titulo, fecha, tipo, resultado, urlDocumento = photoUrl)
            try {
                val response = repository.agregarEstudioConDetalle(nuevo.copy(id = "")) 
                
                if (response.isSuccessful && response.body() != null) {
                    _createResource.value = Resource.Success(response.body()!!)
                    loadEstudios()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    _createResource.value = Resource.Error(errorMsg)
                }
            } catch (e: Exception) {
                if (OfflineCacheManager.isNetworkError(e)) {
                    OfflineCacheManager.addPendingEstudio(getApplication(), nuevo)
                    _createResource.value = Resource.Success(nuevo)
                    loadEstudios()
                } else {
                    _createResource.value = Resource.Error(e.localizedMessage ?: "Error")
                }
            }
        }
    }

    fun syncPendingEstudios() {
        val pending = OfflineCacheManager.getPendingEstudios(getApplication())
        if (pending.isEmpty()) return

        viewModelScope.launch {
            val synced = mutableListOf<EstudioMedico>()
            pending.forEach { estudio ->
                try {
                    repository.agregarEstudio(estudio)
                    synced.add(estudio)
                } catch (_: Exception) {
                    return@forEach
                }
            }
            if (synced.isNotEmpty()) {
                OfflineCacheManager.removePendingEstudios(getApplication(), synced)
                loadEstudios()
            }
        }
    }

    fun eliminarEstudio(id: String) {
        viewModelScope.launch {
            _estudiosResource.value = Resource.Loading
            try {
                repository.eliminarEstudio(id)
                loadEstudios()
            } catch (e: Exception) {
                _estudiosResource.value = Resource.Error(e.localizedMessage ?: "Error al eliminar")
            }
        }
    }
}

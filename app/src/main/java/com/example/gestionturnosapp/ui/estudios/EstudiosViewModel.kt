package com.example.gestionturnosapp.ui.estudios

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MediatorLiveData
import com.example.gestionturnosapp.data.EstudioMedico
import com.example.gestionturnosapp.data.EstudioRepository
import com.example.gestionturnosapp.data.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EstudiosViewModel : ViewModel() {

    private val repository = EstudioRepository()

    private val _allEstudios = MutableLiveData<List<EstudioMedico>>(emptyList())
    private val _startDate = MutableLiveData<String?>(null)
    private val _endDate = MutableLiveData<String?>(null)
    private val _searchQuery = MutableLiveData<String>("")

    private val _estudiosResource = MediatorLiveData<Resource<List<EstudioMedico>>>().apply {
        val observer = { _: Any? ->
            val list = _allEstudios.value ?: emptyList()
            val start = _startDate.value
            val end = _endDate.value
            val query = _searchQuery.value?.lowercase() ?: ""

            val filtered = list.filter { estudio ->
                val matchesDate = (start == null || estudio.fecha >= start) && (end == null || estudio.fecha <= end)
                val matchesQuery = query.isEmpty() || 
                                  estudio.titulo.lowercase().contains(query) || 
                                  estudio.tipo.lowercase().contains(query)
                
                matchesDate && matchesQuery
            }
            value = Resource.Success(filtered)
        }
        addSource(_allEstudios, observer)
        addSource(_startDate, observer)
        addSource(_endDate, observer)
        addSource(_searchQuery, observer)
    }
    val estudios: LiveData<Resource<List<EstudioMedico>>> = _estudiosResource

    init {
        // Carga iniciada desde Fragment para soporte Offline
    }

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

    fun loadEstudios(context: android.content.Context? = null) {
        viewModelScope.launch {
            _estudiosResource.value = Resource.Loading
            
            // Offline Cache First
            context?.let {
                val cached = com.example.gestionturnosapp.data.OfflineCacheManager.getCachedEstudios(it)
                if (cached.isNotEmpty()) {
                    _allEstudios.value = cached
                    _estudiosResource.value = Resource.Success(cached)
                }
            }

            try {
                val list = repository.getEstudios()
                _allEstudios.value = list
                
                // Save to Cache
                context?.let { com.example.gestionturnosapp.data.OfflineCacheManager.saveEstudios(it, list) }
            } catch (e: Exception) {
                if (_allEstudios.value.isNullOrEmpty()) {
                    _estudiosResource.value = Resource.Error(e.localizedMessage ?: "Error al cargar")
                }
            }
        }
    }

    fun agregarEstudio(context: android.content.Context, titulo: String, fecha: String, tipo: String, resultado: String, photoUrl: String? = null) {
        viewModelScope.launch {
            _createResource.value = Resource.Loading
            val nuevo = EstudioMedico("", titulo, fecha, tipo, resultado, urlDocumento = photoUrl)
            try {
                val response = repository.agregarEstudioConDetalle(nuevo)
                
                if (response.isSuccessful && response.body() != null) {
                    _createResource.value = Resource.Success(response.body()!!)
                    loadEstudios(context)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido del servidor"
                    _createResource.value = Resource.Error("Error ${response.code()}: $errorMsg")
                }
            } catch (e: Exception) {
                if (com.example.gestionturnosapp.data.OfflineCacheManager.isNetworkError(e)) {
                    com.example.gestionturnosapp.data.OfflineCacheManager.addPendingEstudio(context, nuevo)
                    _createResource.value = Resource.Success(nuevo.copy(id = "pending"))
                    loadEstudios(context)
                } else {
                    _createResource.value = Resource.Error("Error de conexión: ${e.localizedMessage}")
                }
            }
        }
    }

    fun syncPendingEstudios(context: android.content.Context) {
        val pending = com.example.gestionturnosapp.data.OfflineCacheManager.getPendingEstudios(context)
        if (pending.isEmpty()) return

        viewModelScope.launch {
            val synced = mutableListOf<EstudioMedico>()
            pending.forEach { estudio ->
                try {
                    repository.agregarEstudio(estudio)
                    synced.add(estudio)
                } catch (e: Exception) {
                    return@forEach
                }
            }
            if (synced.isNotEmpty()) {
                com.example.gestionturnosapp.data.OfflineCacheManager.removePendingEstudios(context, synced)
                loadEstudios(context)
            }
        }
    }

    fun eliminarEstudio(context: android.content.Context, id: String) {
        viewModelScope.launch {
            _estudiosResource.value = Resource.Loading
            try {
                repository.eliminarEstudio(id)
                loadEstudios(context)
            } catch (e: Exception) {
                _estudiosResource.value = Resource.Error(e.localizedMessage ?: "Error al eliminar")
            }
        }
    }
}

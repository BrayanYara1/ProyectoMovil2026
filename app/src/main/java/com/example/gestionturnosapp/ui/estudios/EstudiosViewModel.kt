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
        loadEstudios()
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

    fun loadEstudios() {
        viewModelScope.launch {
            _estudiosResource.value = Resource.Loading
            try {
                val list = repository.getEstudios()
                _allEstudios.value = list
            } catch (e: Exception) {
                _estudiosResource.value = Resource.Error(e.localizedMessage ?: "Error al cargar")
            }
        }
    }

    fun agregarEstudio(titulo: String, fecha: String, tipo: String, resultado: String, photoUrl: String? = null) {
        viewModelScope.launch {
            _createResource.value = Resource.Loading
            try {
                val nuevo = EstudioMedico("", titulo, fecha, tipo, resultado, urlDocumento = photoUrl)
                val response = repository.agregarEstudioConDetalle(nuevo)
                
                if (response.isSuccessful && response.body() != null) {
                    _createResource.value = Resource.Success(response.body()!!)
                    loadEstudios()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido del servidor"
                    _createResource.value = Resource.Error("Error ${response.code()}: $errorMsg")
                }
            } catch (e: Exception) {
                _createResource.value = Resource.Error("Error de conexión: ${e.localizedMessage}")
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

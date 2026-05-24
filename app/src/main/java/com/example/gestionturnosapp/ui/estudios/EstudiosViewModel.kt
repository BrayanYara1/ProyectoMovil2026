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
        val update = { _: Any? ->
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
        addSource(_allEstudios, update)
        addSource(_startDate, update)
        addSource(_endDate, update)
        addSource(_searchQuery, update)
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
            
            try {
                // El repo maneja Server -> Cache
                val list = repository.getEstudios()
                
                // Mezclar con pendientes
                val pending = OfflineCacheManager.getPendingEstudios(getApplication())
                val combined = (list + pending).distinctBy { it.id }
                
                _allEstudios.value = combined
            } catch (e: Exception) {
                val current = _allEstudios.value ?: emptyList()
                if (current.isEmpty()) {
                    _estudiosResource.value = Resource.Error(e.localizedMessage ?: "Error al cargar estudios")
                } else {
                    // Si ya hay datos, forzamos la actualización del Resource para quitar el Loading
                    // y mostrar lo que tenemos (el update observer se encargará)
                    _allEstudios.value = current
                }
            }
        }
    }

    fun agregarEstudio(titulo: String, fecha: String, tipo: String, resultado: String, photoUrl: String? = null) {
        viewModelScope.launch {
            _createResource.value = Resource.Loading
            
            val localId = "pending_${System.currentTimeMillis()}"
            val nuevo = EstudioMedico(
                id = localId,
                titulo = titulo,
                fecha = fecha,
                tipo = tipo,
                resultadoBreve = resultado,
                urlDocumento = photoUrl
            )
            
            try {
                val result = repository.agregarEstudio(nuevo)
                // Si el result es el mismo 'nuevo', se guardó como pendiente (ID empieza con pending_)
                // Si es distinto, vino del servidor. En ambos casos es un Success para la UI de creación.
                _createResource.value = Resource.Success(result ?: nuevo)
                loadEstudios()
            } catch (e: Exception) {
                _createResource.value = Resource.Error(e.localizedMessage ?: "Error al guardar estudio")
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
                    val result = repository.agregarEstudio(estudio)
                    // Consideramos sincronizado si el servidor devolvió un ID real
                    if (result != null && result.id != estudio.id) {
                        synced.add(estudio)
                    }
                } catch (_: Exception) {
                    // Si falla uno crítico, saltamos al siguiente
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
            // No cambiamos _estudiosResource a Loading aquí para evitar parpadeos de toda la lista
            // Una mejor opción sería un estado de "eliminando" específico o solo el feedback tras el resultado
            try {
                repository.eliminarEstudio(id)
                loadEstudios()
            } catch (e: Exception) {
                // Si falla, notificamos el error sin borrar lo que hay en pantalla
                _estudiosResource.value = Resource.Error(e.localizedMessage ?: "Error al eliminar")
                // Intentamos recuperar el estado anterior disparando el observer
                _allEstudios.value = _allEstudios.value
            }
        }
    }
}

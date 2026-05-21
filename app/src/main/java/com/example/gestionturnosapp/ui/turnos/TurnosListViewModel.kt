package com.example.gestionturnosapp.ui.turnos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MediatorLiveData
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.data.TurnoRepository
import com.example.gestionturnosapp.data.NuevoTurnoRequest
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.data.OfflineCacheManager
import com.example.gestionturnosapp.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TurnosListViewModel : ViewModel() {

    private val repository = TurnoRepository()

    private val _turnos = MutableLiveData<List<Turno>>()
    
    private val _turnosResource = MutableLiveData<Resource<List<Turno>>>()
    val turnosResource: LiveData<Resource<List<Turno>>> = _turnosResource
    
    private val _searchQuery = MutableLiveData<String>("")
    private val _filterStatus = MutableLiveData<String>("TODOS")

    val filteredTurnos = MediatorLiveData<List<Turno>>().apply {
        addSource(_turnos) { value = applyFilters() }
        addSource(_searchQuery) { value = applyFilters() }
        addSource(_filterStatus) { value = applyFilters() }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _createTurnoResource = MutableLiveData<Resource<Turno>>(Resource.Idle)
    val createTurnoResource: LiveData<Resource<Turno>> = _createTurnoResource

    private val _isSlotAvailable = MutableLiveData<Boolean?>(null)
    val isSlotAvailable: LiveData<Boolean?> = _isSlotAvailable

    private var availabilityJob: Job? = null

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    private fun applyFilters(): List<Turno> {
        val currentList = _turnos.value ?: return emptyList()
        val query = _searchQuery.value?.lowercase() ?: ""
        val status = _filterStatus.value ?: "TODOS"

        return currentList.filter { turno ->
            val matchesQuery = turno.pacienteNombre.lowercase().contains(query) || 
                             turno.motivo.lowercase().contains(query)
            
            val matchesStatus = when (status) {
                "PENDIENTE" -> turno.estado.lowercase() in listOf("pendiente", "pending")
                "COMPLETADO" -> turno.estado.lowercase() in listOf("completado", "completed")
                "CANCELADO" -> turno.estado.lowercase() in listOf("cancelado", "cancelled")
                else -> true
            }
            
            matchesQuery && matchesStatus
        }
    }

    fun fetchTurnos(context: android.content.Context? = null) {
        viewModelScope.launch {
            _turnosResource.value = Resource.Loading
            _isLoading.value = true
            
            // 1. Cargar de caché primero para respuesta instantánea (Offline Pro)
            context?.let {
                val cached = OfflineCacheManager.getCachedTurnos(it)
                if (cached.isNotEmpty()) {
                    _turnos.value = cached
                    _turnosResource.value = Resource.Success(cached)
                }
            }

            try {
                val turnosList = repository.getTurnos()
                _turnos.value = turnosList
                _turnosResource.value = Resource.Success(turnosList)
                
                // 2. Guardar en caché para uso offline
                context?.let { OfflineCacheManager.saveTurnos(it, turnosList) }
                
            } catch (e: Exception) {
                android.util.Log.e("TurnosListViewModel", "Error fetchTurnos", e)
                // Si ya tenemos datos del caché, no mostramos error crítico, solo log
                if (_turnos.value.isNullOrEmpty()) {
                    _turnosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun crearNuevoTurno(context: android.content.Context, nombre: String, fecha: String, hora: String, motivo: String, especialidad: String? = null, doctor: String? = null) {
        viewModelScope.launch {
            _createTurnoResource.value = Resource.Loading
            _isLoading.value = true

            val fechaNormalizada = fecha.replace("/", "-").trim()
            val horaNormalizada = DateUtils.formatTo24h(hora.trim())

            val request = NuevoTurnoRequest(
                nombre, 
                fechaNormalizada, 
                horaNormalizada, 
                motivo, 
                especialidad ?: "General", 
                doctor ?: "Dr. Asignado"
            )
            try {
                val nuevoTurno = repository.crearTurno(request)
                if (nuevoTurno != null) {
                    _createTurnoResource.value = Resource.Success(nuevoTurno)
                    fetchTurnos(context)
                } else {
                    _createTurnoResource.value = Resource.Error("Error")
                }
            } catch (e: Exception) {
                if (OfflineCacheManager.isNetworkError(e)) {
                    // MODO OFFLINE: Guardar para sincronizar después
                    OfflineCacheManager.addPendingTurno(context, request)
                    _createTurnoResource.value = Resource.Success(Turno("pending", nombre, fechaNormalizada, horaNormalizada, motivo, context.getString(R.string.status_pending_offline), especialidad, doctor))
                    fetchTurnos(context)
                } else {
                    _createTurnoResource.value = Resource.Error(e.localizedMessage ?: "Error")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncPendingTurnos(context: android.content.Context) {
        val pending = OfflineCacheManager.getPendingTurnos(context)
        if (pending.isEmpty()) return

        viewModelScope.launch {
            val synced = mutableListOf<NuevoTurnoRequest>()
            pending.forEach { request ->
                try {
                    repository.crearTurno(request)
                    synced.add(request)
                } catch (e: Exception) {
                    // Si falla uno, paramos pero guardamos los que sí se sincronizaron
                    return@forEach
                }
            }
            if (synced.isNotEmpty()) {
                OfflineCacheManager.removePendingTurnos(context, synced)
                fetchTurnos(context)
            }
        }
    }

    private val _turnoEliminadoExitosamente = MutableLiveData<Boolean>(false)
    val turnoEliminadoExitosamente: LiveData<Boolean> = _turnoEliminadoExitosamente

    fun resetNavegacion() {
        _createTurnoResource.value = Resource.Idle
        _turnoEliminadoExitosamente.value = false
        _isSlotAvailable.value = null
    }

    fun verificarDisponibilidad(fecha: String, hora: String) {
        val fLimpia = fecha.replace("/", "-").trim()
        val hLimpia = DateUtils.formatTo24h(hora.trim())

        if (fLimpia.isEmpty() || hLimpia.isEmpty()) {
            _isSlotAvailable.value = null
            return
        }
        
        availabilityJob?.cancel()
        availabilityJob = viewModelScope.launch {
            try {
                // Verificar disponibilidad REAL contra el servidor (todos los usuarios)
                val disponible = repository.checkAvailability(fLimpia, hLimpia)
                _isSlotAvailable.value = disponible
            } catch (e: Exception) {
                if (_isSlotAvailable.value == null) _isSlotAvailable.value = true
            }
        }
    }

    fun eliminarTurno(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.eliminarTurno(id)
                _turnoEliminadoExitosamente.value = true
                fetchTurnos()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        availabilityJob?.cancel()
    }
}

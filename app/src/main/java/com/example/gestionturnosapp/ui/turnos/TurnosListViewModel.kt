package com.example.gestionturnosapp.ui.turnos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MediatorLiveData
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.data.TurnoRepository
import com.example.gestionturnosapp.data.NuevoTurnoRequest
import com.example.gestionturnosapp.data.Resource
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

    fun fetchTurnos() {
        viewModelScope.launch {
            _turnosResource.value = Resource.Loading
            _isLoading.value = true
            try {
                val turnosList = repository.getTurnos()
                _turnos.value = turnosList
                _turnosResource.value = Resource.Success(turnosList)
            } catch (e: Exception) {
                android.util.Log.e("TurnosListViewModel", "Error fetchTurnos", e)
                _turnosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun crearNuevoTurno(nombre: String, fecha: String, hora: String, motivo: String, especialidad: String? = null, doctor: String? = null) {
        viewModelScope.launch {
            _createTurnoResource.value = Resource.Loading
            _isLoading.value = true

            // Normalizar fecha y hora antes de enviar al servidor
            val fechaNormalizada = fecha.replace("/", "-").trim()
            val horaNormalizada = normalizeTime(hora.trim())

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
                    fetchTurnos()
                } else {
                    _createTurnoResource.value = Resource.Error("Error")
                }
            } catch (e: Exception) {
                _createTurnoResource.value = Resource.Error(e.localizedMessage ?: "Error")
            } finally {
                _isLoading.value = false
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
        val hLimpia = normalizeTime(hora.trim())

        if (fLimpia.isEmpty() || hLimpia.isEmpty()) {
            _isSlotAvailable.value = null
            return
        }
        
        availabilityJob?.cancel()
        availabilityJob = viewModelScope.launch {
            try {
                val lista = repository.getTurnos()
                _turnos.value = lista
                val ocupado = lista.any { 
                    it.fecha.replace("/", "-").trim() == fLimpia && 
                    normalizeTime(it.hora.trim()) == hLimpia &&
                    it.estado.lowercase() !in listOf("cancelado", "cancelled")
                }
                _isSlotAvailable.value = !ocupado
            } catch (e: Exception) {
                if (_isSlotAvailable.value == null) _isSlotAvailable.value = true
            }
        }
    }

    private fun normalizeTime(time: String): String {
        return try {
            val inputFormats = listOf("hh:mm a", "h:mm a", "HH:mm")
            var date: java.util.Date? = null
            
            for (format in inputFormats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                    sdf.isLenient = false
                    date = sdf.parse(time)
                    if (date != null) break
                } catch (e: Exception) {
                    continue
                }
            }
            
            if (date != null) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(date)
            } else {
                // Fallback manual mejorado si fallan los parsers
                val parts = time.split(":")
                if (parts.size >= 2) {
                    val h = parts[0].trim().padStart(2, '0')
                    val m = parts[1].trim().take(2).padStart(2, '0')
                    "$h:$m"
                } else time
            }
        } catch (e: Exception) {
            time
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
}

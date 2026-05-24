package com.example.gestionturnosapp.ui.turnos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.model.Turno
import com.example.gestionturnosapp.data.repository.TurnoRepository
import com.example.gestionturnosapp.data.remote.dto.NuevoTurnoRequest
import com.example.gestionturnosapp.util.Resource
import com.example.gestionturnosapp.data.local.OfflineCacheManager
import com.example.gestionturnosapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TurnosListViewModel @Inject constructor(
    application: Application,
    private val repository: TurnoRepository,
) : AndroidViewModel(application) {

    private val _turnos = MutableLiveData<List<Turno>>()
    
    private val _turnosResource = MutableLiveData<Resource<List<Turno>>>()
    val turnosResource: LiveData<Resource<List<Turno>>> = _turnosResource
    
    private val _searchQuery = MutableLiveData("")
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

    // CAMPOS FORMULARIO REACTIVO
    val formPacienteNombre = MutableLiveData("")
    val formFecha = MutableLiveData("")
    val formHora = MutableLiveData("")
    val formMotivo = MutableLiveData("")

    val isFormValid = MediatorLiveData<Boolean>().apply {
        val observer = { _: Any? ->
            val nombre = formPacienteNombre.value ?: ""
            val fecha = formFecha.value ?: ""
            val hora = formHora.value ?: ""
            val motivo = formMotivo.value ?: ""
            val disponible = isSlotAvailable.value
            
            value = nombre.isNotBlank() && 
                    fecha.isNotBlank() && 
                    hora.isNotBlank() && 
                    motivo.isNotBlank() &&
                    disponible != false // Si es null (no verificado) o true (disponible), es válido
        }
        addSource(formPacienteNombre, observer)
        addSource(formFecha, observer)
        addSource(formHora, observer)
        addSource(formMotivo, observer)
        addSource(isSlotAvailable, observer)
    }

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
                "PENDIENTE" -> {
                    val st = turno.estado.lowercase()
                    st.contains("pendiente") || st.contains("pending")
                }
                "COMPLETADO" -> {
                    val st = turno.estado.lowercase()
                    st.contains("completado") || st.contains("completed")
                }
                "CANCELADO" -> {
                    val st = turno.estado.lowercase()
                    st.contains("cancelado") || st.contains("cancelled")
                }
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
                // El Repo maneja Server -> Cache
                val turnosList = repository.getTurnos()
                
                // Mezclar con pendientes locales si los hubiera
                val pendingRequests = OfflineCacheManager.getPendingTurnos(getApplication())
                val pendingTurnos = pendingRequests.map { req ->
                    Turno("pending_${req.fecha}_${req.hora}", req.nombre, req.fecha, req.hora, req.motivo, getApplication<Application>().getString(R.string.status_pending_offline), req.especialidad, req.doctor)
                }
                
                val combined = (turnosList + pendingTurnos).distinctBy { "${it.fecha}_${it.hora}" }
                
                _turnos.value = combined
                _turnosResource.value = Resource.Success<List<Turno>>(combined)
                
            } catch (e: Exception) {
                if (_turnos.value.isNullOrEmpty()) {
                    _turnosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun crearNuevoTurno(especialidad: String? = null, doctor: String? = null) {
        val nombre = formPacienteNombre.value ?: ""
        val fecha = formFecha.value ?: ""
        val hora = formHora.value ?: ""
        val motivo = formMotivo.value ?: ""

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
                // El Repo maneja la creación o el encolado offline
                val nuevoTurno = repository.crearTurno(request)
                
                if (nuevoTurno != null) {
                    _createTurnoResource.value = Resource.Success<Turno>(nuevoTurno)
                } else {
                    // Si es null pero no lanzó excepción, el repo lo guardó como pendiente
                    val pendingTurno = Turno("pending", nombre, fechaNormalizada, horaNormalizada, motivo, getApplication<Application>().getString(R.string.status_pending_offline), especialidad, doctor)
                    _createTurnoResource.value = Resource.Success<Turno>(pendingTurno)
                }
                fetchTurnos()
            } catch (e: Exception) {
                _createTurnoResource.value = Resource.Error(e.localizedMessage ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncPendingTurnos() {
        val pending = OfflineCacheManager.getPendingTurnos(getApplication())
        if (pending.isEmpty()) return

        viewModelScope.launch {
            val synced = mutableListOf<NuevoTurnoRequest>()
            pending.forEach { request ->
                try {
                    repository.crearTurno(request)
                    synced.add(request)
                } catch (_: Exception) {
                    // Si falla uno, paramos pero guardamos los que sí se sincronizaron
                    return@forEach
                }
            }
            if (synced.isNotEmpty()) {
                OfflineCacheManager.removePendingTurnos(getApplication(), synced)
                fetchTurnos()
            }
        }
    }

    private val _turnoEliminadoExitosamente = MutableLiveData<Boolean>(false)
    val turnoEliminadoExitosamente: LiveData<Boolean> = _turnoEliminadoExitosamente

    fun resetNavegacion() {
        _createTurnoResource.value = Resource.Idle
        _turnoEliminadoExitosamente.value = false
        _isSlotAvailable.value = null
        // Limpiar formulario
        formPacienteNombre.value = ""
        formFecha.value = ""
        formHora.value = ""
        formMotivo.value = ""
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

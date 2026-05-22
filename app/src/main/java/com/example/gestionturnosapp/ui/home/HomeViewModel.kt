package com.example.gestionturnosapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.MedicamentoRepository
import com.example.gestionturnosapp.data.OfflineCacheManager
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.data.TurnoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val turnoRepository: TurnoRepository,
    private val medRepository: MedicamentoRepository,
) : AndroidViewModel(application) {

    private val _turnosCount = MutableLiveData<Int>()
    val turnosCount: LiveData<Int> = _turnosCount

    private val _nextTurno = MutableLiveData<Turno?>()
    val nextTurno: LiveData<Turno?> = _nextTurno

    private val _allTurnos = MutableLiveData<List<Turno>>(emptyList())
    val allTurnos: LiveData<List<Turno>> = _allTurnos

    private val _medicamentos = MutableLiveData<List<Medicamento>>(emptyList())
    val medicamentos: LiveData<List<Medicamento>> = _medicamentos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _healthTipResId = MutableLiveData<Int>()
    val healthTipResId: LiveData<Int> = _healthTipResId

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var refreshJob: Job? = null

    init {
        loadRandomHealthTip()
        // Carga inmediata de cache al iniciar
        refreshLocalData()
        // Iniciar sync y refresh
        syncAll()
    }

    private fun refreshLocalData() {
        viewModelScope.launch {
            val local = OfflineCacheManager.getCachedMedicamentos(getApplication())
            val pending = OfflineCacheManager.getPendingMeds(getApplication())
            val combined = (local + pending).distinctBy { it.nombre.lowercase().trim() }
            _medicamentos.postValue(combined)
            
            val cachedTurnos = OfflineCacheManager.getCachedTurnos(getApplication())
            if (cachedTurnos.isNotEmpty()) updateTurnosUI(cachedTurnos)
        }
    }

    fun refreshData() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoading.postValue(true)
            _errorMessage.postValue(null)

            try {
                // Paso 1: Mostrar lo local de inmediato
                refreshLocalData()

                // Paso 2: Llamadas al servidor en paralelo
                val turnosDef = async { try { turnoRepository.getTurnos() } catch (e: Exception) { null } }
                val medsDef = async { try { medRepository.getMedicamentos() } catch (e: Exception) { null } }

                val serverTurnos = turnosDef.await()
                val serverMeds = medsDef.await()

                // Paso 3: Actualizar con lo que venga del servidor
                if (serverTurnos != null) {
                    updateTurnosUI(serverTurnos)
                    OfflineCacheManager.saveTurnos(getApplication(), serverTurnos)
                }

                if (serverMeds != null) {
                    // Mezclar server con los pendientes actuales (que podrían haber cambiado durante la red)
                    val currentPending = OfflineCacheManager.getPendingMeds(getApplication())
                    val finalMeds = (serverMeds + currentPending).distinctBy { it.nombre.lowercase().trim() }
                    _medicamentos.postValue(finalMeds)
                    
                    // Solo guardar en cache si hay algo real del servidor
                    if (serverMeds.isNotEmpty()) {
                        OfflineCacheManager.saveMedicamentos(getApplication(), serverMeds)
                    }
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("HomeViewModel", "Refresh Error", e)
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun updateTurnosUI(turnos: List<Turno>) {
        _allTurnos.value = turnos
        _turnosCount.value = turnos.size
        
        _nextTurno.value = turnos.asSequence()
            .filter { it.estado.lowercase() in listOf("pendiente", "pending") }
            .minByOrNull { turno ->
                // Normalizar hora para ordenamiento lexicográfico correcto (HH:mm)
                val horaNormalizada = com.example.gestionturnosapp.util.DateUtils.formatTo24h(turno.hora)
                "${turno.fecha} $horaNormalizada"
            }
    }

    fun syncAll() {
        viewModelScope.launch {
            val pendingMeds = OfflineCacheManager.getPendingMeds(getApplication())
            if (pendingMeds.isNotEmpty()) {
                val syncedMeds = mutableListOf<Medicamento>()
                for (med in pendingMeds) {
                    try {
                        val res = medRepository.agregarMedicamento(med)
                        if (res != null) syncedMeds.add(med)
                    } catch (e: Exception) {
                        break // Si falla uno, paramos este ciclo
                    }
                }
                if (syncedMeds.isNotEmpty()) {
                    OfflineCacheManager.removePendingMeds(getApplication(), syncedMeds)
                }
            }
            // Después de intentar sincronizar, pedimos los datos frescos
            refreshData()
        }
    }

    fun marcarComoTomado(med: Medicamento) {
        viewModelScope.launch {
            try {
                // Lógica simple: Extraer horas de la frecuencia (ej: "Cada 8 horas" -> 8)
                val hoursToAdd = med.frecuencia.filter { it.isDigit() }.toIntOrNull() ?: 8
                
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                val calendar = java.util.Calendar.getInstance()
                
                // Si la proxima toma ya pasó, empezamos desde ahora
                val proximaDate = try { sdf.parse(med.proximaToma) } catch (_: Exception) { null }
                if (proximaDate != null) {
                    val pCal = java.util.Calendar.getInstance().apply { time = proximaDate }
                    // Solo actualizamos la hora/minuto en el calendario de "hoy"
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, pCal.get(java.util.Calendar.HOUR_OF_DAY))
                    calendar.set(java.util.Calendar.MINUTE, pCal.get(java.util.Calendar.MINUTE))
                }
                
                calendar.add(java.util.Calendar.HOUR_OF_DAY, hoursToAdd)
                val nuevaProximaToma = sdf.format(calendar.time)
                
                val medActualizado = med.copy(proximaToma = nuevaProximaToma)
                
                // Actualizar en el servidor
                medRepository.updateMedicamento(med.id, medActualizado)
                
                // Refrescar datos
                refreshData()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error marking as taken", e)
            }
        }
    }

    private fun loadRandomHealthTip() {
        val tips = listOf(R.string.tip_health_1, R.string.tip_health_2, R.string.tip_health_3, R.string.tip_health_4, R.string.tip_health_5, R.string.tip_health_6, R.string.tip_health_7, R.string.tip_health_8, R.string.tip_health_9, R.string.tip_health_10)
        val dayOfYear = Calendar.getInstance()[Calendar.DAY_OF_YEAR]
        _healthTipResId.value = tips[dayOfYear % tips.size]
    }
}

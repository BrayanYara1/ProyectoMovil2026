package com.example.gestionturnosapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.MedicamentoRepository
import com.example.gestionturnosapp.data.NuevoTurnoRequest
import com.example.gestionturnosapp.data.OfflineCacheManager
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.data.TurnoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val turnoRepository: TurnoRepository,
    private val medRepository: MedicamentoRepository
) : AndroidViewModel(application) {

    private val _turnosCount = MutableLiveData<Int>()
    val turnosCount: LiveData<Int> = _turnosCount

    private val _nextTurno = MutableLiveData<Turno?>()
    val nextTurno: LiveData<Turno?> = _nextTurno

    private val _medicamentos = MutableLiveData<List<Medicamento>>()
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
        // Carga inicial desde caché para rapidez
        loadFromCache()
    }

    private fun loadFromCache() {
        viewModelScope.launch {
            val cachedTurnos = OfflineCacheManager.getCachedTurnos(getApplication())
            if (cachedTurnos.isNotEmpty()) {
                updateTurnosUI(cachedTurnos)
            }
            val cachedMeds = OfflineCacheManager.getCachedMedicamentos(getApplication())
            if (cachedMeds.isNotEmpty()) {
                _medicamentos.value = cachedMeds
            }
        }
    }

    fun refreshData() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 1. Carga desde Servidor
                val turnos = turnoRepository.getTurnos()
                updateTurnosUI(turnos)
                OfflineCacheManager.saveTurnos(getApplication(), turnos)

                val meds = medRepository.getMedicamentos()
                _medicamentos.value = meds
                OfflineCacheManager.saveMedicamentos(getApplication(), meds)

            } catch (e: Exception) {
                if (refreshJob?.isActive == true) {
                    val msg = e.localizedMessage ?: ""
                    _errorMessage.value = if (msg.contains("resolve host", true) || msg.contains("connect", true)) {
                        getApplication<Application>().getString(R.string.msg_no_connection)
                    } else {
                        msg.ifBlank { getApplication<Application>().getString(R.string.msg_error_unknown) }
                    }
                    
                    // Si falló el servidor, nos aseguramos de que al menos la caché sea visible
                    if (_nextTurno.value == null && _medicamentos.value.isNullOrEmpty()) {
                        loadFromCache()
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateTurnosUI(turnos: List<Turno>) {
        _turnosCount.value = turnos.size
        _nextTurno.value = turnos.filter { it.estado.lowercase() in listOf("pendiente", "pending") }
            .sortedBy { "${it.fecha} ${it.hora}" }
            .firstOrNull()
    }

    fun syncAll() {
        viewModelScope.launch {
            // Sincronizar Turnos
            val pendingTurnos = OfflineCacheManager.getPendingTurnos(getApplication())
            if (pendingTurnos.isNotEmpty()) {
                val syncedTurnos = mutableListOf<NuevoTurnoRequest>()
                pendingTurnos.forEach { 
                    try { 
                        turnoRepository.crearTurno(it)
                        syncedTurnos.add(it)
                    } catch (_: Exception) {} 
                }
                if (syncedTurnos.isNotEmpty()) {
                    OfflineCacheManager.removePendingTurnos(getApplication(), syncedTurnos)
                }
            }

            // Sincronizar Meds
            val pendingMeds = OfflineCacheManager.getPendingMeds(getApplication())
            if (pendingMeds.isNotEmpty()) {
                val syncedMeds = mutableListOf<Medicamento>()
                pendingMeds.forEach { 
                    try { 
                        medRepository.agregarMedicamento(it)
                        syncedMeds.add(it)
                    } catch (_: Exception) {} 
                }
                if (syncedMeds.isNotEmpty()) {
                    OfflineCacheManager.removePendingMeds(getApplication(), syncedMeds)
                }
            }

            refreshData()
        }
    }

    private fun loadRandomHealthTip() {
        val tips = listOf(
            R.string.tip_health_1,
            R.string.tip_health_2,
            R.string.tip_health_3,
            R.string.tip_health_4,
            R.string.tip_health_5,
            R.string.tip_health_6,
            R.string.tip_health_7,
            R.string.tip_health_8,
            R.string.tip_health_9,
            R.string.tip_health_10
        )
        // Usamos el día del año para que la recomendación sea realmente "del día"
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        _healthTipResId.value = tips[dayOfYear % tips.size]
    }
}

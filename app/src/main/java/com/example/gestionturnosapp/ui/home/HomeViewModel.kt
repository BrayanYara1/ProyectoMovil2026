package com.example.gestionturnosapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.TurnoRepository
import com.example.gestionturnosapp.data.MedicamentoRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val turnoRepository = TurnoRepository()
    private val medRepository = MedicamentoRepository()

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

    init {
        loadRandomHealthTip()
    }

    fun refreshData(context: android.content.Context? = null) {
        loadRandomHealthTip()
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // 1. Carga desde Caché (Respuesta instantánea)
            context?.let {
                val cachedTurnos = com.example.gestionturnosapp.data.OfflineCacheManager.getCachedTurnos(it)
                if (cachedTurnos.isNotEmpty()) {
                    updateTurnosUI(cachedTurnos)
                }
                val cachedMeds = com.example.gestionturnosapp.data.OfflineCacheManager.getCachedMedicamentos(it)
                if (cachedMeds.isNotEmpty()) {
                    _medicamentos.value = cachedMeds
                }
            }

            try {
                // 2. Carga desde Servidor
                val turnos = turnoRepository.getTurnos()
                updateTurnosUI(turnos)
                context?.let { com.example.gestionturnosapp.data.OfflineCacheManager.saveTurnos(it, turnos) }

                val meds = medRepository.getMedicamentos()
                _medicamentos.value = meds
                context?.let { com.example.gestionturnosapp.data.OfflineCacheManager.saveMedicamentos(it, meds) }

            } catch (e: Exception) {
                // Si no hay nada en LiveData aún, mostramos error
                if (_nextTurno.value == null && _medicamentos.value.isNullOrEmpty()) {
                    _turnosCount.value = 0
                    _nextTurno.value = null
                    _medicamentos.value = emptyList()
                    _errorMessage.value = e.localizedMessage ?: "Error al conectar con el servidor"
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

    fun refreshTurnosCount(context: android.content.Context) = refreshData(context)

    fun syncAll(context: android.content.Context) {
        viewModelScope.launch {
            // Sincronizar Turnos
            val pendingTurnos = com.example.gestionturnosapp.data.OfflineCacheManager.getPendingTurnos(context)
            if (pendingTurnos.isNotEmpty()) {
                pendingTurnos.forEach { try { turnoRepository.crearTurno(it) } catch (e: Exception) {} }
                com.example.gestionturnosapp.data.OfflineCacheManager.clearPendingTurnos(context)
            }

            // Sincronizar Meds
            val pendingMeds = com.example.gestionturnosapp.data.OfflineCacheManager.getPendingMeds(context)
            if (pendingMeds.isNotEmpty()) {
                pendingMeds.forEach { try { medRepository.agregarMedicamento(it) } catch (e: Exception) {} }
                com.example.gestionturnosapp.data.OfflineCacheManager.clearPendingMeds(context)
            }

            refreshData(context)
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
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        _healthTipResId.value = tips[dayOfYear % tips.size]
    }
}

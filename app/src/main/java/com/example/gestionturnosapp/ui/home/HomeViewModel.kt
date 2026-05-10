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

    init {
        loadRandomHealthTip()
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cargar Turnos
                val turnos = turnoRepository.getTurnos()
                _turnosCount.value = turnos.size
                
                _nextTurno.value = turnos.filter { it.estado.lowercase() in listOf("pendiente", "pending") }
                    .sortedBy { "${it.fecha} ${it.hora}" }
                    .firstOrNull()

                // Cargar Medicamentos
                val meds = medRepository.getMedicamentos()
                _medicamentos.value = meds

            } catch (e: Exception) {
                _turnosCount.value = 0
                _nextTurno.value = null
                _medicamentos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshTurnosCount() = refreshData()

    private fun loadRandomHealthTip() {
        val tips = listOf(
            R.string.tip_health_1,
            R.string.tip_health_2,
            R.string.tip_health_3,
            R.string.tip_health_4,
            R.string.tip_health_5
        )
        _healthTipResId.value = tips.random()
    }
}

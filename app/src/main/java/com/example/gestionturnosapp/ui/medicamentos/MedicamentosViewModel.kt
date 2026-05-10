package com.example.gestionturnosapp.ui.medicamentos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.MedicamentoRepository
import kotlinx.coroutines.launch

class MedicamentosViewModel : ViewModel() {

    private val repository = MedicamentoRepository()

    private val _medicamentos = MutableLiveData<List<Medicamento>>()
    val medicamentos: LiveData<List<Medicamento>> = _medicamentos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    init {
        loadMedicamentos()
    }

    fun loadMedicamentos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.getMedicamentos()
                _medicamentos.value = list
            } catch (e: Exception) {
                _medicamentos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun agregarMedicamento(nombre: String, dosis: String, frecuencia: String, proximaToma: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val nuevoMed = Medicamento(
                    id = "", // El backend debería generar el ID
                    nombre = nombre,
                    dosis = dosis,
                    frecuencia = frecuencia,
                    proximaToma = proximaToma
                )
                val result = repository.agregarMedicamento(nuevoMed)
                if (result != null) {
                    _operationSuccess.value = true
                    loadMedicamentos()
                } else {
                    _operationSuccess.value = false
                }
            } catch (e: Exception) {
                _operationSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

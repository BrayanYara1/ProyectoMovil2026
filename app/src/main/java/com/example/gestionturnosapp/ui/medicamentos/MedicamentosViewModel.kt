package com.example.gestionturnosapp.ui.medicamentos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.MedicamentoRepository
import com.example.gestionturnosapp.data.Resource
import kotlinx.coroutines.launch

class MedicamentosViewModel : ViewModel() {

    private val repository = MedicamentoRepository()

    private val _medicamentosResource = MutableLiveData<Resource<List<Medicamento>>>()
    val medicamentosResource: LiveData<Resource<List<Medicamento>>> = _medicamentosResource

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResource = MutableLiveData<Resource<Medicamento>>(Resource.Idle)
    val operationResource: LiveData<Resource<Medicamento>> = _operationResource

    init {
        loadMedicamentos()
    }

    fun loadMedicamentos() {
        viewModelScope.launch {
            _medicamentosResource.value = Resource.Loading
            _isLoading.value = true
            try {
                val list = repository.getMedicamentos()
                _medicamentosResource.value = Resource.Success(list)
            } catch (e: Exception) {
                _medicamentosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun agregarMedicamento(nombre: String, dosis: String, frecuencia: String, proximaToma: String) {
        viewModelScope.launch {
            _operationResource.value = Resource.Loading
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
                    _operationResource.value = Resource.Success(result)
                    loadMedicamentos()
                } else {
                    _operationResource.value = Resource.Error("Error al guardar")
                }
            } catch (e: Exception) {
                _operationResource.value = Resource.Error(e.localizedMessage ?: "Error de red")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetOperationState() {
        _operationResource.value = Resource.Idle
    }
}

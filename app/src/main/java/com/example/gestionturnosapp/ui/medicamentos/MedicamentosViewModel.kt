package com.example.gestionturnosapp.ui.medicamentos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.MedicamentoRepository
import com.example.gestionturnosapp.data.OfflineCacheManager
import com.example.gestionturnosapp.data.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicamentosViewModel @Inject constructor(
    application: Application,
    private val repository: MedicamentoRepository,
) : AndroidViewModel(application) {

    private val _medicamentosResource = MutableLiveData<Resource<List<Medicamento>>>()
    val medicamentosResource: LiveData<Resource<List<Medicamento>>> = _medicamentosResource

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResource = MutableLiveData<Resource<Medicamento>>(Resource.Idle)
    val operationResource: LiveData<Resource<Medicamento>> = _operationResource

    fun loadMedicamentos() {
        viewModelScope.launch {
            _medicamentosResource.value = Resource.Loading
            _isLoading.value = true
            
            // Offline Cache First
            val cached = OfflineCacheManager.getCachedMedicamentos(getApplication())
            if (cached.isNotEmpty()) {
                _medicamentosResource.value = Resource.Success(cached)
            }

            try {
                val list = repository.getMedicamentos()
                _medicamentosResource.value = Resource.Success(list)
                
                // Save to Cache
                OfflineCacheManager.saveMedicamentos(getApplication(), list)
            } catch (e: Exception) {
                if (_medicamentosResource.value !is Resource.Success) {
                    _medicamentosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun agregarMedicamento(nombre: String, dosis: String, frecuencia: String, proximaToma: String) {
        viewModelScope.launch {
            _operationResource.value = Resource.Loading
            _isLoading.value = true
            val nuevoMed = Medicamento(
                id = "", 
                nombre = nombre,
                dosis = dosis,
                frecuencia = frecuencia,
                proximaToma = proximaToma
            )
            try {
                val result = repository.agregarMedicamento(nuevoMed)
                if (result != null) {
                    _operationResource.value = Resource.Success(result)
                    loadMedicamentos()
                } else {
                    _operationResource.value = Resource.Error("Error al guardar")
                }
            } catch (e: Exception) {
                if (OfflineCacheManager.isNetworkError(e)) {
                    OfflineCacheManager.addPendingMed(getApplication(), nuevoMed)
                    _operationResource.value = Resource.Success(nuevoMed.copy(id = "pending"))
                    loadMedicamentos()
                } else {
                    _operationResource.value = Resource.Error(e.localizedMessage ?: "Error de red")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncPendingMeds() {
        val pending = OfflineCacheManager.getPendingMeds(getApplication())
        if (pending.isEmpty()) return

        viewModelScope.launch {
            val synced = mutableListOf<Medicamento>()
            pending.forEach { med ->
                try {
                    repository.agregarMedicamento(med)
                    synced.add(med)
                } catch (_: Exception) {
                    return@forEach
                }
            }
            if (synced.isNotEmpty()) {
                OfflineCacheManager.removePendingMeds(getApplication(), synced)
                loadMedicamentos()
            }
        }
    }

    fun resetOperationState() {
        _operationResource.value = Resource.Idle
    }

    fun eliminarMedicamento(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.eliminarMedicamento(id)
                loadMedicamentos()
            } catch (e: Exception) {
                _medicamentosResource.value = Resource.Error(e.localizedMessage ?: "Error al eliminar")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

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
            // Generar un ID local único para evitar colisiones en la DB
            val tempId = "local_${System.currentTimeMillis()}"
            val nuevoMed = Medicamento(
                id = tempId,
                nombre = nombre,
                dosis = dosis,
                frecuencia = frecuencia,
                proximaToma = proximaToma
            )
            try {
                val result = repository.agregarMedicamento(nuevoMed)
                if (result != null) {
                    _operationResource.value = Resource.Success(result)
                    
                    // Actualizar lista local inmediatamente para feedback instantáneo
                    val currentList = (_medicamentosResource.value as? Resource.Success)?.data?.toMutableList() ?: mutableListOf()
                    currentList.add(0, result)
                    _medicamentosResource.value = Resource.Success(currentList)
                    
                    // Sincronizar cache
                    OfflineCacheManager.saveMedicamentos(getApplication(), currentList)
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
                
                // Actualizar lista local inmediatamente
                val currentList = (_medicamentosResource.value as? Resource.Success)?.data?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.id == id }
                _medicamentosResource.value = Resource.Success(currentList)
                
                // Sincronizar cache
                OfflineCacheManager.saveMedicamentos(getApplication(), currentList)

            } catch (e: Exception) {
                _medicamentosResource.value = Resource.Error(e.localizedMessage ?: "Error al eliminar")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

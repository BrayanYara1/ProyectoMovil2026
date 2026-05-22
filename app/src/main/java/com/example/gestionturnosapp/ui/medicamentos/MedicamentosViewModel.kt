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
            
            // Offline First: Combinar Room + Pendientes
            refreshLocalList()

            try {
                val list = repository.getMedicamentos()
                OfflineCacheManager.saveMedicamentos(getApplication(), list)
                refreshLocalList()
            } catch (e: Exception) {
                if (_medicamentosResource.value !is Resource.Success) {
                    _medicamentosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshLocalList() {
        val cached = OfflineCacheManager.getCachedMedicamentos(getApplication())
        val pending = OfflineCacheManager.getPendingMeds(getApplication())
        // Combinar y eliminar duplicados por nombre
        val combined = (cached + pending).distinctBy { it.nombre.lowercase().trim() }
        _medicamentosResource.postValue(Resource.Success(combined))
    }

    fun agregarMedicamento(nombre: String, dosis: String, frecuencia: String, proximaToma: String) {
        viewModelScope.launch {
            _operationResource.value = Resource.Loading
            _isLoading.value = true
            
            // 1. Crear el objeto local con ID único
            val tempId = "local_${System.currentTimeMillis()}"
            val nuevoMed = Medicamento(
                id = tempId, 
                nombre = nombre,
                dosis = dosis,
                frecuencia = frecuencia,
                proximaToma = proximaToma
            )

            try {
                // 2. Intentar guardar en el servidor
                val result = repository.agregarMedicamento(nuevoMed)
                if (result != null) {
                    _operationResource.value = Resource.Success(result)
                    
                    // Actualizar lista local inmediatamente (Servidor ganó)
                    val currentList = OfflineCacheManager.getCachedMedicamentos(getApplication()).toMutableList()
                    currentList.add(0, result)
                    _medicamentosResource.value = Resource.Success(currentList)
                    OfflineCacheManager.saveMedicamentos(getApplication(), currentList)
                } else {
                    _operationResource.value = Resource.Error("Error de respuesta")
                }
            } catch (e: Exception) {
                // 3. Fallo de red: Guardar como pendiente
                if (OfflineCacheManager.isNetworkError(e)) {
                    OfflineCacheManager.addPendingMed(getApplication(), nuevoMed)
                    
                    // Mostrar en la lista local de inmediato como "Success" local
                    val currentList = OfflineCacheManager.getCachedMedicamentos(getApplication()).toMutableList()
                    currentList.add(0, nuevoMed)
                    _medicamentosResource.value = Resource.Success(currentList)
                    
                    _operationResource.value = Resource.Success(nuevoMed)
                } else {
                    _operationResource.value = Resource.Error(e.localizedMessage ?: "Error")
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

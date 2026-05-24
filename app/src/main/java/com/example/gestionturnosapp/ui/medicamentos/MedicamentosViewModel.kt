package com.example.gestionturnosapp.ui.medicamentos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.data.model.Medicamento
import com.example.gestionturnosapp.data.remote.dto.NuevoMedicamentoRequest
import com.example.gestionturnosapp.data.repository.MedicamentoRepository
import com.example.gestionturnosapp.data.local.OfflineCacheManager
import com.example.gestionturnosapp.util.Resource
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
            
            // 1. Mostrar lo que tenemos localmente ya
            refreshLocalList()

            try {
                // 2. El Repo intenta Server -> Cache
                val list = repository.getMedicamentos()
                
                // 3. Volver a mostrar incluyendo pendientes
                val pending = repository.getPendingMeds()
                val combined = (list + pending).distinctBy { it.nombre.lowercase().trim() }
                _medicamentosResource.value = Resource.Success(combined)
            } catch (e: Exception) {
                if (_medicamentosResource.value !is Resource.Success<*>) {
                    _medicamentosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshLocalList() {
        val cached = repository.getMedicamentos() // Esto ahora lee de cache si falla el repo
        _medicamentosResource.postValue(Resource.Success(cached))
    }

    fun agregarMedicamento(nombre: String, dosis: String, frecuencia: String, proximaToma: String) {
        viewModelScope.launch {
            _operationResource.value = Resource.Loading
            _isLoading.value = true
            
            try {
                val request = NuevoMedicamentoRequest(nombre, dosis, frecuencia, proximaToma)
                val result = repository.agregarMedicamento(request)
                if (result != null) {
                    _operationResource.value = Resource.Success(result)
                    loadMedicamentos()
                }
            } catch (e: Exception) {
                _operationResource.value = Resource.Error(e.localizedMessage ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncPendingMeds() {
        viewModelScope.launch {
            val pending = repository.getPendingMeds()
            if (pending.isEmpty()) return@launch

            val synced = mutableListOf<Medicamento>()
            pending.forEach { med ->
                try {
                    val request = NuevoMedicamentoRequest(med.nombre, med.dosis, med.frecuencia, med.proximaToma, med.notas)
                    repository.agregarMedicamento(request)
                    synced.add(med)
                } catch (_: Exception) {}
            }
            if (synced.isNotEmpty()) {
                repository.removePendingMeds(synced)
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
                val currentList = (_medicamentosResource.value as? Resource.Success<List<Medicamento>>)?.data?.toMutableList() ?: mutableListOf()
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

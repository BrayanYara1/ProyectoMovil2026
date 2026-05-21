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
        // Se carga desde el Fragment con contexto para usar caché
    }

    fun loadMedicamentos(context: android.content.Context? = null) {
        viewModelScope.launch {
            _medicamentosResource.value = Resource.Loading
            _isLoading.value = true
            
            // Offline Cache First
            context?.let {
                val cached = com.example.gestionturnosapp.data.OfflineCacheManager.getCachedMedicamentos(it)
                if (cached.isNotEmpty()) {
                    _medicamentosResource.value = Resource.Success(cached)
                }
            }

            try {
                val list = repository.getMedicamentos()
                _medicamentosResource.value = Resource.Success(list)
                
                // Save to Cache
                context?.let { com.example.gestionturnosapp.data.OfflineCacheManager.saveMedicamentos(it, list) }
            } catch (e: Exception) {
                if (_medicamentosResource.value !is Resource.Success) {
                    _medicamentosResource.value = Resource.Error(e.localizedMessage ?: "Error desconocido")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun agregarMedicamento(context: android.content.Context, nombre: String, dosis: String, frecuencia: String, proximaToma: String) {
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
                    loadMedicamentos(context)
                } else {
                    _operationResource.value = Resource.Error("Error al guardar")
                }
            } catch (e: Exception) {
                if (com.example.gestionturnosapp.data.OfflineCacheManager.isNetworkError(e)) {
                    com.example.gestionturnosapp.data.OfflineCacheManager.addPendingMed(context, nuevoMed)
                    _operationResource.value = Resource.Success(nuevoMed.copy(id = "pending"))
                    loadMedicamentos(context)
                } else {
                    _operationResource.value = Resource.Error(e.localizedMessage ?: "Error de red")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncPendingMeds(context: android.content.Context) {
        val pending = com.example.gestionturnosapp.data.OfflineCacheManager.getPendingMeds(context)
        if (pending.isEmpty()) return

        viewModelScope.launch {
            pending.forEach { med ->
                try {
                    repository.agregarMedicamento(med)
                } catch (e: Exception) { return@launch }
            }
            com.example.gestionturnosapp.data.OfflineCacheManager.clearPendingMeds(context)
            loadMedicamentos(context)
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

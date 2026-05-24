package com.example.gestionturnosapp.ui.especialidades

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.gestionturnosapp.data.model.Especialidad
import com.example.gestionturnosapp.data.repository.EspecialidadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EspecialidadesViewModel @Inject constructor(
    application: Application,
    private val repository: EspecialidadRepository,
) : AndroidViewModel(application) {
    private val _allEspecialidades = MutableLiveData<List<Especialidad>>()
    private val _searchQuery = MutableLiveData("")
    private val _localeTrigger = MutableLiveData<Long>(System.currentTimeMillis())

    val filteredEspecialidades = MediatorLiveData<List<Especialidad>>().apply {
        val observer = { _: Any? ->
            val list = _allEspecialidades.value ?: emptyList()
            val query = _searchQuery.value?.lowercase(Locale.getDefault()) ?: ""
            
            value = if (query.isEmpty()) {
                list
            } else {
                // Usamos el locale actual para las comparaciones de texto
                val context = getApplication<Application>().applicationContext
                list.filter { especialidad ->
                    val name = context.getString(especialidad.nombreRes).lowercase(Locale.getDefault())
                    val desc = context.getString(especialidad.descripcionRes).lowercase(Locale.getDefault())
                    
                    name.contains(query) || desc.contains(query)
                }
            }
        }
        addSource(_allEspecialidades, observer)
        addSource(_searchQuery, observer)
        addSource(_localeTrigger, observer)
    }

    init {
        loadEspecialidades()
    }

    fun loadEspecialidades() {
        _allEspecialidades.value = repository.getEspecialidades()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshForLocale() {
        _localeTrigger.value = System.currentTimeMillis()
    }
}

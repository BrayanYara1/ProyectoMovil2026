package com.example.gestionturnosapp.ui.especialidades

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.gestionturnosapp.data.Especialidad
import com.example.gestionturnosapp.data.EspecialidadRepository
import java.util.Locale

class EspecialidadesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EspecialidadRepository()
    private val _allEspecialidades = MutableLiveData<List<Especialidad>>()
    private val _searchQuery = MutableLiveData<String>("")

    val filteredEspecialidades = MediatorLiveData<List<Especialidad>>().apply {
        val observer = { _: Any? ->
            val list = _allEspecialidades.value ?: emptyList()
            val query = _searchQuery.value?.lowercase(Locale.getDefault()) ?: ""
            
            if (query.isEmpty()) {
                value = list
            } else {
                val context = getApplication<Application>().applicationContext
                value = list.filter { especialidad ->
                    val name = context.getString(especialidad.nombreRes).lowercase(Locale.getDefault())
                    val desc = context.getString(especialidad.descripcionRes).lowercase(Locale.getDefault())
                    
                    // Búsqueda robusta: coincide con nombre o descripción
                    name.contains(query) || desc.contains(query)
                }
            }
        }
        addSource(_allEspecialidades, observer)
        addSource(_searchQuery, observer)
    }

    init {
        loadEspecialidades()
    }

    private fun loadEspecialidades() {
        _allEspecialidades.value = repository.getEspecialidades()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

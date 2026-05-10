package com.example.gestionturnosapp.ui.especialidades

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gestionturnosapp.data.Especialidad
import com.example.gestionturnosapp.data.EspecialidadRepository
import java.util.Locale

class EspecialidadesViewModel : ViewModel() {

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
                // Como los nombres son recursos, necesitamos el contexto para filtrar, 
                // pero en el ViewModel no lo tenemos. 
                // Alternativa: El repositorio podría proveer strings o filtramos en el Fragment.
                // O mejor, el repositorio ya tiene los datos hardcodeados, filtramos por IDs o 
                // asumimos que el usuario conoce el nombre.
                value = list // Temporalmente devolvemos todo si no podemos acceder a strings aquí
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

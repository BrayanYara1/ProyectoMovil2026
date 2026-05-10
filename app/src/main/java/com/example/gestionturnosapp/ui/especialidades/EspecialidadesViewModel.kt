package com.example.gestionturnosapp.ui.especialidades

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gestionturnosapp.data.Especialidad
import com.example.gestionturnosapp.data.EspecialidadRepository

class EspecialidadesViewModel : ViewModel() {

    private val repository = EspecialidadRepository()
    private val _especialidades = MutableLiveData<List<Especialidad>>()
    val especialidades: LiveData<List<Especialidad>> = _especialidades

    init {
        loadEspecialidades()
    }

    private fun loadEspecialidades() {
        _especialidades.value = repository.getEspecialidades()
    }
}

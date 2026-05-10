package com.example.gestionturnosapp.data

import com.example.gestionturnosapp.R

class EspecialidadRepository {
    fun getEspecialidades(): List<Especialidad> {
        return listOf(
            Especialidad(1, R.string.name_cardiology, R.string.desc_cardiology, R.drawable.ic_medical_logo),
            Especialidad(2, R.string.name_pediatrics, R.string.desc_pediatrics, R.drawable.ic_medical_logo),
            Especialidad(3, R.string.name_traumatology, R.string.desc_traumatology, R.drawable.ic_medical_logo),
            Especialidad(4, R.string.name_dermatology, R.string.desc_dermatology, R.drawable.ic_medical_logo),
            Especialidad(5, R.string.name_neurology, R.string.desc_neurology, R.drawable.ic_medical_logo)
        )
    }
}

package com.example.gestionturnosapp.data

data class NuevoMedicamentoRequest(
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val proximaToma: String,
    val notas: String? = ""
)

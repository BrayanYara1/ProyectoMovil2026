package com.example.gestionturnosapp.data

data class Medicamento(
    val id: String,
    val nombre: String,
    val dosis: String,
    val frecuencia: String, // ej: "Cada 8 horas"
    val proximaToma: String,
    val notas: String? = null
)

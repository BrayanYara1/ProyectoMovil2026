package com.example.gestionturnosapp.data

data class NuevoTurnoRequest(
    val nombre: String,
    val fecha: String,
    val hora: String,
    val motivo: String,
    val especialidad: String? = "General",
    val doctor: String? = "Dr. Asignado"
)

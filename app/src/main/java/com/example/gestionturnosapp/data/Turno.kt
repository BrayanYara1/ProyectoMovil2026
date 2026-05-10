package com.example.gestionturnosapp.data

import com.google.gson.annotations.SerializedName

data class Turno(
    @SerializedName("_id", alternate = ["id"])
    val id: String,
    val pacienteNombre: String,
    val fecha: String,
    val hora: String,
    val motivo: String,
    val estado: String = "Pendiente",
    val especialidad: String? = "General",
    val doctor: String? = "Dr. Asignado"
)

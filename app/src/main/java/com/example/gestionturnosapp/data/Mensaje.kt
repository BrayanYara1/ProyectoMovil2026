package com.example.gestionturnosapp.data

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Mensaje(
    @SerializedName("_id") val id: String = "",
    val remitente: String, // "PACIENTE" o "DOCTOR"
    val texto: String,
    val fecha: Date = Date(),
    val leido: Boolean = false
)

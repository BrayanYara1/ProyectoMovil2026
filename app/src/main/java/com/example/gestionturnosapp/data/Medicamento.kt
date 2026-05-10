package com.example.gestionturnosapp.data

import com.google.gson.annotations.SerializedName

data class Medicamento(
    @SerializedName("_id", alternate = ["id"])
    val id: String,
    val nombre: String,
    val dosis: String,
    val frecuencia: String, // ej: "Cada 8 horas"
    val proximaToma: String,
    val notas: String? = null
)

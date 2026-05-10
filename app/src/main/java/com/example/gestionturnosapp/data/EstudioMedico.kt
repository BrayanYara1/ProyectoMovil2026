package com.example.gestionturnosapp.data

import com.google.gson.annotations.SerializedName

data class EstudioMedico(
    @SerializedName("_id", alternate = ["id"])
    val id: String,
    val titulo: String,
    val fecha: String,
    val tipo: String, // ej: "Análisis de Sangre", "Radiografía"
    val resultadoBreve: String,
    val urlDocumento: String? = null,
    val notas: String? = null
)

package com.example.gestionturnosapp.data.model

import com.google.gson.annotations.SerializedName

data class Usuario(
    @SerializedName("_id", alternate = ["id"])
    val id: String,
    val nombre: String,
    val email: String,
    val telefono: String? = null
)

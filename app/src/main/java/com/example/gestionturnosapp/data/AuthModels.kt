package com.example.gestionturnosapp.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val contrasena: String
)

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val telefono: String,
    val contrasena: String
)

data class AuthResponse(
    val mensaje: String,
    val usuario: Usuario? = null,
    val token: String? = null,
    val requiresVerification: Boolean = false,
    val email: String? = null
)

data class VerifyRequest(
    val email: String,
    val code: String
)

data class Usuario(
    @SerializedName("_id", alternate = ["id"])
    val id: String,
    val nombre: String,
    val email: String,
    val telefono: String? = null
)

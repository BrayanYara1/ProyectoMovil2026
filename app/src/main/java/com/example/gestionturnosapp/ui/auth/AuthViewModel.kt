package com.example.gestionturnosapp.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.data.*
import com.example.gestionturnosapp.network.RetrofitClient
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authState = MutableLiveData<Resource<Usuario>>(Resource.Idle)
    val authState: LiveData<Resource<Usuario>> = _authState

    fun login(email: String, contrasena: String, context: Context) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, contrasena))
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val usuario = authResponse?.usuario
                    if (usuario != null) {
                        UserManager.saveUser(context, usuario, authResponse.token)
                        _authState.value = Resource.Success(usuario)
                    } else {
                        _authState.value = Resource.Error("Error: Usuario no encontrado")
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Correo o contraseña incorrectos"
                        404 -> "Usuario no encontrado"
                        else -> "Error del servidor: ${response.code()}"
                    }
                    _authState.value = Resource.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Error de red"
                val displayMsg = when {
                    errorMsg.contains("connect", true) -> "No se pudo conectar al servidor. Verifica tu internet."
                    errorMsg.contains("timeout", true) -> "El servidor tardó mucho en responder."
                    else -> "Error: $errorMsg"
                }
                _authState.value = Resource.Error(displayMsg)
            }
        }
    }

    fun register(request: RegisterRequest, context: Context) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = RetrofitClient.instance.register(request)
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val usuario = authResponse?.usuario
                    if (usuario != null) {
                        UserManager.saveUser(context, usuario, authResponse.token)
                        _authState.value = Resource.Success(usuario)
                    } else {
                        _authState.value = Resource.Error("Error al registrar: Respuesta vacía")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val displayMsg = when {
                        errorBody.contains("email", true) -> "Este correo ya está registrado"
                        errorBody.contains("password", true) -> "Contraseña demasiado débil"
                        else -> "Error: $errorBody"
                    }
                    _authState.value = Resource.Error(displayMsg)
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(e.localizedMessage ?: "Error de conexión")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = Resource.Idle
    }
}

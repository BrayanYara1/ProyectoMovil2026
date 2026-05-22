package com.example.gestionturnosapp.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.*
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    
    private val _authState = MutableLiveData<Resource<Usuario>>(Resource.Idle)
    val authState: LiveData<Resource<Usuario>> = _authState

    fun login(email: String, contrasena: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = repository.login(LoginRequest(email, contrasena))
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val usuario = authResponse?.usuario
                    if (usuario != null) {
                        UserManager.saveUser(getApplication(), usuario, authResponse.token)
                        _authState.value = Resource.Success(usuario)
                    } else {
                        _authState.value = Resource.Error(getApplication<Application>().getString(R.string.msg_user_not_found))
                    }
                } else {
                    val code = response.code()
                    val errorMsg = when (code) {
                        401 -> getApplication<Application>().getString(R.string.msg_login_error)
                        404 -> getApplication<Application>().getString(R.string.msg_user_not_found)
                        else -> repository.getErrorMessage(response)
                    }
                    _authState.value = Resource.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(handleException(e))
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful) {
                    _authState.value = Resource.Success(Usuario("", request.nombre, request.email, request.telefono))
                } else {
                    val errorMsg = repository.getErrorMessage(response)
                    val displayMsg = when {
                        errorMsg.contains("email", true) || errorMsg.contains("correo", true) -> getApplication<Application>().getString(R.string.msg_email_already_registered)
                        errorMsg.contains("password", true) || errorMsg.contains("contraseña", true) -> getApplication<Application>().getString(R.string.msg_password_weak)
                        else -> errorMsg
                    }
                    _authState.value = Resource.Error(displayMsg)
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(handleException(e))
            }
        }
    }

    private fun handleException(e: Exception): String {
        android.util.Log.e("AuthViewModel", "Error en autenticación", e)
        val errorMsg = e.localizedMessage ?: ""
        return when {
            errorMsg.contains("resolve host", true) || errorMsg.contains("connect", true) -> 
                getApplication<Application>().getString(R.string.msg_no_connection)
            errorMsg.contains("timeout", true) -> 
                getApplication<Application>().getString(R.string.msg_timeout)
            else -> getApplication<Application>().getString(R.string.msg_server_error, errorMsg.ifBlank { "Error desconocido" })
        }
    }

    fun resetAuthState() {
        _authState.value = Resource.Idle
    }
}

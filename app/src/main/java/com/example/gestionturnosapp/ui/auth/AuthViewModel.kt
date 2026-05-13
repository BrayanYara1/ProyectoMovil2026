package com.example.gestionturnosapp.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.*
import com.example.gestionturnosapp.network.RetrofitClient
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authState = MutableLiveData<Resource<Usuario>>(Resource.Idle)
    val authState: LiveData<Resource<Usuario>> = _authState

    fun login(email: String, contrasena: String, context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, contrasena))
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val usuario = authResponse?.usuario
                    if (usuario != null) {
                        UserManager.saveUser(appContext, usuario, authResponse.token)
                        _authState.value = Resource.Success(usuario)
                    } else {
                        _authState.value = Resource.Error(context.getString(R.string.msg_user_not_found))
                    }
                } else {
                    val code = response.code()
                    val errorMsg = when (code) {
                        401 -> context.getString(R.string.msg_login_error)
                        404 -> context.getString(R.string.msg_user_not_found)
                        else -> {
                            val errorBody = response.errorBody()?.string() ?: ""
                            if (errorBody.contains("mensaje")) {
                                errorBody.substringAfter("\"mensaje\":\"").substringBefore("\"")
                            } else {
                                context.getString(R.string.msg_server_error, code.toString())
                            }
                        }
                    }
                    _authState.value = Resource.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(handleException(e, context))
            }
        }
    }

    fun register(request: RegisterRequest, context: Context) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = RetrofitClient.instance.register(request)
                if (response.isSuccessful) {
                    _authState.value = Resource.Success(Usuario("", request.nombre, request.email, request.telefono))
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val displayMsg = when {
                        errorBody.contains("email", true) || errorBody.contains("correo", true) -> context.getString(R.string.msg_email_already_registered)
                        errorBody.contains("password", true) || errorBody.contains("contraseña", true) -> context.getString(R.string.msg_password_weak)
                        errorBody.contains("mensaje") -> errorBody.substringAfter("\"mensaje\":\"").substringBefore("\"")
                        else -> context.getString(R.string.msg_server_error, response.code().toString())
                    }
                    _authState.value = Resource.Error(displayMsg)
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(handleException(e, context))
            }
        }
    }

    private fun handleException(e: Exception, context: Context): String {
        android.util.Log.e("AuthViewModel", "Error en autenticación", e)
        val errorMsg = e.localizedMessage ?: ""
        return when {
            errorMsg.contains("connect", true) -> context.getString(R.string.msg_no_connection)
            errorMsg.contains("timeout", true) -> context.getString(R.string.msg_timeout)
            else -> context.getString(R.string.msg_server_error, errorMsg.ifBlank { "Error desconocido" })
        }
    }

    fun resetAuthState() {
        _authState.value = Resource.Idle
    }
}

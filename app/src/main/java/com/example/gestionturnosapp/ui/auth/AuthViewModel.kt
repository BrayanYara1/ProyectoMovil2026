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
                    val errorMsg = when (response.code()) {
                        401 -> context.getString(R.string.msg_login_error)
                        404 -> context.getString(R.string.msg_user_not_found)
                        else -> context.getString(R.string.msg_server_error, response.code().toString())
                    }
                    _authState.value = Resource.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: context.getString(R.string.error_connection)
                val displayMsg = when {
                    errorMsg.contains("connect", true) -> context.getString(R.string.msg_no_connection)
                    errorMsg.contains("timeout", true) -> context.getString(R.string.msg_timeout)
                    else -> context.getString(R.string.msg_server_error, errorMsg)
                }
                _authState.value = Resource.Error(displayMsg)
            }
        }
    }

    fun register(request: RegisterRequest, context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = RetrofitClient.instance.register(request)
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val usuario = authResponse?.usuario
                    if (usuario != null) {
                        UserManager.saveUser(appContext, usuario, authResponse.token)
                        _authState.value = Resource.Success(usuario)
                    } else {
                        _authState.value = Resource.Error(context.getString(R.string.msg_register_empty))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val displayMsg = when {
                        errorBody.contains("email", true) -> context.getString(R.string.msg_email_already_registered)
                        errorBody.contains("password", true) -> context.getString(R.string.msg_password_weak)
                        else -> context.getString(R.string.msg_server_error, errorBody)
                    }
                    _authState.value = Resource.Error(displayMsg)
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(e.localizedMessage ?: context.getString(R.string.error_connection))
            }
        }
    }

    fun resetAuthState() {
        _authState.value = Resource.Idle
    }
}

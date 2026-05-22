package com.example.gestionturnosapp.ui.auth

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repository: AuthRepository,
) : AndroidViewModel(application) {
    
    private val _authState = MutableLiveData<Resource<Usuario>>(Resource.Idle)
    val authState: LiveData<Resource<Usuario>> = _authState

    // CAMPOS REACTIVOS LOGIN
    val loginEmail = MutableLiveData("")
    val loginPassword = MutableLiveData("")

    val isLoginValid = MediatorLiveData<Boolean>().apply {
        val observer = { _: String? ->
            val email = loginEmail.value ?: ""
            val pass = loginPassword.value ?: ""
            value = (Patterns.EMAIL_ADDRESS.matcher(email).matches() && pass.length >= 6)
        }
        addSource(loginEmail, observer)
        addSource(loginPassword, observer)
    }

    // CAMPOS REACTIVOS REGISTRO
    val regName = MutableLiveData("")
    val regEmail = MutableLiveData("")
    val regPhone = MutableLiveData("")
    val regPassword = MutableLiveData("")

    val isRegisterValid = MediatorLiveData<Boolean>().apply {
        val observer = { _: String? ->
            val name = regName.value ?: ""
            val email = regEmail.value ?: ""
            val phone = regPhone.value ?: ""
            val pass = regPassword.value ?: ""
            
            value = name.isNotBlank() && 
                    Patterns.EMAIL_ADDRESS.matcher(email).matches() && 
                    phone.length >= 10 && 
                    pass.length >= 6
        }
        addSource(regName, observer)
        addSource(regEmail, observer)
        addSource(regPhone, observer)
        addSource(regPassword, observer)
    }

    fun login() {
        val email = loginEmail.value ?: return
        val pass = loginPassword.value ?: return
        
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = repository.login(LoginRequest(email, pass))
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
                    val errorMsg = when (response.code()) {
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

    fun register() {
        val name = regName.value ?: return
        val email = regEmail.value ?: return
        val phone = regPhone.value ?: return
        val pass = regPassword.value ?: return
        
        val request = RegisterRequest(name, email, phone, pass)

        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful) {
                    _authState.value = Resource.Success(Usuario("", request.nombre, request.email, request.telefono))
                } else {
                    val errorMsg = repository.getErrorMessage(response)
                    val displayMsg = when {
                        errorMsg.contains(other = "email", ignoreCase = true) || errorMsg.contains(other = "correo", ignoreCase = true) -> getApplication<Application>().getString(R.string.msg_email_already_registered)
                        errorMsg.contains(other = "password", ignoreCase = true) || errorMsg.contains(other = "contraseña", ignoreCase = true) -> getApplication<Application>().getString(R.string.msg_password_weak)
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

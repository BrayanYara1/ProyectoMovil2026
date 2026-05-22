package com.example.gestionturnosapp.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.data.Usuario
import com.example.gestionturnosapp.network.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val _user = MutableLiveData<Usuario?>()
    val user: LiveData<Usuario?> = _user

    private val _updateStatus = MutableLiveData<Resource<Usuario>>(Resource.Idle)
    val updateStatus: LiveData<Resource<Usuario>> = _updateStatus

    init {
        loadUser()
    }

    fun loadUser() {
        _user.value = UserManager.loadUser(getApplication())
    }

    fun updateProfile(nuevoUsuario: Usuario) {
        viewModelScope.launch {
            _updateStatus.value = Resource.Loading
            try {
                val response = RetrofitClient.instance.updateProfile(nuevoUsuario)
                if (response.isSuccessful) {
                    val usuarioActualizado = response.body() ?: nuevoUsuario
                    UserManager.saveUser(getApplication(), usuarioActualizado)
                    _user.value = usuarioActualizado
                    _updateStatus.value = Resource.Success(usuarioActualizado)
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val code = response.code()
                    
                    if (code == 401) {
                        _updateStatus.value = Resource.Error("SESSION_EXPIRED")
                    } else {
                        val msg = if (errorBody.contains("ruta") || errorBody.contains("not exist")) {
                            getApplication<Application>().getString(R.string.msg_server_route_error)
                        } else {
                            "${getApplication<Application>().getString(R.string.msg_generic_sync_error)} ($code)"
                        }
                        // Save locally anyway as fallback
                        UserManager.saveUser(getApplication(), nuevoUsuario)
                        _user.value = nuevoUsuario
                        _updateStatus.value = Resource.Error(msg)
                    }
                }
            } catch (_: Exception) {
                // Offline fallback
                UserManager.saveUser(getApplication(), nuevoUsuario)
                _user.value = nuevoUsuario
                _updateStatus.value = Resource.Error("OFFLINE_SAVED")
            }
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = Resource.Idle
    }
}

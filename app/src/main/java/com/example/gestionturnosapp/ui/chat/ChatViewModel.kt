package com.example.gestionturnosapp.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.data.Mensaje
import com.example.gestionturnosapp.data.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = com.example.gestionturnosapp.data.ChatRepository()

    private val _mensajes = MutableLiveData<Resource<List<Mensaje>>>()
    val mensajes: LiveData<Resource<List<Mensaje>>> = _mensajes

    private val _mensajeEnviado = MutableLiveData<Resource<Mensaje>>()
    val mensajeEnviado: LiveData<Resource<Mensaje>> = _mensajeEnviado

    private val _isDoctorTyping = MutableLiveData<Boolean>(false)
    val isDoctorTyping: LiveData<Boolean> = _isDoctorTyping

    fun fetchMensajes() {
        viewModelScope.launch {
            _mensajes.value = Resource.Loading
            try {
                val list = repository.getMensajes()
                _mensajes.value = Resource.Success(list)
            } catch (e: Exception) {
                _mensajes.value = Resource.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    fun enviarMensaje(texto: String) {
        if (texto.isBlank()) return
        
        viewModelScope.launch {
            _mensajeEnviado.value = Resource.Loading
            try {
                val mensaje = repository.enviarMensaje(texto)
                if (mensaje != null) {
                    _mensajeEnviado.value = Resource.Success(mensaje)
                    
                    // Simular que el doctor está escribiendo
                    _isDoctorTyping.value = true
                    delay(2500)
                    _isDoctorTyping.value = false

                    fetchMensajes()
                } else {
                    _mensajeEnviado.value = Resource.Error("Error al enviar")
                }
            } catch (e: Exception) {
                _mensajeEnviado.value = Resource.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }
}

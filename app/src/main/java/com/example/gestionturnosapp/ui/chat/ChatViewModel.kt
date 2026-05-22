package com.example.gestionturnosapp.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestionturnosapp.data.*
import com.example.gestionturnosapp.util.SmartAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val repository: ChatRepository,
    private val turnoRepository: TurnoRepository,
    private val medRepository: MedicamentoRepository,
) : AndroidViewModel(application) {

    private val _mensajes = MutableLiveData<Resource<List<Mensaje>>>()
    val mensajes: LiveData<Resource<List<Mensaje>>> = _mensajes

    private val _mensajeEnviado = MutableLiveData<Resource<Mensaje>>()
    val mensajeEnviado: LiveData<Resource<Mensaje>> = _mensajeEnviado

    private val _isDoctorTyping = MutableLiveData(false)
    val isDoctorTyping: LiveData<Boolean> = _isDoctorTyping

    // Modo Asistente IA (Por defecto activado para Help)
    private val _isAiAssistantMode = MutableLiveData<Boolean>(true)
    val isAiAssistantMode: LiveData<Boolean> = _isAiAssistantMode

    fun setAiMode(enabled: Boolean) {
        _isAiAssistantMode.value = enabled
        fetchMensajes()
    }

    fun fetchMensajes() {
        if (_isAiAssistantMode.value == true) {
            if (_mensajes.value !is Resource.Success) {
                _mensajes.value = Resource.Success(emptyList())
            }
            return
        }
        
        viewModelScope.launch {
            _mensajes.value = Resource.Loading
            
            // Offline First
            val cached = OfflineCacheManager.getCachedMensajes(getApplication())
            if (cached.isNotEmpty()) {
                _mensajes.value = Resource.Success(cached)
            }

            try {
                val list = repository.getMensajes()
                _mensajes.value = Resource.Success(list)
                OfflineCacheManager.saveMensajes(getApplication(), list)
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    _mensajes.value = Resource.Error(e.localizedMessage ?: "Error de red")
                }
            }
        }
    }

    fun enviarMensaje(texto: String) {
        if (texto.isBlank()) return
        
        if (_isAiAssistantMode.value == true) {
            enviarMensajeIA(texto)
            return
        }

        viewModelScope.launch {
            // Optimistic UI: Agregar mensaje localmente antes de la red
            val userMsg = Mensaje(id = "local_${System.currentTimeMillis()}", texto = texto, remitente = "PACIENTE", fecha = Date())
            val currentList = (_mensajes.value as? Resource.Success)?.data?.toMutableList() ?: mutableListOf()
            currentList.add(userMsg)
            _mensajes.value = Resource.Success(currentList)

            _mensajeEnviado.value = Resource.Loading
            try {
                val mensaje = repository.enviarMensaje(texto)
                if (mensaje != null) {
                    _mensajeEnviado.value = Resource.Success(mensaje)
                    _isDoctorTyping.value = true
                    // Simulamos delay del doctor solo si el servidor no respondió con uno ya
                    delay(1500)
                    _isDoctorTyping.value = false
                    fetchMensajes()
                } else {
                    _mensajeEnviado.value = Resource.Error("Error al enviar")
                }
            } catch (e: Exception) {
                _mensajeEnviado.value = Resource.Error(e.localizedMessage ?: "Error de red")
                // Opcional: Remover el mensaje local si falló el envío (o marcarlo como fallido)
            }
        }
    }

    private fun enviarMensajeIA(texto: String) {
        viewModelScope.launch {
            // 1. Agregar mensaje del usuario localmente
            val userMsg = Mensaje(id = "local_${System.currentTimeMillis()}", texto = texto, remitente = "PACIENTE", fecha = Date())
            val currentList = (_mensajes.value as? Resource.Success)?.data?.toMutableList() ?: mutableListOf()
            currentList.add(userMsg)
            _mensajes.value = Resource.Success(currentList)
            
            _isDoctorTyping.value = true
            delay(1500)
            
            // 2. Obtener contexto para la IA
            val turnos = try { turnoRepository.getTurnos() } catch(_: Exception) { emptyList() }
            val meds = try { medRepository.getMedicamentos() } catch(_: Exception) { emptyList() }
            
            // 3. Generar respuesta "IA"
            val response = SmartAssistant.generateResponse(getApplication(), texto, turnos, meds)
            
            val aiMsg = Mensaje(id = "ai_${System.currentTimeMillis()}", texto = response, remitente = "DOCTOR", fecha = Date())
            currentList.add(aiMsg)
            _isDoctorTyping.value = false
            _mensajes.value = Resource.Success(currentList)
        }
    }
}

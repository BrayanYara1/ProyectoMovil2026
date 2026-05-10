package com.example.gestionturnosapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.RegisterRequest
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentRegisterBinding
import com.example.gestionturnosapp.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRegisterActions()
    }

    private fun setupRegisterActions() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etTelefono.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && pass.isNotEmpty()) {
                val request = RegisterRequest(name, email, phone, pass)
                executeRegistration(request)
            } else {
                Toast.makeText(requireContext(), getString(R.string.msg_complete_fields), Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun executeRegistration(request: RegisterRequest) {
        binding.progressBar.isVisible = true
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.register(request)
                
                if (_binding == null) return@launch

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val usuario = authResponse?.usuario
                    if (usuario != null) {
                        UserManager.saveUser(requireContext(), usuario, authResponse.token)
                    }
                    Toast.makeText(requireContext(), getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val displayMsg = when {
                        errorBody.contains("email", true) -> "Este correo ya está registrado"
                        errorBody.contains("password", true) -> "Contraseña demasiado débil"
                        else -> "Error: $errorBody"
                    }
                    Toast.makeText(requireContext(), displayMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    val errorMsg = e.localizedMessage ?: "Error de conexión"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
            } finally {
                _binding?.let {
                    it.progressBar.isVisible = false
                    it.btnRegister.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

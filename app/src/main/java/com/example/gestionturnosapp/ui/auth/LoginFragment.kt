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
import com.example.gestionturnosapp.data.LoginRequest
import com.example.gestionturnosapp.databinding.FragmentLoginBinding
import com.example.gestionturnosapp.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                Toast.makeText(requireContext(), "Campos vacíos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun performLogin(email: String, contrasena: String) {
        binding.progressBar.isVisible = true
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, contrasena))
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val usuario = authResponse?.usuario
                    if (usuario != null) {
                        com.example.gestionturnosapp.data.UserManager.saveUser(
                            requireContext(), 
                            usuario, 
                            authResponse.token
                        )
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                } else {
                    when (response.code()) {
                        401 -> Toast.makeText(requireContext(), "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        404 -> Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(requireContext(), "Error del servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    val errorMsg = e.localizedMessage ?: "Error de red"
                    val displayMsg = when {
                        errorMsg.contains("connect", true) -> "No se pudo conectar al servidor. Verifica tu internet."
                        errorMsg.contains("timeout", true) -> "El servidor tardó mucho en responder (puede estar despertando)."
                        else -> "Error: $errorMsg"
                    }
                    Toast.makeText(requireContext(), displayMsg, Toast.LENGTH_LONG).show()
                }
                android.util.Log.e("NETWORK_ERROR", "Fallo de login", e)
            } finally {
                binding.progressBar.isVisible = false
                binding.btnLogin.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

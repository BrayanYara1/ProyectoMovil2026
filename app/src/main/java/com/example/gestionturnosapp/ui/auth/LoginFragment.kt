package com.example.gestionturnosapp.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        // Enlace bidireccional manual (sin DataBinding para mayor control)
        binding.etEmail.doAfterTextChanged { 
            viewModel.loginEmail.value = it.toString()
            binding.tilEmail.error = null 
        }
        binding.etPassword.doAfterTextChanged { 
            viewModel.loginPassword.value = it.toString()
            binding.tilPassword.error = null 
        }

        binding.btnLogin.setOnClickListener {
            viewModel.login()
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun setupObservers() {
        viewModel.isLoginValid.observe(viewLifecycleOwner) { isValid ->
            binding.btnLogin.isEnabled = isValid
            // Opción: Cambiar opacidad del botón si no es válido
            binding.btnLogin.alpha = if (isValid) 1.0f else 0.5f
        }

        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnLogin.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.btnLogin.isEnabled = true
                    viewModel.resetAuthState() // Limpiar estado para evitar re-navegación
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBar.isVisible = false
                    binding.btnLogin.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

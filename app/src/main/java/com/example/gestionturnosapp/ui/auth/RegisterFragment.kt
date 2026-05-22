package com.example.gestionturnosapp.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.RegisterRequest
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupRegisterActions()
    }

    private fun setupObservers() {
        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnRegister.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(requireContext(), getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                    
                    // Ya no vamos a verificación, vamos directo al login
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnRegister.isEnabled = true
                    
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBar.isVisible = false
                    binding.btnRegister.isEnabled = true
                }
            }
        }
    }

    private fun setupRegisterActions() {
        // Limpiar errores mientras el usuario escribe
        binding.etNombre.doAfterTextChanged { binding.tilNombre.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etTelefono.doAfterTextChanged { binding.tilTelefono.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }

        binding.etTelefono.doAfterTextChanged { s ->
            val text = s.toString()
            if (text.isNotEmpty() && !text.startsWith("+57 ")) {
                val clean = text.replace("+57", "").trim()
                val formatted = "+57 $clean"
                binding.etTelefono.setText(formatted)
                binding.etTelefono.setSelection(binding.etTelefono.length())
            }
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etTelefono.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            var isValid = true

            if (name.isEmpty()) {
                binding.tilNombre.error = getString(R.string.msg_complete_fields)
                isValid = false
            }

            if (email.isEmpty()) {
                binding.tilEmail.error = getString(R.string.msg_complete_fields)
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = getString(R.string.msg_invalid_email)
                isValid = false
            }

            if (phone.isEmpty()) {
                binding.tilTelefono.error = getString(R.string.msg_complete_fields)
                isValid = false
            } else if (phone.length < 10) {
                binding.tilTelefono.error = getString(R.string.msg_invalid_phone)
                isValid = false
            }

            if (pass.isEmpty()) {
                binding.tilPassword.error = getString(R.string.msg_complete_fields)
                isValid = false
            } else if (pass.length < 6) {
                binding.tilPassword.error = getString(R.string.msg_password_length)
                isValid = false
            }

            if (isValid) {
                viewModel.register(RegisterRequest(name, email, phone, pass))
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

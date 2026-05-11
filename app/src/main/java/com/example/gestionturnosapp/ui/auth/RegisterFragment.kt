package com.example.gestionturnosapp.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
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
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
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
        binding.etTelefono.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && !text.startsWith("+57 ")) {
                    val clean = text.replace("+57 ", "")
                    binding.etTelefono.setText("+57 $clean")
                    binding.etTelefono.setSelection(binding.etTelefono.text?.length ?: 0)
                }
            }
        })

        binding.btnRegister.setOnClickListener {
            val name = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etTelefono.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            binding.tilNombre.error = null
            binding.tilEmail.error = null
            binding.tilTelefono.error = null
            binding.tilPassword.error = null

            var isValid = true

            if (name.isEmpty()) {
                binding.tilNombre.error = getString(R.string.msg_complete_fields)
                isValid = false
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = if (email.isEmpty()) getString(R.string.msg_complete_fields) else "Email inválido"
                isValid = false
            }

            if (phone.length < 10) {
                binding.tilTelefono.error = "Ingresa un número válido"
                isValid = false
            }

            if (pass.length < 6) {
                binding.tilPassword.error = if (pass.isEmpty()) getString(R.string.msg_complete_fields) else "Mínimo 6 caracteres"
                isValid = false
            }

            if (isValid) {
                binding.btnRegister.isEnabled = false
                viewModel.register(RegisterRequest(name, email, phone, pass), requireContext())
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

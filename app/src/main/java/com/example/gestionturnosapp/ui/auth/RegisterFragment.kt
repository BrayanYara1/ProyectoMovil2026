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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
        viewModel.isRegisterValid.observe(viewLifecycleOwner) { isValid ->
            binding.btnRegister.isEnabled = isValid
            binding.btnRegister.alpha = if (isValid) 1.0f else 0.5f
        }

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
        // Enlace bidireccional manual
        binding.etNombre.doAfterTextChanged { 
            viewModel.regName.value = it.toString()
            binding.tilNombre.error = null 
        }
        binding.etEmail.doAfterTextChanged { 
            viewModel.regEmail.value = it.toString()
            binding.tilEmail.error = null 
        }
        binding.etPassword.doAfterTextChanged { 
            viewModel.regPassword.value = it.toString()
            binding.tilPassword.error = null 
        }

        binding.etTelefono.addTextChangedListener(object : android.text.TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                
                val text = s.toString()
                if (text.isNotEmpty() && !text.startsWith("+57 ")) {
                    isUpdating = true
                    val clean = text.replace("+57", "").trim()
                    binding.etTelefono.setText("+57 $clean")
                    binding.etTelefono.setSelection(binding.etTelefono.length())
                    isUpdating = false
                }
                viewModel.regPhone.value = binding.etTelefono.text.toString()
                binding.tilTelefono.error = null
            }
        })

        binding.btnRegister.setOnClickListener {
            viewModel.register()
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

package com.example.gestionturnosapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.databinding.FragmentVerificationBinding

class VerificationFragment : Fragment() {

    private var _binding: FragmentVerificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var email: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        email = arguments?.getString("email") ?: ""
        if (email.isEmpty()) {
            findNavController().popBackStack()
            return
        }

        setupObservers()
        
        binding.btnVerify.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.length == 6) {
                viewModel.verify(email, code, requireContext())
            } else {
                binding.tilCode.error = getString(R.string.hint_verification_code)
            }
        }
    }

    private fun setupObservers() {
        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnVerify.isEnabled = false
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnVerify.isEnabled = true
                    if (resource.message == "VERIFY_SUCCESS") {
                        Toast.makeText(requireContext(), getString(R.string.msg_verify_success), Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_verificationFragment_to_loginFragment)
                    } else if (resource.message.startsWith("VERIFY_REQUIRED")) {
                        // Ya estamos aquí
                    } else {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    binding.progressBar.isVisible = false
                    binding.btnVerify.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

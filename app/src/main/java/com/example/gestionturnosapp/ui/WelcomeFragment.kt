package com.example.gestionturnosapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            setupUI()
            
            // Animación de entrada
            val animation = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_up)
            if (_binding != null) {
                binding.logoCard.startAnimation(animation)
            }
        } catch (e: Exception) {
            android.util.Log.e("WelcomeFragment", "Error en onViewCreated", e)
        }
    }

    private fun setupUI() {
        val user = UserManager.getUser(requireContext())
        
        if (user != null) {
            // Usuario logueado: Mostrar bienvenida personalizada
            binding.layoutAuthButtons.isVisible = false
            binding.btnGetStarted.isVisible = true
            binding.tvWelcomeSubtitle.text = "¡Hola de nuevo, ${user.nombre}!"
            
            binding.btnGetStarted.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                findNavController().navigate(R.id.action_welcomeFragment_to_homeFragment)
            }
        } else {
            // Usuario nuevo: Mostrar Login/Registro
            binding.layoutAuthButtons.isVisible = true
            binding.btnGetStarted.isVisible = false
            
            binding.btnLoginWelcome.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
            }

            binding.btnRegisterWelcome.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                findNavController().navigate(R.id.action_welcomeFragment_to_registerFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

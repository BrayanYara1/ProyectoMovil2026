package com.example.gestionturnosapp.ui

import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentWelcomeBinding
import com.example.gestionturnosapp.data.local.PreferenceManager
import com.example.gestionturnosapp.util.BiometricHelper
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        
        // Animación de entrada
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_up)
        binding.logoCard.startAnimation(animation)
    }

    private fun setupUI() {
        val user = userManager.getUser()
        
        if (user != null) {
            // Usuario logueado
            binding.layoutAuthButtons.isVisible = false
            binding.btnGetStarted.isVisible = true
            binding.tvWelcomeSubtitle.text = getString(R.string.welcome_back, user.nombre)
            
            val isBiometricEnabled = PreferenceManager.isBiometricEnabled(requireContext())
            val isBiometricAvailable = BiometricHelper.isBiometricAvailable(requireContext())

            if (isBiometricEnabled && isBiometricAvailable) {
                // Solo mostrar si no estamos navegando ya
                BiometricHelper.showBiometricPrompt(
                    activity = requireActivity(),
                    title = getString(R.string.title_biometric_auth),
                    subtitle = getString(R.string.subtitle_biometric_auth),
                    onSuccess = { safeNavigate(R.id.action_welcomeFragment_to_homeFragment) },
                    onError = { error ->
                        if (isAdded) {
                            Toast.makeText(requireContext(), "${getString(R.string.error_biometric_auth)}: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(800) // Un poco más rápido para mejorar la sensación de fluidez
                    safeNavigate(R.id.action_welcomeFragment_to_homeFragment)
                }
            }
            
            binding.btnGetStarted.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                safeNavigate(R.id.action_welcomeFragment_to_homeFragment)
            }
        } else {
            // Usuario nuevo: Mostrar Login/Registro
            binding.layoutAuthButtons.isVisible = true
            binding.btnGetStarted.isVisible = false
            
            binding.btnLoginWelcome.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                safeNavigate(R.id.action_welcomeFragment_to_loginFragment)
            }

            binding.btnRegisterWelcome.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                safeNavigate(R.id.action_welcomeFragment_to_registerFragment)
            }
        }
    }

    private fun safeNavigate(actionId: Int) {
        if (!isAdded) return
        try {
            val currentDest = findNavController().currentDestination?.id
            if (currentDest == R.id.welcomeFragment) {
                findNavController().navigate(actionId)
            }
        } catch (e: Exception) {
            Log.e("WelcomeFragment", "Navigation failed", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

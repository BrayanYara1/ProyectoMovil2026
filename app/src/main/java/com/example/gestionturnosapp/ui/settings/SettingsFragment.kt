package com.example.gestionturnosapp.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.gestionturnosapp.data.local.OfflineCacheManager
import com.example.gestionturnosapp.data.local.PreferenceManager
import com.example.gestionturnosapp.databinding.FragmentSettingsBinding
import com.example.gestionturnosapp.util.BiometricHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupUI()
        applyAnimations()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupUI() {
        val context = requireContext()

        // Inicializar Switches sin disparar listeners
        binding.switchNotifications.setOnCheckedChangeListener(null)
        binding.switchNotifications.isChecked = PreferenceManager.areNotificationsEnabled(context)
        
        binding.switchDarkMode.setOnCheckedChangeListener(null)
        binding.switchDarkMode.isChecked = PreferenceManager.isDarkMode(context)

        // BIOMETRÍA
        val isBiometricAvailable = BiometricHelper.isBiometricAvailable(context)
        binding.layoutBiometric.isVisible = isBiometricAvailable
        if (isBiometricAvailable) {
            binding.switchBiometric.setOnCheckedChangeListener(null)
            binding.switchBiometric.isChecked = PreferenceManager.isBiometricEnabled(context)
            binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                PreferenceManager.setBiometricEnabled(context, isChecked)
                val msg = if (isChecked) getString(R.string.msg_biometric_enabled) else getString(R.string.msg_biometric_disabled)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            }
        }

        // LISTENERS
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setNotificationsEnabled(context, isChecked)
            val msg = if (isChecked) getString(R.string.msg_notifications_on) else getString(R.string.msg_notifications_off)
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setDarkMode(context, isChecked)
            // El cambio de tema recreará la actividad automáticamente
        }

        // IDIOMA
        val currentLang = PreferenceManager.getLocale(context)
        binding.btnChangeLanguage.text = if (currentLang == "es") "Idioma: Español (ES)" else "Language: English (EN)"
        
        binding.btnChangeLanguage.setOnClickListener {
            val options = arrayOf("Español (ES)", "English (EN)")
            val checkedItem = if (currentLang == "es") 0 else 1
            
            MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.label_language))
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    val langTag = if (which == 0) "es" else "en"
                    if (langTag != currentLang) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            PreferenceManager.setLocale(context, langTag)
                            // setLocale recreará la actividad
                        }
                    }
                    dialog.dismiss()
                }
                .show()
        }

        // ACERCA DE
        binding.btnAbout.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.label_about))
                .setMessage(getString(R.string.version_info) + "\n\n" + getString(R.string.msg_about_app))
                .setPositiveButton(getString(android.R.string.ok), null)
                .show()
        }

        // CERRAR SESIÓN
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.btn_logout))
                .setMessage(getString(R.string.msg_logout_confirm))
                .setPositiveButton(getString(R.string.btn_yes)) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        OfflineCacheManager.clearCache(context)
                        UserManager.logout(context)
                        findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }

        // AYUDA
        binding.btnHelp.setOnClickListener {
            val user = UserManager.usuarioActual
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:soporte@saludactiva.com")
                val subject = getString(R.string.label_help_subject, user?.nombre ?: getString(R.string.label_anonymous))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Versión de la App: ${getString(R.string.version_info)}\nID Usuario: ${user?.id ?: "N/A"}\n\nEscribe tu consulta aquí:")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(binding.root, getString(R.string.msg_no_mail_app), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.root.startAnimation(fadeIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

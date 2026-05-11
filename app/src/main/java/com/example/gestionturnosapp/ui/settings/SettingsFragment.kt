package com.example.gestionturnosapp.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.PreferenceManager
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

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
        setupUI()
        applyAnimations()
    }

    private fun setupUI() {
        // Inicializar Switches
        binding.switchNotifications.isChecked = PreferenceManager.areNotificationsEnabled(requireContext())
        binding.switchDarkMode.isChecked = PreferenceManager.isDarkMode(requireContext())

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setNotificationsEnabled(requireContext(), isChecked)
            val msg = if (isChecked) "Notificaciones activadas" else "Notificaciones desactivadas"
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setDarkMode(requireContext(), isChecked)
        }

        // IDIOMA (MÉTODO MODERNO PERSISTENTE)
        binding.btnChangeLanguage.setOnClickListener {
            val options = arrayOf("Español (ES)", "English (EN)")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.label_language))
                .setItems(options) { _, which ->
                    val appLocale: LocaleListCompat = if (which == 0) {
                        LocaleListCompat.forLanguageTags("es")
                    } else {
                        LocaleListCompat.forLanguageTags("en")
                    }
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
                .show()
        }

        // ACERCA DE
        binding.btnAbout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.label_about))
                .setMessage(getString(R.string.version_info) + "\n\nGestionTurnos es una plataforma integral para la gestión de turnos médicos, diseñada para mejorar la experiencia del paciente.")
                .setPositiveButton(getString(android.R.string.ok), null)
                .show()
        }

        // CERRAR SESIÓN
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.btn_logout))
                .setMessage(getString(R.string.msg_logout_confirm))
                .setPositiveButton(getString(R.string.btn_yes)) { _, _ ->
                    UserManager.logout(requireContext())
                    findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }

        // AYUDA
        binding.btnHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:soporte@gestionturnos.com")
                putExtra(Intent.EXTRA_SUBJECT, "Ayuda GestionTurnos - Usuario: ${UserManager.usuarioActual?.nombre ?: "Anónimo"}")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "No hay app de correo instalada", Snackbar.LENGTH_SHORT).show()
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

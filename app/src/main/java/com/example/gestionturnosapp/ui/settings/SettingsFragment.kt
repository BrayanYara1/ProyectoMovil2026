package com.example.gestionturnosapp.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.databinding.FragmentSettingsBinding
import com.example.gestionturnosapp.data.UserManager

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
    }

    private fun setupUI() {
        // IDIOMA (MÉTODO MODERNO PERSISTENTE)
        binding.btnChangeLanguage.setOnClickListener {
            val options = arrayOf("Español (ES)", "English (EN)")
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.label_language))
                .setItems(options) { _, which ->
                    val appLocale: LocaleListCompat = if (which == 0) {
                        LocaleListCompat.forLanguageTags("es")
                    } else {
                        LocaleListCompat.forLanguageTags("en")
                    }
                    AppCompatDelegate.setApplicationLocales(appLocale)
                    Toast.makeText(requireContext(), R.string.label_language, Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // ACERCA DE
        binding.btnAbout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.label_about))
                .setMessage(getString(R.string.version_info))
                .setPositiveButton(getString(android.R.string.ok), null)
                .show()
        }

        // CERRAR SESIÓN
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
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
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("soporte@saludactiva.com"))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Ayuda SaludActiva - Usuario: ${UserManager.usuarioActual?.nombre}")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No hay app de correo instalada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

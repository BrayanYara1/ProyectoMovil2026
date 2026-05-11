package com.example.gestionturnosapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.ImageStorageManager
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        updateUI()

        binding.cardHomeProfile.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_homeFragment_to_userProfileFragment)
        }

        binding.cardSolicitarTurno.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_solicitarTurnoFragment)
        }

        binding.cardMisTurnos.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_turnosListFragment)
        }

        binding.cardEspecialidades.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_especialidadesFragment)
        }

        binding.cardMedication.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_medicamentosFragment)
        }

        binding.cardEstudios.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_estudiosFragment)
        }

        binding.cardUrgencias.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Llamada de Emergencia")
                .setMessage("¿Deseas llamar a la línea de emergencias 123 (Colombia)?")
                .setPositiveButton("Llamar") { _, _ ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:123")
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.cardUrgencias.setOnLongClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
            intent.data = android.net.Uri.parse("tel:123")
            startActivity(intent)
            true
        }

        binding.cardNextAppointment.setOnClickListener {
            viewModel.nextTurno.value?.let { turno ->
                val bundle = Bundle().apply {
                    putString("TURNO_ID", turno.id)
                    putString("PACIENTE_NOMBRE", turno.pacienteNombre)
                    putString("TURNO_FECHA", turno.fecha)
                    putString("TURNO_HORA", turno.hora)
                    putString("TURNO_MOTIVO", turno.motivo)
                    putString("TURNO_ESTADO", turno.estado)
                }
                findNavController().navigate(R.id.action_homeFragment_to_turnoDetailFragment, bundle)
            }
        }
    }

    private fun updateUI() {
        val user = UserManager.getUser(requireContext())
        binding.tvGreeting.text = getString(R.string.home_greeting, user?.nombre ?: "Usuario")
        updateAvatar()
    }

    private fun updateAvatar() {
        val savedImageUri = ImageStorageManager.getProfileImageUri(requireContext())
        if (savedImageUri != null) {
            binding.ivUserAvatar.load(savedImageUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            binding.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (it.contains("401") || it.contains("token", true)) {
                    handleSessionExpired()
                } else {
                    com.google.android.material.snackbar.Snackbar.make(binding.root, it, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.turnosCount.observe(viewLifecycleOwner) { count ->
            binding.tvBadgeTurnos.text = count.toString()
            binding.tvBadgeTurnos.isVisible = count > 0
        }

        viewModel.nextTurno.observe(viewLifecycleOwner) { turno ->
            if (turno != null) {
                binding.cardNextAppointment.isVisible = true
                binding.tvNextAppointName.text = getString(
                    R.string.label_next_appointment_format,
                    turno.especialidad ?: "Consulta",
                    turno.doctor ?: "Dr. Pendiente"
                )
                
                // Formatear hora para mostrar AM/PM
                val displayTime = try {
                    val inputFormats = listOf("hh:mm a", "h:mm a", "HH:mm")
                    var dateObj: java.util.Date? = null
                    for (fmt in inputFormats) {
                        try {
                            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                            dateObj = sdf.parse(turno.hora)
                            if (dateObj != null) break
                        } catch (e: Exception) {}
                    }
                    if (dateObj != null) {
                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(dateObj)
                    } else turno.hora
                } catch (e: Exception) {
                    turno.hora
                }

                binding.tvNextAppointDate.text = getString(
                    R.string.detail_date_time_format,
                    turno.fecha,
                    displayTime
                )
            } else {
                binding.cardNextAppointment.isVisible = false
            }
        }

        viewModel.medicamentos.observe(viewLifecycleOwner) { meds ->
            binding.tvNoMeds.isVisible = meds.isEmpty()
            displayMedicamentos(meds)
        }

        viewModel.healthTipResId.observe(viewLifecycleOwner) { resId ->
            binding.tvHealthTip.text = getString(resId)
        }
    }

    private fun handleSessionExpired() {
        UserManager.logout(requireContext())
        findNavController().navigate(R.id.loginFragment, null, androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build())
    }

    private fun displayMedicamentos(meds: List<com.example.gestionturnosapp.data.Medicamento>) {
        binding.layoutMedication.removeAllViews()
        if (meds.isEmpty()) {
            binding.layoutMedication.addView(binding.tvNoMeds)
            return
        }

        meds.forEach { med ->
            val medView = LayoutInflater.from(context).inflate(R.layout.item_medication_home, binding.layoutMedication, false)
            medView.findViewById<android.widget.TextView>(R.id.tvMedName).text = "${med.nombre} ${med.dosis}"
            medView.findViewById<android.widget.TextView>(R.id.tvMedSchedule).text = "${med.frecuencia} - Próxima: ${med.proximaToma}"
            
            // LIMPIEZA HOME: Ocultar botón de borrar en el dashboard para una vista más limpia
            medView.findViewById<android.view.View>(R.id.btnDeleteMed).visibility = android.view.View.GONE

            binding.layoutMedication.addView(medView)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }

        binding.cardHomeProfile.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_homeFragment_to_userProfileFragment)
        }

        binding.cardSolicitarTurno.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_homeFragment_to_solicitarTurnoFragment)
        }

        binding.cardMisTurnos.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_homeFragment_to_turnosListFragment)
        }

        binding.cardEspecialidades.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_homeFragment_to_especialidadesFragment)
        }

        binding.cardMedication.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_homeFragment_to_medicamentosFragment)
        }

        binding.cardEstudios.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_homeFragment_to_estudiosFragment)
        }

        binding.cardUrgencias.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.label_emergency_call))
                .setMessage(getString(R.string.msg_emergency_confirm))
                .setPositiveButton(getString(R.string.btn_call)) { _, _ ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:123")
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }

        binding.cardUrgencias.setOnLongClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
            intent.data = android.net.Uri.parse("tel:123")
            startActivity(intent)
            true
        }

        binding.cardShareSummary.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            shareHealthSummary()
        }
    }

    private fun updateUI() {
        val user = UserManager.getUser(requireContext())
        var name = user?.nombre ?: getString(R.string.label_anonymous)
        
        // LIMPIEZA AGRESIVA: Eliminar versión (vX.X.X), saltos de línea y emojis previos del nombre
        name = name.replace(Regex("\\s*\\(v?\\d+(\\.\\d+)*\\)\\s*"), "")
                   .split("\n")[0]
                   .trim()
        
        // Saludo Dinámico según la hora del día
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 6..11 -> R.string.greeting_morning
            in 12..18 -> R.string.greeting_afternoon
            in 19..23 -> R.string.greeting_evening
            else -> R.string.welcome
        }
        
        binding.tvGreeting.text = if (greetingRes == R.string.welcome) {
            getString(greetingRes) + ", " + name
        } else {
            getString(greetingRes, name)
        }
        updateAvatar()
    }

    private fun updateAvatar() {
        val savedImageUri = ImageStorageManager.getProfileImageUri(requireContext())
        binding.ivUserAvatar.load(savedImageUri) {
            crossfade(true)
            placeholder(R.drawable.ic_nav_profile)
            error(R.drawable.ic_nav_profile)
            transformations(CircleCropTransformation())
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            if (!isLoading) {
                binding.swipeRefresh.isRefreshing = false
            }
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
                // Estado con cita
                binding.cardNextAppointment.setCardBackgroundColor(requireContext().getColor(R.color.primary))
                binding.tvNextAppointLabel.text = getString(R.string.title_next_appointment)
                binding.tvNextAppointLabel.setTextColor(android.graphics.Color.parseColor("#B3FFFFFF"))
                
                binding.tvNextAppointName.text = getString(
                    R.string.label_next_appointment_format,
                    turno.especialidad ?: "Consulta",
                    turno.doctor ?: "Dr. Pendiente"
                )
                binding.tvNextAppointName.setTextColor(requireContext().getColor(R.color.white))
                
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

                // Formatear fecha de forma más amigable
                val displayDate = try {
                    val sdfInput = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val date = sdfInput.parse(turno.fecha)
                    if (date != null) {
                        val calendar = java.util.Calendar.getInstance()
                        val today = calendar.time
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                        val tomorrow = calendar.time
                        
                        val sdfDay = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        when (sdfDay.format(date)) {
                            sdfDay.format(today) -> getString(R.string.filter_all).replace("Todos", "Hoy") // O un string específico
                            sdfDay.format(tomorrow) -> "Mañana"
                            else -> java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale("es", "ES")).format(date).replaceFirstChar { it.uppercase() }
                        }
                    } else turno.fecha
                } catch (e: Exception) {
                    turno.fecha
                }

                binding.tvNextAppointDate.text = getString(
                    R.string.detail_date_time_format,
                    displayDate,
                    displayTime
                )
                binding.tvNextAppointDate.setTextColor(requireContext().getColor(R.color.white))
                binding.ivNextAppointIcon.imageTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.white))
                
                binding.cardNextAppointment.setOnClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
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
            } else {
                // Estado vacío: Invitar a agendar
                binding.cardNextAppointment.setCardBackgroundColor(requireContext().getColor(R.color.primary_container))
                binding.tvNextAppointLabel.text = getString(R.string.no_upcoming_appointments).uppercase()
                binding.tvNextAppointLabel.setTextColor(requireContext().getColor(R.color.primary))
                
                binding.tvNextAppointName.text = getString(R.string.schedule_appointment_prompt)
                binding.tvNextAppointName.setTextColor(requireContext().getColor(R.color.text_primary))
                
                binding.tvNextAppointDate.text = getString(R.string.menu_request_appointment)
                binding.tvNextAppointDate.setTextColor(requireContext().getColor(R.color.primary))
                binding.ivNextAppointIcon.imageTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.primary))

                binding.cardNextAppointment.setOnClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    findNavController().navigate(R.id.action_homeFragment_to_solicitarTurnoFragment)
                }
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

    private fun shareHealthSummary() {
        val nextTurno = viewModel.nextTurno.value
        val meds = viewModel.medicamentos.value ?: emptyList()
        val user = UserManager.getUser(requireContext())
        
        val summary = StringBuilder()
        summary.append(getString(R.string.label_share_appointment_header)).append("\n")
        summary.append(getString(R.string.label_share_patient, user?.nombre ?: getString(R.string.label_anonymous))).append("\n\n")
        
        if (nextTurno != null) {
            summary.append(getString(R.string.title_next_appointment)).append(":\n")
            summary.append("- ${nextTurno.especialidad}: ${nextTurno.fecha} ").append(getString(R.string.detail_date_time_format, "", nextTurno.hora).trim()).append("\n")
            summary.append("- Dr. ${nextTurno.doctor}\n\n")
        }
        
        if (meds.isNotEmpty()) {
            summary.append(getString(R.string.title_your_medications)).append(":\n")
            meds.forEach { med ->
                summary.append("- ${med.nombre} (${med.dosis}): ${med.frecuencia}\n")
            }
        } else {
            summary.append(getString(R.string.msg_no_medication)).append("\n")
        }
        
        summary.append("\n_").append(getString(R.string.app_name)).append("_")
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, summary.toString())
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.label_share_via)))
    }

    private fun displayMedicamentos(meds: List<com.example.gestionturnosapp.data.Medicamento>) {
        binding.layoutMedication.removeAllViews()
        if (meds.isEmpty()) {
            binding.layoutMedication.addView(binding.tvNoMeds)
            return
        }

        // Mostrar solo los 2-3 primeros para no saturar el home
        meds.take(3).forEach { med ->
            val medView = LayoutInflater.from(context).inflate(R.layout.item_medication_home, binding.layoutMedication, false)
            medView.findViewById<android.widget.TextView>(R.id.tvMedName).text = "${med.nombre} ${med.dosis}"
            medView.findViewById<android.widget.TextView>(R.id.tvMedSchedule).text = "${med.frecuencia}"
            
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

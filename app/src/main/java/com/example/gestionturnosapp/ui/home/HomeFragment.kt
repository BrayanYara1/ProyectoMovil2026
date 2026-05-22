package com.example.gestionturnosapp.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
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
import com.example.gestionturnosapp.util.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.gestionturnosapp.data.OfflineCacheManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

        val navigateTo = { actionId: Int ->
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(actionId)
        }

        binding.cardHomeProfile.setOnClickListener { navigateTo(R.id.action_homeFragment_to_userProfileFragment) }
        binding.cardSearch.setOnClickListener { navigateTo(R.id.action_homeFragment_to_especialidadesFragment) }
        binding.cardSolicitarTurno.setOnClickListener { navigateTo(R.id.action_homeFragment_to_solicitarTurnoFragment) }
        binding.cardMisTurnos.setOnClickListener { navigateTo(R.id.action_homeFragment_to_turnosListFragment) }
        binding.cardEspecialidades.setOnClickListener { navigateTo(R.id.action_homeFragment_to_especialidadesFragment) }
        binding.cardMedication.setOnClickListener { navigateTo(R.id.action_homeFragment_to_medicamentosFragment) }
        binding.btnAddMedHome.setOnClickListener { navigateTo(R.id.action_homeFragment_to_medicamentosFragment) }
        binding.cardEstudios.setOnClickListener { navigateTo(R.id.action_homeFragment_to_estudiosFragment) }
        binding.cardChat.setOnClickListener { navigateTo(R.id.action_homeFragment_to_chatFragment) }

        binding.cardUrgencias.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.label_emergency_call))
                .setMessage(getString(R.string.msg_emergency_confirm))
                .setPositiveButton(getString(R.string.btn_call)) { _, _ ->
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = "tel:123".toUri()
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }

        binding.cardUrgencias.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = "tel:123".toUri()
            startActivity(intent)
            true
        }

        binding.cardShareSummary.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            shareHealthSummary()
        }
    }

    private fun updateUI() {
        val context = context ?: return
        val user = UserManager.getUser(context)
        val rawName = user?.nombre ?: getString(R.string.label_anonymous)
        
        // Limpieza del nombre: Eliminar versión (vX.X.X), saltos de línea y espacios extra
        val cleanName = rawName.replace(Regex("\\s*\\(v?\\d+(\\.\\d+)*\\)\\s*"), "")
                              .split("\n")[0]
                              .trim()
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 6..11 -> R.string.greeting_morning
            in 12..18 -> R.string.greeting_afternoon
            in 19..23 -> R.string.greeting_evening
            else -> R.string.welcome
        }
        
        binding.tvGreeting.text = getString(greetingRes, cleanName)
        updateAvatar()
    }

    private fun updateAvatar() {
        val context = context ?: return
        val savedImageUri = ImageStorageManager.getProfileImageUri(context)
        binding.ivUserAvatar.load(savedImageUri) {
            crossfade(true)
            placeholder(R.drawable.ic_nav_profile)
            error(R.drawable.ic_nav_profile)
            transformations(CircleCropTransformation())
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = false // Desactivamos el progress bar viejo
            val isRefreshing = binding.swipeRefresh.isRefreshing
            
            if (isLoading && !isRefreshing) {
                binding.shimmerHome.isVisible = true
                binding.shimmerHome.startShimmer()
            } else {
                binding.shimmerHome.stopShimmer()
                binding.shimmerHome.isVisible = false
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.takeIf { it.isNotBlank() }?.let {
                if (it.contains("401") || it.contains("token", true)) {
                    handleSessionExpired()
                } else {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.turnosCount.observe(viewLifecycleOwner) { count ->
            binding.tvBadgeTurnos.text = count.toString()
            binding.tvBadgeTurnos.isVisible = count > 0
        }

        viewModel.nextTurno.observe(viewLifecycleOwner) { turno ->
            if (turno != null) {
                // Estado con cita: Ticket Premium
                binding.cardNextAppointment.setCardBackgroundColor(requireContext().getColor(R.color.surface))
                binding.tvNextAppointLabel.text = getString(R.string.title_next_appointment)
                binding.tvNextAppointLabel.setTextColor(requireContext().getColor(R.color.text_secondary))
                
                binding.tvNextAppointName.text = getString(
                    R.string.label_next_appointment_format,
                    turno.especialidad ?: getString(R.string.label_default_specialty),
                    turno.doctor ?: getString(R.string.label_default_doctor)
                )
                binding.tvNextAppointName.setTextColor(requireContext().getColor(R.color.text_primary))
                
                val displayTime = DateUtils.formatDisplayTime(turno.hora)
                val displayDate = DateUtils.formatDisplayDate(requireContext(), turno.fecha)

                binding.tvNextAppointDate.text = getString(
                    R.string.detail_date_time_format,
                    displayDate,
                    displayTime
                )
                binding.tvNextAppointDate.setTextColor(requireContext().getColor(R.color.text_secondary))
                binding.ivNextAppointIcon.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.primary))
                binding.ivNextAppointIconContainer.setCardBackgroundColor(ColorStateList.valueOf(requireContext().getColor(R.color.primary_container)))
                
                binding.cardNextAppointment.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
                // Estado vacío: Invitar a agendar con estilo Dashboard
                binding.cardNextAppointment.setCardBackgroundColor(requireContext().getColor(R.color.primary_container))
                binding.tvNextAppointLabel.text = getString(R.string.no_upcoming_appointments).uppercase()
                binding.tvNextAppointLabel.setTextColor(requireContext().getColor(R.color.primary))
                
                binding.tvNextAppointName.text = getString(R.string.schedule_appointment_prompt)
                binding.tvNextAppointName.setTextColor(requireContext().getColor(R.color.text_primary))
                
                binding.tvNextAppointDate.text = getString(R.string.menu_request_appointment)
                binding.tvNextAppointDate.setTextColor(requireContext().getColor(R.color.primary))
                binding.ivNextAppointIcon.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.white))
                binding.ivNextAppointIconContainer.setCardBackgroundColor(ColorStateList.valueOf(requireContext().getColor(R.color.primary)))

                binding.cardNextAppointment.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
        viewLifecycleOwner.lifecycleScope.launch {
            OfflineCacheManager.clearCache(requireContext())
            UserManager.logout(requireContext())
            findNavController().navigate(R.id.loginFragment, null, androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build())
        }
    }

    private fun shareHealthSummary() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_health_summary)
            .setMessage(R.string.label_share_health_question)
            .setPositiveButton(R.string.label_share_pdf) { _, _ ->
                generateAndSharePdf()
            }
            .setNegativeButton(R.string.label_share_text) { _, _ ->
                shareTextSummary()
            }
            .show()
    }

    private fun generateAndSharePdf() {
        val user = UserManager.getUser(requireContext())
        val turnos = viewModel.allTurnos.value ?: emptyList()
        val meds = viewModel.medicamentos.value ?: emptyList()
        
        val pdfUri = com.example.gestionturnosapp.util.PdfGenerator.generateHealthReport(
            requireContext(), user, turnos, meds
        )
        
        if (pdfUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.label_share_via)))
        } else {
            Snackbar.make(binding.root, R.string.msg_pdf_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun shareTextSummary() {
        val nextTurno = viewModel.nextTurno.value
        val meds = viewModel.medicamentos.value ?: emptyList()
        val user = UserManager.getUser(requireContext())
        
        val summary = StringBuilder()
        summary.append(getString(R.string.label_share_appointment_header)).append("\n")
        summary.append(getString(R.string.label_share_patient, user?.nombre ?: getString(R.string.label_anonymous))).append("\n\n")
        
        if (nextTurno != null) {
            val displayTime = DateUtils.formatDisplayTime(nextTurno.hora)
            val displayDate = DateUtils.formatDisplayDate(requireContext(), nextTurno.fecha)

            summary.append(getString(R.string.title_next_appointment)).append(":\n")
            summary.append("- ").append(getString(R.string.label_share_date, displayDate)).append("\n")
            summary.append("- ").append(getString(R.string.label_share_time, displayTime)).append("\n")
            summary.append("- ").append(getString(R.string.label_share_reason, nextTurno.especialidad ?: getString(R.string.label_default_specialty))).append("\n")
            summary.append("- Dr. ${nextTurno.doctor ?: getString(R.string.label_default_doctor)}\n\n")
        }
        
        if (meds.isNotEmpty()) {
            summary.append(getString(R.string.title_your_medications)).append(":\n")
            meds.forEach { med ->
                summary.append("- ").append(getString(R.string.label_medication_format, med.nombre, med.dosis))
                summary.append(": ${med.frecuencia}\n")
            }
        } else {
            summary.append(getString(R.string.msg_no_medication)).append("\n")
        }
        
        summary.append("\n_").append(getString(R.string.app_name)).append("_")
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summary.toString())
        }
        startActivity(Intent.createChooser(intent, getString(R.string.label_share_via)))
    }

    private fun displayMedicamentos(meds: List<com.example.gestionturnosapp.data.Medicamento>) {
        val hasMeds = meds.isNotEmpty()
        binding.tvNoMeds.visibility = if (hasMeds) View.GONE else View.VISIBLE
        binding.lottieMeds.visibility = if (hasMeds) View.GONE else View.VISIBLE

        binding.layoutMedication.apply {
            // Limpieza TOTAL de views previas EXCEPTO los elementos fijos (tvNoMeds, lottieMeds)
            val fixedViewIds = setOf(R.id.tvNoMeds, R.id.lottieMeds)
            val childrenToRemove = mutableListOf<View>()
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.id !in fixedViewIds) {
                    childrenToRemove.add(child)
                }
            }
            childrenToRemove.forEach { removeView(it) }

            if (hasMeds) {
                // Mostrar máximo 3 medicamentos para no saturar el Inicio
                meds.take(3).forEach { med ->
                    try {
                        val medView = LayoutInflater.from(context).inflate(R.layout.item_medication_home, this, false)
                        medView.findViewById<TextView>(R.id.tvMedName).text = getString(R.string.label_medication_format, med.nombre, med.dosis)
                        
                        val scheduleText = if (!med.proximaToma.isNullOrBlank()) {
                            "${med.frecuencia} • ${getString(R.string.hint_med_next)}: ${med.proximaToma}"
                        } else {
                            med.frecuencia
                        }
                        medView.findViewById<TextView>(R.id.tvMedSchedule).text = scheduleText
                        
                        // Configuración visual interactiva para Dashboard
                        medView.findViewById<View>(R.id.btnDeleteMed).visibility = View.GONE
                        val btnTomar = medView.findViewById<ImageView>(R.id.ivMedInfo)
                        btnTomar.setImageResource(android.R.drawable.checkbox_off_background)
                        btnTomar.alpha = 1.0f
                        btnTomar.setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            btnTomar.setImageResource(android.R.drawable.checkbox_on_background)
                            btnTomar.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.success))
                            viewModel.marcarComoTomado(med)
                            Snackbar.make(binding.root, "Dosis de ${med.nombre} registrada", Snackbar.LENGTH_SHORT).show()
                        }
                        
                        addView(medView)
                    } catch (e: Exception) {
                        android.util.Log.e("HomeFragment", "Error inflating med item", e)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncAll()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

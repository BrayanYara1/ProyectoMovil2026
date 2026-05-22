package com.example.gestionturnosapp.ui.turnos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentTurnoDetailBinding
import com.example.gestionturnosapp.notifications.ReminderReceiver
import com.example.gestionturnosapp.util.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class TurnoDetailFragment : Fragment() {

    private var _binding: FragmentTurnoDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TurnosListViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = android.transition.TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTurnoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = arguments?.getString("TURNO_ID") ?: ""
        val paciente = arguments?.getString("PACIENTE_NOMBRE") ?: getString(R.string.label_anonymous)
        val fecha = arguments?.getString("TURNO_FECHA") ?: "0000-00-00"
        val hora = arguments?.getString("TURNO_HORA") ?: "00:00"
        val motivo = arguments?.getString("TURNO_MOTIVO") ?: getString(R.string.no_appointments)
        val estado = arguments?.getString("TURNO_ESTADO") ?: "Pendiente"

        setupUI(id, paciente, fecha, hora, motivo, estado)
        setupObservers()
    }

    private fun setupUI(id: String, paciente: String, fecha: String, hora: String, motivo: String, estado: String) {
        binding.apply {
            tvDetailNombre.text = paciente
            
            // Llenar Badge de Fecha (Shared Element)
            try {
                val sdfInput = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val date = sdfInput.parse(fecha)
                if (date != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = date
                    tvDetailDia.text = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
                    tvDetailMes.text = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(date).uppercase()
                }
            } catch (e: Exception) {}

            // Formatear hora para mostrar AM/PM siempre (Resiliente)
            val displayTime = DateUtils.formatDisplayTime(hora)
            val displayDate = DateUtils.formatDisplayDate(requireContext(), fecha)

            tvDetailFechaHora.text = getString(R.string.detail_date_time_format, displayDate, displayTime)
            tvDetailMotivo.text = motivo
            
            // Configurar Estado Visual
            tvDetailStatus.text = when(estado.lowercase()) {
                "completado", "completed" -> getString(R.string.status_completed)
                "cancelado", "cancelled" -> getString(R.string.status_cancelled)
                else -> getString(R.string.status_pending)
            }
            
            val statusBg = when(estado.lowercase()) {
                "completado", "completed" -> R.drawable.bg_status_completed
                "cancelado", "cancelled" -> R.drawable.bg_status_cancelled
                else -> R.drawable.bg_status_pending
            }
            tvDetailStatus.setBackgroundResource(statusBg)

            btnCancelarTurno.visibility = if (estado.lowercase() == "pendiente" || estado.lowercase() == "pending") {
                View.VISIBLE
            } else {
                View.GONE
            }

            btnVolver.setOnClickListener {
                findNavController().popBackStack()
            }

            btnAddToCalendar.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                addToCalendar(paciente, fecha, hora, motivo)
            }

            btnShareTurno.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                shareTurno(paciente, fecha, hora, motivo)
            }

            btnOpenMap.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                openMap()
            }

            btnSetReminder.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                setReminder(paciente, fecha, hora)
            }

            btnCancelarTurno.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.title_delete_appointment)
                    .setMessage(R.string.msg_delete_confirm)
                    .setPositiveButton(R.string.btn_delete) { _, _ ->
                        viewModel.eliminarTurno(id)
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.turnoEliminadoExitosamente.observe(viewLifecycleOwner) { exito ->
            if (exito) {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    getString(R.string.msg_cancel_success),
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
                viewModel.resetNavegacion()
                findNavController().popBackStack()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnCancelarTurno.isEnabled = !loading
            binding.btnVolver.isEnabled = !loading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                if (error.contains("401") || error.contains("token", true)) {
                    handleSessionExpired()
                } else {
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        error,
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handleSessionExpired() {
        UserManager.logout(requireContext())
        Toast.makeText(requireContext(), getString(R.string.msg_session_expired), Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.loginFragment, null, androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build())
    }

    private fun setReminder(paciente: String, fecha: String, hora: String) {
        val calendar = Calendar.getInstance()
        try {
            val dateParts = fecha.split("-")
            val timeDate = DateUtils.parseTime(hora)

            if (dateParts.size == 3 && timeDate != null) {
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = timeDate

                calendar.set(Calendar.YEAR, dateParts[0].toInt())
                calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 0)

                calendar.add(Calendar.HOUR_OF_DAY, -1) // 1 hora antes

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        getString(R.string.msg_slot_too_soon),
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                    return
                }

                val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val notificationId = (paciente + fecha + hora).hashCode()
                val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                    putExtra("TITLE", getString(R.string.title_reminder_appointment))
                    putExtra("MESSAGE", getString(R.string.msg_reminder_appointment, paciente))
                    putExtra("NOTIFICATION_ID", notificationId)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(), notificationId, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                     alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                     alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }

                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    getString(R.string.msg_reminder_set),
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                getString(R.string.msg_reminder_error),
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun shareTurno(paciente: String, fecha: String, hora: String, motivo: String) {
        val message = "${getString(R.string.label_share_appointment_header)}\n" +
                "${getString(R.string.label_share_patient, paciente)}\n" +
                "${getString(R.string.label_share_date, fecha)}\n" +
                "${getString(R.string.label_share_time, hora)}\n" +
                "${getString(R.string.label_share_reason, motivo)}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.label_share_via)))
    }

    private fun openMap() {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=Clinica+Salud+Activa"))
        intent.setPackage("com.google.android.apps.maps")
        try { startActivity(intent) } catch (e: Exception) { 
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                getString(R.string.msg_install_maps),
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun addToCalendar(paciente: String, fecha: String, hora: String, motivo: String) {
        val cal = Calendar.getInstance()
        try {
            val d = fecha.split("-")
            val timeDate = DateUtils.parseTime(hora)

            if (d.size == 3 && timeDate != null) {
                val timeCal = Calendar.getInstance()
                timeCal.time = timeDate
                
                cal.set(d[0].toInt(), d[1].toInt()-1, d[2].toInt(), 
                    timeCal.get(Calendar.HOUR_OF_DAY), timeCal.get(Calendar.MINUTE))

                val intent = Intent(Intent.ACTION_INSERT)
                    .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                    .putExtra(android.provider.CalendarContract.Events.TITLE, getString(R.string.label_calendar_event_title, paciente))
                    .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, getString(R.string.label_calendar_event_desc, motivo))
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                startActivity(intent)
            }
        } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

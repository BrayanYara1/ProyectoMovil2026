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
import com.example.gestionturnosapp.databinding.FragmentTurnoDetailBinding
import com.example.gestionturnosapp.notifications.ReminderReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class TurnoDetailFragment : Fragment() {

    private var _binding: FragmentTurnoDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TurnosListViewModel by activityViewModels()

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
        val paciente = arguments?.getString("PACIENTE_NOMBRE") ?: "No disponible"
        val fecha = arguments?.getString("TURNO_FECHA") ?: "0000-00-00"
        val hora = arguments?.getString("TURNO_HORA") ?: "00:00"
        val motivo = arguments?.getString("TURNO_MOTIVO") ?: "Sin motivo especificado"
        val estado = arguments?.getString("TURNO_ESTADO") ?: "Pendiente"

        setupUI(id, paciente, fecha, hora, motivo, estado)
        setupObservers()
    }

    private fun setupUI(id: String, paciente: String, fecha: String, hora: String, motivo: String, estado: String) {
        binding.apply {
            tvDetailNombre.text = paciente
            
            // Formatear hora para mostrar AM/PM
            val displayTime = try {
                val inputFormats = listOf("HH:mm", "hh:mm a", "h:mm a")
                var dateObj: java.util.Date? = null
                for (fmt in inputFormats) {
                    try {
                        val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                        dateObj = sdf.parse(hora)
                        if (dateObj != null) break
                    } catch (e: Exception) {}
                }
                if (dateObj != null) {
                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(dateObj)
                } else hora
            } catch (e: Exception) {
                hora
            }

            tvDetailFechaHora.text = getString(R.string.detail_date_time_format, fecha, displayTime)
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
                    "Turno cancelado con éxito",
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
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    error,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setReminder(paciente: String, fecha: String, hora: String) {
        val calendar = Calendar.getInstance()
        try {
            val dateParts = fecha.split("-")
            
            // Usar SimpleDateFormat para parsear la hora AM/PM correctamente
            val inputFormats = listOf("HH:mm", "hh:mm a", "h:mm a")
            var timeDate: java.util.Date? = null
            for (fmt in inputFormats) {
                try {
                    val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                    timeDate = sdf.parse(hora)
                    if (timeDate != null) break
                } catch (e: Exception) {}
            }

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
                        "El turno es muy pronto para el recordatorio",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                    return
                }

                val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                    putExtra("TITLE", "Recordatorio de Turno")
                    putExtra("MESSAGE", "Cita con $paciente en 1 hora.")
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(), (paciente + fecha).hashCode(), intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                     alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                     alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }

                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "¡Alerta activada para 1h antes!",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "Error al configurar recordatorio",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun shareTurno(paciente: String, fecha: String, hora: String, motivo: String) {
        val message = "📅 *Turno SaludActiva*\n👤 $paciente\n🗓️ $fecha\n⏰ $hora\n🩺 $motivo"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, "Compartir vía"))
    }

    private fun openMap() {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=Clinica+SaludActiva"))
        intent.setPackage("com.google.android.apps.maps")
        try { startActivity(intent) } catch (e: Exception) { 
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "Instala Google Maps",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun addToCalendar(paciente: String, fecha: String, hora: String, motivo: String) {
        val cal = Calendar.getInstance()
        try {
            val d = fecha.split("-")
            val inputFormats = listOf("HH:mm", "hh:mm a", "h:mm a")
            var timeDate: java.util.Date? = null
            for (fmt in inputFormats) {
                try {
                    val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                    timeDate = sdf.parse(hora)
                    if (timeDate != null) break
                } catch (e: Exception) {}
            }

            if (d.size == 3 && timeDate != null) {
                val timeCal = Calendar.getInstance()
                timeCal.time = timeDate
                
                cal.set(d[0].toInt(), d[1].toInt()-1, d[2].toInt(), 
                    timeCal.get(Calendar.HOUR_OF_DAY), timeCal.get(Calendar.MINUTE))

                val intent = Intent(Intent.ACTION_INSERT)
                    .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                    .putExtra(android.provider.CalendarContract.Events.TITLE, "Cita: $paciente")
                    .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Motivo: $motivo")
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

package com.example.gestionturnosapp.ui.medicamentos

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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.model.Medicamento
import com.example.gestionturnosapp.util.Resource
import com.example.gestionturnosapp.databinding.FragmentMedicamentosBinding
import com.example.gestionturnosapp.notifications.ReminderReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class MedicamentosFragment : Fragment() {

    private var _binding: FragmentMedicamentosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicamentosViewModel by viewModels()
    private lateinit var adapter: MedicamentosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicamentosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncPendingMeds()
        viewModel.loadMedicamentos()
    }

    private fun setupRecyclerView() {
        adapter = MedicamentosAdapter(
            onItemClick = { med ->
                showMedicationDetailDialog(med)
            },
            onDeleteClick = { med ->
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.title_delete_medication))
                    .setMessage(getString(R.string.msg_confirm_delete_medication, med.nombre))
                    .setPositiveButton(getString(R.string.btn_delete_confirm)) { _, _ ->
                        viewModel.eliminarMedicamento(med.id)
                    }
                    .setNegativeButton(getString(R.string.btn_cancel_dialog), null)
                    .show()
            }
        )
        binding.rvMedicamentos.layoutManager = LinearLayoutManager(context)
        binding.rvMedicamentos.adapter = adapter
    }

    private fun showMedicationDetailDialog(med: Medicamento) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(med.nombre)
            .setMessage("${getString(R.string.hint_med_dose)}: ${med.dosis}\n" +
                     "${getString(R.string.hint_med_freq)}: ${med.frecuencia}\n" +
                     "${getString(R.string.hint_med_next)}: ${med.proximaToma}")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveMed.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            val nombre = binding.etMedName.text.toString()
            val dosis = binding.etMedDose.text.toString()
            val frecuencia = binding.etMedFreq.text.toString()
            val proxima = binding.etMedNext.text.toString()

            binding.tilMedName.error = null
            binding.tilMedDose.error = null

            if (nombre.isBlank()) {
                binding.tilMedName.error = getString(R.string.msg_complete_fields)
                return@setOnClickListener
            }
            if (dosis.isBlank()) {
                binding.tilMedDose.error = getString(R.string.msg_complete_fields)
                return@setOnClickListener
            }

            binding.btnSaveMed.isEnabled = false // Prevenir duplicados
            viewModel.agregarMedicamento(nombre, dosis, frecuencia, proxima)
        }

        binding.etMedNext.setOnClickListener {
            val c = Calendar.getInstance()
            android.app.TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    binding.etMedNext.setText(sdf.format(calendar.time))
                },
                c[Calendar.HOUR_OF_DAY],
                c[Calendar.MINUTE],
                false
            ).show()
        }
    }

    private fun setupObservers() {
        viewModel.medicamentosResource.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val list = resource.data
                    adapter.submitList(list)
                    binding.progressBar.isVisible = false
                    binding.layoutEmptyMeds.isVisible = list.isEmpty()
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    val msg = resource.message
                    if (msg.contains("401") || msg.contains(other = "token", ignoreCase = true)) {
                        handleSessionExpired()
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                }
                else -> {}
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSaveMed.isEnabled = !isLoading
        }

        viewModel.operationResource.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(context, getString(R.string.msg_medication_saved), Toast.LENGTH_SHORT).show()
                    // PROGRAMAR ALERTA: Programamos una notificación para la próxima toma
                    scheduleMedicationAlarm(resource.data)
                    clearFields()
                    viewModel.resetOperationState()
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationState()
                }
                else -> {}
            }
        }
    }

    private fun scheduleMedicationAlarm(med: Medicamento) {
        try {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val timeDate = com.example.gestionturnosapp.util.DateUtils.parseTime(med.proximaToma) ?: return

            val timeCalendar = Calendar.getInstance().apply { time = timeDate }
            
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                putExtra("TITLE", getString(R.string.title_reminder_medication))
                putExtra("MESSAGE", getString(R.string.msg_reminder_medication, med.nombre, med.dosis))
                putExtra("TYPE", "MEDICAMENTO")
                putExtra("NOTIFICATION_ID", med.id.hashCode())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                med.id.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("Medicamentos", "Error scheduling alarm", e)
        }
    }

    private fun handleSessionExpired() {
        com.example.gestionturnosapp.data.UserManager.logout(requireContext())
        Toast.makeText(requireContext(), getString(R.string.msg_session_expired), Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.loginFragment, null, androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build())
    }

    private fun clearFields() {
        binding.etMedName.text?.clear()
        binding.etMedDose.text?.clear()
        binding.etMedFreq.text?.clear()
        binding.etMedNext.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

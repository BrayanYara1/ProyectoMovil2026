package com.example.gestionturnosapp.ui.turnos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentSolicitarTurnoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class SolicitarTurnoFragment : Fragment() {

    private var _binding: FragmentSolicitarTurnoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TurnosListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSolicitarTurnoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.usuarioActual?.let {
            binding.etPacienteNombre.setText(it.nombre)
        }

        val especialidadArg = arguments?.getString("especialidadNombre")
        especialidadArg?.let { esp ->
            binding.etMotivo.setText(getString(R.string.reason_consultation_for, esp))
        }

        setupObservers()
        setupValidationListeners()
        setupPickers()
        setupOnBackPressed()
        
        binding.etFecha.isFocusable = false
        binding.etHora.isFocusable = false

        binding.btnConfirmarTurno.setOnClickListener {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            if (validarCampos()) {
                val nombre = binding.etPacienteNombre.text.toString()
                val fecha = binding.etFecha.text.toString()
                val hora = binding.etHora.text.toString()
                val motivo = binding.etMotivo.text.toString()

                if (esFechaPasada(fecha, hora)) {
                    Snackbar.make(binding.root, R.string.msg_past_date, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.msg_confirm_booking_title)
                    .setMessage(getString(R.string.msg_confirm_booking_body, fecha, hora))
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        // Prevenir múltiples clics
                        binding.btnConfirmarTurno.isEnabled = false

                        viewModel.crearNuevoTurno(
                            nombre, fecha, hora, motivo, 
                            especialidad = especialidadArg,
                            doctor = getString(R.string.label_assigned_doctor)
                        )
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            }
        }
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hayCambiosSinGuardar()) {
                    showDiscardDialog()
                } else {
                    isEnabled = false
                    findNavController().popBackStack()
                }
            }
        })
    }

    private fun hayCambiosSinGuardar(): Boolean {
        val nombre = binding.etPacienteNombre.text.toString()
        val fecha = binding.etFecha.text.toString()
        val hora = binding.etHora.text.toString()
        val motivo = binding.etMotivo.text.toString()
        val especialidadArg = arguments?.getString("especialidadNombre")
        val motivoDefault = if (especialidadArg != null) getString(R.string.reason_consultation_for, especialidadArg) else ""
        return (nombre.isNotEmpty() && nombre != UserManager.usuarioActual?.nombre) || fecha.isNotEmpty() || hora.isNotEmpty() || (motivo.isNotEmpty() && motivo != motivoDefault)
    }

    private fun showDiscardDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.msg_discard_title)
            .setMessage(R.string.msg_discard_message)
            .setNegativeButton(R.string.btn_keep_editing, null)
            .setPositiveButton(R.string.btn_discard) { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }

    private fun validarCampos(): Boolean {
        var esValido = true
        if (binding.etPacienteNombre.text.isNullOrBlank()) {
            binding.tilPacienteNombre.error = getString(R.string.msg_complete_fields)
            esValido = false
        } else { binding.tilPacienteNombre.error = null }
        if (binding.etFecha.text.isNullOrBlank()) {
            binding.tilFecha.error = getString(R.string.msg_complete_fields)
            esValido = false
        } else { binding.tilFecha.error = null }
        if (binding.etHora.text.isNullOrBlank()) {
            binding.tilHora.error = getString(R.string.msg_complete_fields)
            esValido = false
        } else { binding.tilHora.error = null }
        if (binding.etMotivo.text.isNullOrBlank()) {
            binding.tilMotivo.error = getString(R.string.msg_complete_fields)
            esValido = false
        } else { binding.tilMotivo.error = null }
        return esValido
    }

    private fun esFechaPasada(fecha: String, hora: String): Boolean {
        return try {
            // Soportar ambos formatos por si acaso
            val formatStr = if (hora.contains("AM", ignoreCase = true) || hora.contains("PM", ignoreCase = true)) {
                "yyyy-MM-dd hh:mm a"
            } else {
                "yyyy-MM-dd HH:mm"
            }
            val sdf = java.text.SimpleDateFormat(formatStr, Locale.US)
            val fechaSeleccionada = sdf.parse("$fecha $hora")
            val ahora = Calendar.getInstance().time
            fechaSeleccionada?.before(ahora) ?: true
        } catch (_: Exception) { true }
    }

    private fun setupPickers() {
        binding.etFecha.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                binding.etFecha.setText(formattedDate)
                binding.tilFecha.error = null
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }

        binding.etHora.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                
                val sdf = java.text.SimpleDateFormat("hh:mm a", Locale.US)
                val formattedTime = sdf.format(calendar.time)
                
                binding.etHora.setText(formattedTime)
                binding.tilHora.error = null
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        }
    }

    private fun setupValidationListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarDebounced()
            }
        }
        binding.etFecha.addTextChangedListener(watcher)
        binding.etHora.addTextChangedListener(watcher)
    }

    private var availabilityCheckJob: kotlinx.coroutines.Job? = null
    private fun validarDebounced() {
        availabilityCheckJob?.cancel()
        availabilityCheckJob = viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            val fecha = binding.etFecha.text.toString().trim()
            val hora = binding.etHora.text.toString().trim()
            if (fecha.isNotEmpty() && hora.isNotEmpty()) {
                viewModel.verificarDisponibilidad(fecha, hora)
            }
        }
    }

    private fun setupObservers() {
        viewModel.isSlotAvailable.observe(viewLifecycleOwner) { disponible ->
            actualizarEstadoBotonDisponibilidad(disponible)
        }

        viewModel.createTurnoResource.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnConfirmarTurno.isEnabled = false
                    binding.btnConfirmarTurno.text = getString(R.string.btn_processing)
                }
                is Resource.Success -> {
                    Snackbar.make(binding.root, R.string.msg_appointment_success, Snackbar.LENGTH_LONG).show()
                    viewModel.resetNavegacion()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    binding.btnConfirmarTurno.isEnabled = true
                    actualizarEstadoBotonDisponibilidad(viewModel.isSlotAvailable.value)
                    val msg = resource.message
                    if (msg.contains("401") || msg.contains("token", true)) {
                        handleSessionExpired()
                    } else {
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    }
                }
                else -> {}
            }
        }
    }

    private fun handleSessionExpired() {
        UserManager.logout(requireContext())
        findNavController().navigate(R.id.loginFragment, null, androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build())
    }

    private fun actualizarEstadoBotonDisponibilidad(disponible: Boolean?) {
        when (disponible) {
            true -> {
                binding.btnConfirmarTurno.isEnabled = true
                binding.btnConfirmarTurno.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
                binding.btnConfirmarTurno.text = getString(R.string.msg_slot_available)
            }
            false -> {
                binding.btnConfirmarTurno.isEnabled = false
                binding.btnConfirmarTurno.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error))
                binding.btnConfirmarTurno.text = getString(R.string.msg_slot_occupied)
            }
            else -> {
                binding.btnConfirmarTurno.isEnabled = true
                binding.btnConfirmarTurno.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
                binding.btnConfirmarTurno.text = getString(R.string.btn_confirm_appointment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

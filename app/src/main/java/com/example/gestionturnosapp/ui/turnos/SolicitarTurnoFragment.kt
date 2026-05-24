package com.example.gestionturnosapp.ui.turnos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
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
import com.example.gestionturnosapp.util.Resource
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.FragmentSolicitarTurnoBinding
import com.example.gestionturnosapp.util.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class SolicitarTurnoFragment : Fragment() {

    private var _binding: FragmentSolicitarTurnoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TurnosListViewModel by activityViewModels()

    @Inject
    lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSolicitarTurnoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialData()
        setupObservers()
        setupValidationListeners()
        setupPickers()
        setupOnBackPressed()
        
        binding.etFecha.isFocusable = false
        binding.etHora.isFocusable = false

        binding.btnConfirmarTurno.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            val fecha = viewModel.formFecha.value ?: ""
            val hora = viewModel.formHora.value ?: ""

            if (DateUtils.isPastDateTime(fecha, hora)) {
                Snackbar.make(binding.root, R.string.msg_past_date, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            val displayFecha = DateUtils.formatDisplayDate(requireContext(), fecha)
            val displayHora = DateUtils.formatDisplayTime(hora)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.msg_confirm_booking_title)
                .setMessage(getString(R.string.msg_confirm_booking_body, displayFecha, displayHora))
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    val especialidadArg = arguments?.getString("especialidadNombre")
                    viewModel.crearNuevoTurno(
                        especialidad = especialidadArg,
                        doctor = getString(R.string.label_assigned_doctor)
                    )
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }

        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun setupInitialData() {
        val currentUser = userManager.getUser()
        if (viewModel.formPacienteNombre.value.isNullOrBlank()) {
            currentUser?.let {
                viewModel.formPacienteNombre.value = it.nombre
                binding.etPacienteNombre.setText(it.nombre)
            }
        } else {
            binding.etPacienteNombre.setText(viewModel.formPacienteNombre.value)
        }

        val especialidadArg = arguments?.getString("especialidadNombre")
        if (especialidadArg != null && viewModel.formMotivo.value.isNullOrBlank()) {
            val motivo = getString(R.string.reason_consultation_for, especialidadArg)
            viewModel.formMotivo.value = motivo
            binding.etMotivo.setText(motivo)
        } else {
            binding.etMotivo.setText(viewModel.formMotivo.value)
        }

        binding.etFecha.setText(viewModel.formFecha.value)
        binding.etHora.setText(viewModel.formHora.value)
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(enabled = true) {
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
        val nombre = binding.etPacienteNombre.text.toString().trim()
        val fecha = binding.etFecha.text.toString().trim()
        val hora = binding.etHora.text.toString().trim()
        val motivo = binding.etMotivo.text.toString().trim()
        
        val user = userManager.getUser()
        val especialidadArg = arguments?.getString("especialidadNombre")
        val motivoDefault = if (especialidadArg != null) getString(R.string.reason_consultation_for, especialidadArg) else ""
        
        return (nombre != (user?.nombre ?: "").trim() && nombre.isNotEmpty()) || 
               fecha.isNotEmpty() || 
               hora.isNotEmpty() || 
               (motivo != motivoDefault.trim() && motivo.isNotEmpty())
    }

    private fun showDiscardDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.msg_discard_title)
            .setMessage(R.string.msg_discard_message)
            .setNegativeButton(R.string.btn_keep_editing, null)
            .setPositiveButton(R.string.btn_discard) { _, _ ->
                viewModel.resetNavegacion() // Limpiar VM al descartar
                findNavController().popBackStack()
            }
            .show()
    }

    private fun setupPickers() {
        binding.etFecha.setOnClickListener {
            val c = Calendar.getInstance()
            // Intentar usar la fecha ya seleccionada si existe
            viewModel.formFecha.value?.takeIf { it.isNotBlank() }?.let { currentFecha ->
                try {
                    val parts = currentFecha.split("-")
                    if (parts.size == 3) {
                        c.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    }
                } catch (_: Exception) {}
            }

            DatePickerDialog(requireContext(), { _, year, month, day ->
                val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                binding.etFecha.setText(formattedDate)
                binding.tilFecha.error = null
            }, c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH]).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }

        binding.etHora.setOnClickListener {
            val c = Calendar.getInstance()
            // Intentar usar la hora ya seleccionada
            viewModel.formHora.value?.takeIf { it.isNotBlank() }?.let { currentHora ->
                DateUtils.parseTime(currentHora)?.let { date ->
                    c.time = date
                }
            }

            TimePickerDialog(requireContext(), { _, hour, minute ->
                val calendar = Calendar.getInstance()
                calendar[Calendar.HOUR_OF_DAY] = hour
                calendar[Calendar.MINUTE] = minute
                
                val sdf = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedTime = sdf.format(calendar.time)
                
                binding.etHora.setText(formattedTime)
                binding.tilHora.error = null
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        }
    }

    private fun setupValidationListeners() {
        binding.etPacienteNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { viewModel.formPacienteNombre.value = s.toString() }
        })
        binding.etFecha.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { 
                viewModel.formFecha.value = s.toString()
                validarDebounced() 
            }
        })
        binding.etHora.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { 
                viewModel.formHora.value = s.toString()
                validarDebounced() 
            }
        })
        binding.etMotivo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { viewModel.formMotivo.value = s.toString() }
        })
    }

    private var availabilityCheckJob: kotlinx.coroutines.Job? = null
    private fun validarDebounced() {
        availabilityCheckJob?.cancel()
        availabilityCheckJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(400)
            val fecha = viewModel.formFecha.value ?: ""
            val hora = viewModel.formHora.value ?: ""
            if (fecha.isNotEmpty() && hora.isNotEmpty()) {
                viewModel.verificarDisponibilidad(fecha, hora)
            }
        }
    }

    private fun setupObservers() {
        viewModel.isFormValid.observe(viewLifecycleOwner) { isValid ->
            binding.btnConfirmarTurno.isEnabled = isValid
            binding.btnConfirmarTurno.alpha = if (isValid) 1.0f else 0.6f
        }

        viewModel.isSlotAvailable.observe(viewLifecycleOwner) { disponible ->
            if (isAdded) actualizarEstadoBotonDisponibilidad(disponible)
        }

        viewModel.createTurnoResource.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnConfirmarTurno.isEnabled = false
                    binding.btnConfirmarTurno.text = getString(R.string.btn_processing)
                }
                is Resource.Success -> {
                    Snackbar.make(binding.root, getString(R.string.msg_appointment_success), Snackbar.LENGTH_LONG).show()
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
        userManager.logout()
        findNavController().navigate(R.id.loginFragment, null, androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build())
    }

    private fun actualizarEstadoBotonDisponibilidad(disponible: Boolean?) {
        val context = context ?: return
        when (disponible) {
            true -> {
                binding.btnConfirmarTurno.isEnabled = true
                binding.btnConfirmarTurno.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                binding.btnConfirmarTurno.text = getString(R.string.msg_slot_available)
            }
            false -> {
                binding.btnConfirmarTurno.isEnabled = false
                binding.btnConfirmarTurno.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error))
                binding.btnConfirmarTurno.text = getString(R.string.msg_slot_occupied)
            }
            else -> {
                binding.btnConfirmarTurno.isEnabled = false // Deshabilitado hasta que verifique
                binding.btnConfirmarTurno.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                binding.btnConfirmarTurno.text = getString(R.string.btn_confirm_appointment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.gestionturnosapp.ui.medicamentos

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
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.databinding.FragmentMedicamentosBinding

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

    private fun setupRecyclerView() {
        adapter = MedicamentosAdapter { med ->
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Medicamento")
                .setMessage("¿Deseas eliminar ${med.nombre} de tu lista?")
                .setPositiveButton("Eliminar") { _, _ ->
                    viewModel.eliminarMedicamento(med.id)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        binding.rvMedicamentos.layoutManager = LinearLayoutManager(context)
        binding.rvMedicamentos.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveMed.setOnClickListener {
            val nombre = binding.etMedName.text.toString()
            val dosis = binding.etMedDose.text.toString()
            val frecuencia = binding.etMedFreq.text.toString()
            val proxima = binding.etMedNext.text.toString()

            if (nombre.isNotBlank() && dosis.isNotBlank()) {
                viewModel.agregarMedicamento(nombre, dosis, frecuencia, proxima)
            } else {
                Toast.makeText(context, "Completa nombre y dosis", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etMedNext.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                binding.etMedNext.setText(String.format(java.util.Locale.US, "%02d:%02d", hour, minute))
            }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true).show()
        }
    }

    private fun setupObservers() {
        viewModel.medicamentosResource.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    adapter.submitList(resource.data)
                    binding.progressBar.isVisible = false
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    val msg = resource.message
                    if (msg.contains("401") || msg.contains("token", true)) {
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
                    Toast.makeText(context, "Medicamento guardado", Toast.LENGTH_SHORT).show()
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

    private fun handleSessionExpired() {
        com.example.gestionturnosapp.data.UserManager.logout(requireContext())
        Toast.makeText(requireContext(), "Tu sesión ha expirado", Toast.LENGTH_SHORT).show()
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

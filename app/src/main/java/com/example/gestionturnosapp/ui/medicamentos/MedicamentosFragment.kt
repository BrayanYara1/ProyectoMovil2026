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
        adapter = MedicamentosAdapter()
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
    }

    private fun setupObservers() {
        viewModel.medicamentos.observe(viewLifecycleOwner) { meds ->
            adapter.submitList(meds)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnSaveMed.isEnabled = !isLoading
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Medicamento guardado", Toast.LENGTH_SHORT).show()
                clearFields()
            } else {
                Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
        }
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

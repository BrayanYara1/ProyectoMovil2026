package com.example.gestionturnosapp.ui.estudios

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.databinding.FragmentEstudiosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import java.util.Locale

class EstudiosFragment : Fragment() {

    private var _binding: FragmentEstudiosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EstudiosViewModel by viewModels()
    private lateinit var adapter: EstudiosAdapter

    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null

    private var filterStart: String? = null
    private var filterEnd: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            dialogImageView?.let {
                it.isVisible = true
                it.load(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEstudiosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = EstudiosAdapter()
        binding.rvEstudios.layoutManager = LinearLayoutManager(context)
        binding.rvEstudios.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.etSearchEstudios.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadEstudios()
        }

        binding.fabAddEstudio.setOnClickListener {
            showAddEstudioDialog()
        }

        binding.chipDateStart.setOnClickListener {
            showDatePicker { date ->
                filterStart = date
                binding.chipDateStart.text = "Desde: $date"
                applyFilters()
            }
        }

        binding.chipDateEnd.setOnClickListener {
            showDatePicker { date ->
                filterEnd = date
                binding.chipDateEnd.text = "Hasta: $date"
                applyFilters()
            }
        }

        binding.btnClearFilters.setOnClickListener {
            filterStart = null
            filterEnd = null
            binding.chipDateStart.text = "Desde: Todas"
            binding.chipDateEnd.text = "Hasta: Todas"
            applyFilters()
        }
    }

    private fun applyFilters() {
        binding.btnClearFilters.isVisible = filterStart != null || filterEnd != null
        viewModel.setDateFilter(filterStart, filterEnd)
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(formatted)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupObservers() {
        viewModel.estudios.observe(viewLifecycleOwner) { resource ->
            binding.swipeRefresh.isRefreshing = resource is Resource.Loading
            
            when (resource) {
                is Resource.Success -> {
                    val list = resource.data
                    adapter.submitList(list)
                    binding.layoutEmpty.isVisible = list.isEmpty()
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.createResource.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(context, "Estudio guardado correctamente", Toast.LENGTH_SHORT).show()
                    viewModel.resetCreateState()
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                    viewModel.resetCreateState()
                }
                else -> {}
            }
        }
    }

    private fun showAddEstudioDialog() {
        selectedImageUri = null
        
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 20, 60, 0)
        }

        val etTitulo = EditText(requireContext()).apply { hint = "Título del estudio" }
        val etTipo = EditText(requireContext()).apply { hint = "Tipo (Análisis, Placa, etc)" }
        val etResultado = EditText(requireContext()).apply { hint = "Resultado breve" }
        val etFecha = EditText(requireContext()).apply {
            hint = "Fecha del estudio"
            isFocusable = false
            setText(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Calendar.getInstance().time))
            setOnClickListener {
                val c = java.util.Calendar.getInstance()
                android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                    setText(String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d))
                }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
            }
        }
        
        val btnPick = Button(requireContext()).apply {
            text = "Adjuntar Foto"
            setOnClickListener { pickImageLauncher.launch("image/*") }
        }

        dialogImageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply { topMargin = 20 }
            scaleType = ImageView.ScaleType.CENTER_CROP
            isVisible = false
        }

        layout.addView(etTitulo)
        layout.addView(etTipo)
        layout.addView(etResultado)
        layout.addView(etFecha)
        layout.addView(btnPick)
        layout.addView(dialogImageView)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo Estudio")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = etTitulo.text.toString()
                val tipo = etTipo.text.toString()
                val resultado = etResultado.text.toString()
                val fecha = etFecha.text.toString()
                
                if (titulo.isNotBlank()) {
                    viewModel.agregarEstudio(titulo, fecha, tipo, resultado, selectedImageUri?.toString())
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

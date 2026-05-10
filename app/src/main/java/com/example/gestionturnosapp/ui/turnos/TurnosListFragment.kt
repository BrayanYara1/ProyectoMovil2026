package com.example.gestionturnosapp.ui.turnos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Resource
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.databinding.FragmentTurnosListBinding

class TurnosListFragment : Fragment() {

    private var _binding: FragmentTurnosListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TurnosListViewModel by activityViewModels()
    private lateinit var adapter: TurnosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTurnosListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchView()
        setupFilters()
        setupObservers()
        setupFab()
        
        // Cargar turnos al iniciar si la lista está vacía o es la primera vez
        if (viewModel.turnosResource.value == null) {
            viewModel.fetchTurnos()
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, _ ->
            val status = when (group.checkedChipId) {
                R.id.chipPending -> "PENDIENTE"
                R.id.chipCompleted -> "COMPLETADO"
                else -> "TODOS"
            }
            viewModel.setFilterStatus(status)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = TurnosAdapter(
            onTurnoClick = { turno, _ ->
                val bundle = Bundle().apply {
                    putString("TURNO_ID", turno.id)
                    putString("PACIENTE_NOMBRE", turno.pacienteNombre)
                    putString("TURNO_FECHA", turno.fecha)
                    putString("TURNO_HORA", turno.hora)
                    putString("TURNO_MOTIVO", turno.motivo)
                    putString("TURNO_ESTADO", turno.estado)
                }
                findNavController().navigate(R.id.action_turnosListFragment_to_turnoDetailFragment, bundle)
            },
            onDeleteClick = { turno ->
                showDeleteConfirmation(turno)
            }
        )
        binding.rvTurnos.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchTurnos()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun showDeleteConfirmation(turno: Turno) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.title_delete_appointment))
            .setMessage("${getString(R.string.msg_delete_confirm)}: ${turno.pacienteNombre}?")
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                // Feedback háptico profesional
                view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                
                viewModel.eliminarTurno(turno.id)
                
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Turno cancelado",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun setupObservers() {
        viewModel.filteredTurnos.observe(viewLifecycleOwner) { turnos ->
            adapter.submitList(turnos)
            binding.layoutEmpty.isVisible = turnos.isEmpty() && (viewModel.turnosResource.value as? Resource.Success)?.data?.isEmpty() == true
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.turnosResource.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.isVisible = true
                    }
                    binding.layoutError.isVisible = false
                    binding.layoutEmpty.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    binding.layoutError.isVisible = false
                    binding.layoutEmpty.isVisible = resource.data.isEmpty()
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    
                    val errorMessage = resource.message
                    if (errorMessage.contains("401") || errorMessage.contains("token", true)) {
                        com.example.gestionturnosapp.data.UserManager.logout(requireContext())
                        findNavController().navigate(R.id.loginFragment)
                        Toast.makeText(requireContext(), "Sesión expirada", Toast.LENGTH_SHORT).show()
                        return@observe
                    }

                    if (adapter.currentList.isEmpty()) {
                        binding.layoutError.isVisible = true
                        binding.tvErrorMessage.text = "¡Ups! $errorMessage"
                        binding.layoutEmpty.isVisible = false
                    } else {
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root,
                            errorMessage,
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
                is Resource.Idle -> {
                    binding.progressBar.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddTurno.setOnClickListener {
            findNavController().navigate(R.id.action_turnosListFragment_to_solicitarTurnoFragment)
        }
        
        binding.btnRetry.setOnClickListener {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.fetchTurnos()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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
        
        viewModel.fetchTurnos(requireContext())
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
            onTurnoClick = { turno, bindingItems ->
                view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                val bundle = Bundle().apply {
                    putString("TURNO_ID", turno.id)
                    putString("PACIENTE_NOMBRE", turno.pacienteNombre)
                    putString("TURNO_FECHA", turno.fecha)
                    putString("TURNO_HORA", turno.hora)
                    putString("TURNO_MOTIVO", turno.motivo)
                    putString("TURNO_ESTADO", turno.estado)
                }
                
                val extras = androidx.navigation.fragment.FragmentNavigatorExtras(
                    bindingItems.dateIconCard to "date_container_shared"
                )
                
                findNavController().navigate(
                    R.id.action_turnosListFragment_to_turnoDetailFragment, 
                    bundle,
                    null,
                    extras
                )
            },
            onDeleteClick = { turno ->
                showDeleteConfirmation(turno)
            }
        )
        binding.rvTurnos.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchTurnos(requireContext())
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun showDeleteConfirmation(turno: Turno) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.title_delete_appointment))
            .setMessage("${getString(R.string.msg_delete_confirm)}: ${turno.pacienteNombre}?")
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                viewModel.eliminarTurno(turno.id)
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    getString(R.string.msg_cancel_success),
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun setupObservers() {
        viewModel.filteredTurnos.observe(viewLifecycleOwner) { turnos ->
            adapter.submitList(turnos) {
                // Forzar la animación de cascada cada vez que se actualiza la lista
                binding.rvTurnos.scheduleLayoutAnimation()
            }
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
                        handleSessionExpired()
                        return@observe
                    }

                    if (adapter.currentList.isEmpty()) {
                        binding.layoutError.isVisible = true
                        binding.tvErrorMessage.text = getString(R.string.msg_error_prefix, errorMessage)
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

    private fun handleSessionExpired() {
        com.example.gestionturnosapp.data.UserManager.logout(requireContext())
        Toast.makeText(requireContext(), R.string.msg_session_expired, Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.loginFragment, null, androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build())
    }

    private fun setupFab() {
        binding.fabAddTurno.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_turnosListFragment_to_solicitarTurnoFragment)
        }
        
        binding.btnRetry.setOnClickListener {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.fetchTurnos(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

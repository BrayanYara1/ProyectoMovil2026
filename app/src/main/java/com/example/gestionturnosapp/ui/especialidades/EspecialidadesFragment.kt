package com.example.gestionturnosapp.ui.especialidades

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.databinding.FragmentEspecialidadesBinding

class EspecialidadesFragment : Fragment() {

    private var _binding: FragmentEspecialidadesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EspecialidadesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEspecialidadesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        binding.rvEspecialidades.layoutManager = GridLayoutManager(context, 2)
    }

    private fun setupObservers() {
        viewModel.especialidades.observe(viewLifecycleOwner) { lista ->
            val adapter = EspecialidadesAdapter(lista) { especialidad ->
                val bundle = Bundle().apply {
                    putString("especialidadNombre", getString(especialidad.nombreRes))
                }
                findNavController().navigate(R.id.action_especialidadesFragment_to_solicitarTurnoFragment, bundle)
            }
            binding.rvEspecialidades.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

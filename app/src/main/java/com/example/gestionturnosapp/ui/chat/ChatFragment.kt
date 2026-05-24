package com.example.gestionturnosapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.util.Resource
import com.example.gestionturnosapp.data.model.Mensaje
import com.example.gestionturnosapp.databinding.FragmentChatBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupObservers()
        viewModel.fetchMensajes()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.rvChat.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.enviarMensaje(text)
                binding.etMessage.text?.clear()
            }
        }
    }

    private fun setupObservers() {
        viewModel.isAiAssistantMode.observe(viewLifecycleOwner) { isAi ->
            binding.toolbar.title = if (isAi) "Asistente Inteligente" else "Chat Médico"
            binding.tvTypingIndicator.text = if (isAi) "El asistente está analizando..." else "El doctor está escribiendo..."
        }

        viewModel.mensajes.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.isVisible = resource is Resource.Loading && adapter.currentList.isEmpty()
            
            when (resource) {
                is Resource.Success<List<Mensaje>> -> {
                    adapter.submitList(resource.data) {
                        binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                    }
                    binding.layoutEmptyChat.isVisible = resource.data.isEmpty()
                }
                is Resource.Error -> {
                    if (resource.message == "SESSION_EXPIRED") {
                        handleSessionExpired()
                    } else {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {}
            }
        }

        viewModel.isDoctorTyping.observe(viewLifecycleOwner) { isTyping ->
            binding.tvTypingIndicator.isVisible = isTyping
            if (isTyping) {
                binding.rvChat.scrollToPosition(adapter.itemCount - 1)
            }
        }

        viewModel.mensajeEnviado.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Error) {
                if (resource.message == "SESSION_EXPIRED") {
                    handleSessionExpired()
                } else {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

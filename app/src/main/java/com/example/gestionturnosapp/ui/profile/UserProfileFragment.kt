package com.example.gestionturnosapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import coil.load
import coil.transform.CircleCropTransformation
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.local.ImageStorageManager
import com.example.gestionturnosapp.util.Resource
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.data.model.Usuario
import com.example.gestionturnosapp.databinding.FragmentUserProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val userId = UserManager.usuarioActual?.id ?: "unknown"
            val savedPath = ImageStorageManager.saveProfileImage(requireContext(), userId, it)
            
            if (savedPath != null) {
                binding.ivProfileAvatar.load(savedPath) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                binding.ivProfileAvatar.imageTintList = null
                Toast.makeText(requireContext(), R.string.photo_updated, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.ivProfileAvatar.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            pickImageLauncher.launch("image/*")
        }

        binding.btnProfileSettings.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            findNavController().navigate(R.id.action_userProfileFragment_to_settingsFragment)
        }

        binding.btnEditProfile.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            mostrarDialogoEdicion()
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { usuario ->
            if (usuario != null) {
                binding.tvProfileName.text = usuario.nombre
                binding.tvProfileEmail.text = usuario.email
                binding.tvProfilePhone.text = usuario.telefono ?: getString(R.string.label_not_specified)
            } else {
                binding.tvProfileName.text = getString(R.string.loading_user)
            }
            updateAvatar()
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnEditProfile.isEnabled = false
                }
                is Resource.Success<*> -> {
                    binding.btnEditProfile.isEnabled = true
                    Toast.makeText(context, R.string.msg_profile_update_success, Toast.LENGTH_SHORT).show()
                    viewModel.resetUpdateStatus()
                }
                is Resource.Error -> {
                    binding.btnEditProfile.isEnabled = true
                    when (resource.message) {
                        "SESSION_EXPIRED" -> handleSessionExpired()
                        "OFFLINE_SAVED" -> {
                            Toast.makeText(context, R.string.msg_profile_sync_local, Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    viewModel.resetUpdateStatus()
                }
                else -> {
                    binding.btnEditProfile.isEnabled = true
                }
            }
        }
    }

    private fun updateAvatar() {
        val savedImageUri = ImageStorageManager.getProfileImageUri(requireContext())
        binding.ivProfileAvatar.load(savedImageUri) {
            crossfade(true)
            placeholder(R.drawable.ic_nav_profile)
            error(R.drawable.ic_nav_profile)
            transformations(CircleCropTransformation())
            listener(
                onSuccess = { _, _ -> binding.ivProfileAvatar.imageTintList = null },
                onError = { _, _ -> 
                    binding.ivProfileAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
                        requireContext().getColor(R.color.primary)
                    )
                }
            )
        }
    }

    private fun mostrarDialogoEdicion() {
        val usuario = viewModel.user.value
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.btn_edit_data)

        val viewInflated = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, view as ViewGroup?, false)
        val inputNombre = viewInflated.findViewById<EditText>(R.id.etEditName)
        val inputPhone = viewInflated.findViewById<EditText>(R.id.etEditPhone)

        inputNombre.setText(usuario?.nombre)
        inputPhone.setText(usuario?.telefono)

        inputPhone.addTextChangedListener(object : android.text.TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                val text = s.toString()
                if (text.isNotEmpty() && !text.startsWith("+57 ")) {
                    isUpdating = true
                    val clean = text.replace("+57", "").trim()
                    inputPhone.setText("+57 $clean")
                    inputPhone.setSelection(inputPhone.length())
                    isUpdating = false
                }
            }
        })

        builder.setView(viewInflated)

        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            val nuevoNombre = inputNombre.text.toString()
            val nuevoTelefono = inputPhone.text.toString()

            if (nuevoNombre.isNotEmpty()) {
                val nuevoUsuario = Usuario(
                    id = usuario?.id ?: "",
                    nombre = nuevoNombre,
                    email = usuario?.email ?: "",
                    telefono = nuevoTelefono
                )
                viewModel.updateProfile(nuevoUsuario)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.btn_cancel) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun handleSessionExpired() {
        UserManager.logout(requireContext())
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

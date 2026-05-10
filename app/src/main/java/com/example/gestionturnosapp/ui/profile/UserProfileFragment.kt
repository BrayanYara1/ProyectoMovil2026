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
import coil.load
import coil.transform.CircleCropTransformation
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.databinding.FragmentUserProfileBinding
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.data.ImageStorageManager
import com.example.gestionturnosapp.data.Usuario

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ImageStorageManager.saveProfileImageUri(requireContext(), it.toString())
            binding.ivProfileAvatar.load(it) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
            binding.ivProfileAvatar.imageTintList = null
            Toast.makeText(requireContext(), R.string.photo_updated, Toast.LENGTH_SHORT).show()
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

        cargarDatos()

        binding.ivProfileAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnProfileSettings.setOnClickListener {
            findNavController().navigate(R.id.action_userProfileFragment_to_settingsFragment)
        }

        binding.btnEditProfile.setOnClickListener {
            mostrarDialogoEdicion()
        }
    }

    private fun cargarDatos() {
        val usuario = UserManager.loadUser(requireContext())
        if (usuario != null) {
            binding.tvProfileName.text = usuario.nombre
            binding.tvProfileEmail.text = usuario.email
            binding.tvProfilePhone.text = usuario.telefono ?: "---"
        } else {
            binding.tvProfileName.text = getString(R.string.loading_user)
        }

        val savedImageUri = ImageStorageManager.getProfileImageUri(requireContext())
        if (savedImageUri != null) {
            binding.ivProfileAvatar.load(savedImageUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
            binding.ivProfileAvatar.imageTintList = null
        }
    }

    private fun mostrarDialogoEdicion() {
        val usuario = UserManager.loadUser(requireContext())
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.btn_edit_data)

        val viewInflated = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, view as ViewGroup?, false)
        val inputNombre = viewInflated.findViewById<EditText>(R.id.etEditName)
        val inputPhone = viewInflated.findViewById<EditText>(R.id.etEditPhone)

        inputNombre.setText(usuario?.nombre)
        inputPhone.setText(usuario?.telefono)

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
                UserManager.saveUser(requireContext(), nuevoUsuario)
                cargarDatos()
                Toast.makeText(context, R.string.msg_register_success, Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.btn_cancel) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

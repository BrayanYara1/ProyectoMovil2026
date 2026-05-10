package com.example.gestionturnosapp.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorageManager {
    private const val PREF_NAME = "image_prefs"
    private const val KEY_PROFILE_IMAGE_PREFIX = "profile_image_path_"

    /**
     * Guarda la imagen físicamente en el almacenamiento interno.
     */
    fun saveProfileImage(context: Context, userId: String, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            // Usamos un nombre de archivo único por usuario para que no se borre al cambiar de cuenta
            val file = File(context.filesDir, "profile_photo_$userId.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val savedPath = file.absolutePath
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PROFILE_IMAGE_PREFIX + userId, savedPath).apply()
            
            savedPath
        } catch (e: Exception) {
            android.util.Log.e("ImageStorage", "Error saving image", e)
            null
        }
    }

    /**
     * Obtiene la ruta de la imagen del perfil verificando que el archivo exista físicamente.
     */
    fun getProfileImageUri(context: Context, userId: String?): String? {
        if (userId.isNullOrEmpty()) return null
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedPath = prefs.getString(KEY_PROFILE_IMAGE_PREFIX + userId, null)
        
        return if (savedPath != null) {
            val file = File(savedPath)
            if (file.exists()) {
                Uri.fromFile(file).toString()
            } else {
                // Si la ruta en preferencias existe pero el archivo no, intentamos buscarlo por nombre estándar
                val fallbackFile = File(context.filesDir, "profile_photo_$userId.jpg")
                if (fallbackFile.exists()) Uri.fromFile(fallbackFile).toString() else null
            }
        } else {
            // Intento de recuperación final: buscar archivo por nombre directamente
            val fallbackFile = File(context.filesDir, "profile_photo_$userId.jpg")
            if (fallbackFile.exists()) Uri.fromFile(fallbackFile).toString() else null
        }
    }

    /**
     * Versión simplificada que usa el usuario actual de UserManager.
     */
    fun getProfileImageUri(context: Context): String? {
        // Intentamos cargar el usuario si no está en memoria
        val user = UserManager.getUser(context)
        return getProfileImageUri(context, user?.id)
    }
}

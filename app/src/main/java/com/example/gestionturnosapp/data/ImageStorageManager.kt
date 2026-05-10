package com.example.gestionturnosapp.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorageManager {
    private const val PREF_NAME = "image_prefs"
    private const val KEY_PROFILE_IMAGE_PREFIX = "profile_image_"

    /**
     * Guarda la imagen en el almacenamiento interno de la app para que sea permanente
     * y no dependa de permisos temporales de la galería.
     */
    fun saveProfileImage(context: Context, userId: String, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "profile_$userId.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val savedUri = Uri.fromFile(file).toString()
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PROFILE_IMAGE_PREFIX + userId, savedUri).apply()
            
            savedUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getProfileImageUri(context: Context, userId: String?): String? {
        if (userId == null) return null
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROFILE_IMAGE_PREFIX + userId, null)
    }

    // Mantener por compatibilidad o para el usuario actual sin ID conocido
    fun getProfileImageUri(context: Context): String? {
        val userId = UserManager.usuarioActual?.id
        return getProfileImageUri(context, userId)
    }
}

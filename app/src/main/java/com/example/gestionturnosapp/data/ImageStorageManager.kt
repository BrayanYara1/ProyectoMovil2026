package com.example.gestionturnosapp.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorageManager {
    private const val PREF_NAME = "image_prefs"
    private const val KEY_PROFILE_IMAGE_PREFIX = "profile_path_v2_"

    /**
     * Guarda la imagen físicamente. Usa un prefijo v2 para forzar una nueva carga limpia.
     */
    fun saveProfileImage(context: Context, userId: String, uri: Uri): String? {
        if (userId.isBlank()) return null
        val cleanId = userId.trim()
        
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "profile_$cleanId.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val savedPath = file.absolutePath
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROFILE_IMAGE_PREFIX + cleanId, savedPath)
                .apply()
            
            savedPath
        } catch (e: Exception) {
            android.util.Log.e("ImageStorage", "Error saving photo for $cleanId", e)
            null
        }
    }

    fun getProfileImageUri(context: Context, userId: String?): String? {
        if (userId.isNullOrBlank()) return null
        val cleanId = userId.trim()
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedPath = prefs.getString(KEY_PROFILE_IMAGE_PREFIX + cleanId, null)
        
        val file = if (savedPath != null) File(savedPath) else File(context.filesDir, "profile_$cleanId.jpg")
        
        return if (file.exists()) {
            Uri.fromFile(file).toString()
        } else {
            null
        }
    }

    fun getProfileImageUri(context: Context): String? {
        val user = UserManager.getUser(context)
        return getProfileImageUri(context, user?.id)
    }
}

package com.example.gestionturnosapp.data

import android.content.Context

object ImageStorageManager {
    private const val PREF_NAME = "image_prefs"
    private const val KEY_PROFILE_IMAGE = "profile_image_uri"

    fun saveProfileImageUri(context: Context, uri: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE_IMAGE, uri).apply()
    }

    fun getProfileImageUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROFILE_IMAGE, null)
    }
}

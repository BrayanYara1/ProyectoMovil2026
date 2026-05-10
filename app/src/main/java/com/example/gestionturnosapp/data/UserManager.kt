package com.example.gestionturnosapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

/**
 * Clase Singleton para administrar los datos del usuario logueado con seguridad.
 */
object UserManager {
    private const val PREF_NAME = "secure_user_prefs"
    private const val KEY_USER = "current_user"
    private const val KEY_TOKEN = "auth_token"
    
    private var appContext: Context? = null
    var usuarioActual: Usuario? = null
    var token: String? = null

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveUser(context: Context, usuario: Usuario, authToken: String? = null) {
        this.appContext = context.applicationContext
        usuarioActual = usuario
        val prefs = getEncryptedPrefs(context)
        val json = Gson().toJson(usuario)
        val editor = prefs.edit().putString(KEY_USER, json)
        if (authToken != null) {
            token = authToken
            editor.putString(KEY_TOKEN, authToken)
        }
        editor.apply()
    }

    fun getToken(context: Context? = null): String? {
        val targetContext = context ?: appContext
        if (token == null && targetContext != null) {
            val prefs = getEncryptedPrefs(targetContext)
            token = prefs.getString(KEY_TOKEN, null)
        }
        return token
    }

    fun getUser(context: Context): Usuario? {
        if (usuarioActual == null) {
            loadUser(context)
        }
        return usuarioActual
    }

    fun loadUser(context: Context): Usuario? {
        this.appContext = context.applicationContext
        val prefs = getEncryptedPrefs(context)
        val json = prefs.getString(KEY_USER, null)
        if (json != null) {
            try {
                usuarioActual = Gson().fromJson(json, Usuario::class.java)
            } catch (e: Exception) {
                // Si hay un error (por ejemplo, al cambiar de prefs normales a encriptadas)
                usuarioActual = null
                prefs.edit().remove(KEY_USER).apply()
            }
        }
        token = prefs.getString(KEY_TOKEN, null)
        return usuarioActual
    }

    fun logout(context: Context) {
        usuarioActual = null
        token = null
        val prefs = getEncryptedPrefs(context)
        prefs.edit().clear().apply()
    }
}

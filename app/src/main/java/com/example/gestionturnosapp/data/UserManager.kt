package com.example.gestionturnosapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

object UserManager {
    private const val PREF_NAME = "secure_user_prefs"
    private const val KEY_USER = "current_user"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_FCM_SYNCED = "fcm_synced"
    
    private var appContext: Context? = null
    var usuarioActual: Usuario? = null
    var token: String? = null

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return try {
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Si hay un error de cifrado (común al cambiar versiones o Keystore), 
            // borramos las preferencias corruptas para que la app no crashee.
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit { clear() }
            // Intentamos crear de nuevo
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun saveUser(context: Context, usuario: Usuario, authToken: String? = null) {
        this.appContext = context.applicationContext
        usuarioActual = usuario
        val prefs = getEncryptedPrefs(appContext!!)
        prefs.edit {
            putString(KEY_USER, Gson().toJson(usuario))
            if (authToken != null) {
                token = authToken
                putString(KEY_TOKEN, authToken)
            }
        }
    }

    fun getToken(context: Context? = null): String? {
        val targetContext = context?.applicationContext ?: appContext
        if (token == null && (targetContext != null)) {
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
        val prefs = getEncryptedPrefs(appContext!!)
        val json = prefs.getString(KEY_USER, null)
        if (json != null) {
            try {
                usuarioActual = Gson().fromJson(json, Usuario::class.java)
            } catch (_: Exception) {
                usuarioActual = null
                prefs.edit { remove(KEY_USER) }
            }
        }
        token = prefs.getString(KEY_TOKEN, null)
        return usuarioActual
    }

    fun saveFcmToken(context: Context, fcmToken: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit { 
            putString(KEY_FCM_TOKEN, fcmToken)
            putBoolean(KEY_FCM_SYNCED, false)
        }
    }

    fun getFcmToken(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_FCM_TOKEN, null)
    }

    fun markFcmAsSynced(context: Context) {
        getEncryptedPrefs(context).edit { putBoolean(KEY_FCM_SYNCED, true) }
    }

    fun isFcmSynced(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_FCM_SYNCED, false)
    }

    fun logout(context: Context) {
        usuarioActual = null
        token = null
        val prefs = getEncryptedPrefs(context)
        prefs.edit { clear() }
    }
}

package com.example.gestionturnosapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gestionturnosapp.data.model.Usuario
import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserManagerEntryPoint {
    fun userManager(): UserManager
}

@Singleton
class UserManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var usuarioActual: Usuario? = null
        private set
    
    var token: String? = null
        get() {
            if (field == null) {
                field = prefs.getString(KEY_TOKEN, null)
            }
            return field
        }
        private set

    init {
        loadUser()
    }

    fun saveUser(usuario: Usuario, authToken: String? = null) {
        usuarioActual = usuario
        prefs.edit {
            putString(KEY_USER, gson.toJson(usuario))
            if (authToken != null) {
                token = authToken
                putString(KEY_TOKEN, authToken)
            }
        }
    }

    fun getUser(): Usuario? {
        if (usuarioActual == null) {
            val json = prefs.getString(KEY_USER, null)
            if (json != null) {
                try {
                    usuarioActual = gson.fromJson(json, Usuario::class.java)
                } catch (_: Exception) {
                    usuarioActual = null
                    prefs.edit { remove(KEY_USER) }
                }
            }
        }
        return usuarioActual
    }

    private fun loadUser() {
        val json = prefs.getString(KEY_USER, null)
        if (json != null) {
            try {
                usuarioActual = gson.fromJson(json, Usuario::class.java)
            } catch (_: Exception) {
                usuarioActual = null
            }
        }
        token = prefs.getString(KEY_TOKEN, null)
    }

    fun saveFcmToken(fcmToken: String) {
        prefs.edit { 
            putString(KEY_FCM_TOKEN, fcmToken)
            putBoolean(KEY_FCM_SYNCED, false)
        }
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun markFcmAsSynced() {
        prefs.edit { putBoolean(KEY_FCM_SYNCED, true) }
    }

    fun isFcmSynced(): Boolean {
        return prefs.getBoolean(KEY_FCM_SYNCED, false)
    }

    fun logout() {
        usuarioActual = null
        token = null
        prefs.edit { clear() }
    }

    companion object {
        private const val PREF_NAME = "secure_user_prefs_diagnostics"
        private const val KEY_USER = "current_user"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_SYNCED = "fcm_synced"

        /**
         * Método puente para acceder a UserManager desde clases que no soportan inyección (legacy).
         */
        fun getUser(context: Context): Usuario? {
            val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, UserManagerEntryPoint::class.java)
            return entryPoint.userManager().getUser()
        }

        /**
         * Método puente para obtener el token desde clases legacy.
         */
        fun getToken(context: Context): String? {
            val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, UserManagerEntryPoint::class.java)
            return entryPoint.userManager().token
        }

        /**
         * Versión sin parámetros para RetrofitClient legacy.
         */
        fun getToken(): String? {
            return try {
                val context = com.example.gestionturnosapp.GestionTurnosApp.instance
                getToken(context)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Cierra la sesión (legacy).
         */
        fun logout(context: Context) {
            val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, UserManagerEntryPoint::class.java)
            entryPoint.userManager().logout()
        }

        /**
         * Usuario actual para compatibilidad legacy.
         */
        val usuarioActual: Usuario?
            get() = try {
                val context = com.example.gestionturnosapp.GestionTurnosApp.instance
                getUser(context)
            } catch (e: Exception) {
                null
            }
    }
}

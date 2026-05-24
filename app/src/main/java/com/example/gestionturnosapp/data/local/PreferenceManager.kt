package com.example.gestionturnosapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.appcompat.app.AppCompatDelegate

object PreferenceManager {
    private const val PREF_NAME = "app_settings_prefs"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_LOCALE = "app_locale"
    private const val KEY_BIOMETRIC = "biometric_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getLocale(context: Context): String {
        return getPrefs(context).getString(KEY_LOCALE, "es") ?: "es"
    }

    suspend fun setLocale(context: Context, localeTag: String) {
        getPrefs(context).edit { putString(KEY_LOCALE, localeTag) }
        
        // Limpiamos el caché para que los datos del servidor se recarguen en el nuevo idioma
        OfflineCacheManager.clearCache(context)

        val appLocale: androidx.core.os.LocaleListCompat = androidx.core.os.LocaleListCompat.forLanguageTags(localeTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun isDarkMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_DARK_MODE, enabled) }
        applyTheme(enabled)
    }

    fun applyTheme(enabled: Boolean) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BIOMETRIC, enabled) }
    }
}

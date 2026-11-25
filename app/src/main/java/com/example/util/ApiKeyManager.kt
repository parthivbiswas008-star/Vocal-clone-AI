package com.example.util

import android.content.Context
import com.example.BuildConfig

class ApiKeyManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getActiveApiKey(): String? {
        val custom = sanitize(prefs.getString(KEY_CUSTOM_API_KEY, null))
        if (isValidFormat(custom)) return custom

        val builtIn = sanitize(BuildConfig.GEMINI_API_KEY)
        if (isValidFormat(builtIn)) return builtIn

        return null
    }

    fun hasValidKey(): Boolean = getActiveApiKey() != null

    fun isUsingCustomKey(): Boolean {
        val custom = sanitize(prefs.getString(KEY_CUSTOM_API_KEY, null))
        return isValidFormat(custom)
    }

    fun hasBuiltInKey(): Boolean {
        val builtIn = sanitize(BuildConfig.GEMINI_API_KEY)
        return isValidFormat(builtIn)
    }

    fun saveCustomApiKey(rawKey: String) {
        val cleaned = sanitize(rawKey)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, cleaned).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
    }

    fun getMaskedKeyPreview(): String {
        val key = getActiveApiKey() ?: return "Not configured"
        return if (key.length > 10) {
            "${key.take(6)}••••••${key.takeLast(4)}"
        } else {
            "••••••••"
        }
    }

    companion object {
        private const val PREFS_NAME = "voice_clone_api_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val PLACEHOLDER_KEY = "MY_GEMINI_API_KEY"

        fun sanitize(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return raw.trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .trim()
        }

        fun isValidFormat(key: String): Boolean {
            return key.isNotBlank() &&
                !key.equals(PLACEHOLDER_KEY, ignoreCase = true) &&
                !key.contains("YOUR_API_KEY", ignoreCase = true) &&
                key.length >= 16
        }
    }
}

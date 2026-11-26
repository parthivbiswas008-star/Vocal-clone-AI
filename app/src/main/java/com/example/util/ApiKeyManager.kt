package com.example.util

import android.content.Context
import com.example.BuildConfig

class ApiKeyManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Gemini API Key (Voices, Accent DNA & Fallback TTS) ---
    fun getActiveApiKey(): String? {
        val custom = sanitize(prefs.getString(KEY_CUSTOM_API_KEY, null))
        if (isValidGeminiFormat(custom)) return custom

        val builtIn = sanitize(BuildConfig.GEMINI_API_KEY)
        if (isValidGeminiFormat(builtIn)) return builtIn

        return null
    }

    fun hasValidKey(): Boolean = getActiveApiKey() != null

    fun isUsingCustomKey(): Boolean {
        val custom = sanitize(prefs.getString(KEY_CUSTOM_API_KEY, null))
        return isValidGeminiFormat(custom)
    }

    fun hasBuiltInKey(): Boolean {
        val builtIn = sanitize(BuildConfig.GEMINI_API_KEY)
        return isValidGeminiFormat(builtIn)
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
        return maskKey(key)
    }

    // --- Cartesia AI API Key (True 1:1 Zero-Shot Voice Cloning & Sonic-3 TTS) ---
    fun getActiveCartesiaApiKey(): String? {
        val custom = sanitize(prefs.getString(KEY_CUSTOM_CARTESIA_API_KEY, null))
        if (isValidCartesiaFormat(custom)) return custom

        val builtIn = sanitize(BuildConfig.CARTESIA_API_KEY)
        if (isValidCartesiaFormat(builtIn)) return builtIn

        return null
    }

    fun hasValidCartesiaKey(): Boolean = getActiveCartesiaApiKey() != null

    fun isUsingCustomCartesiaKey(): Boolean {
        val custom = sanitize(prefs.getString(KEY_CUSTOM_CARTESIA_API_KEY, null))
        return isValidCartesiaFormat(custom)
    }

    fun hasBuiltInCartesiaKey(): Boolean {
        val builtIn = sanitize(BuildConfig.CARTESIA_API_KEY)
        return isValidCartesiaFormat(builtIn)
    }

    fun saveCustomCartesiaApiKey(rawKey: String) {
        val cleaned = sanitize(rawKey)
        prefs.edit().putString(KEY_CUSTOM_CARTESIA_API_KEY, cleaned).apply()
    }

    fun clearCustomCartesiaApiKey() {
        prefs.edit().remove(KEY_CUSTOM_CARTESIA_API_KEY).apply()
    }

    fun getMaskedCartesiaKeyPreview(): String {
        val key = getActiveCartesiaApiKey() ?: return "Not configured"
        return maskKey(key)
    }

    private fun maskKey(key: String): String {
        return if (key.length > 10) {
            "${key.take(6)}••••••${key.takeLast(4)}"
        } else {
            "••••••••"
        }
    }

    companion object {
        private const val PREFS_NAME = "voice_clone_api_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_CUSTOM_CARTESIA_API_KEY = "custom_cartesia_api_key"
        private const val PLACEHOLDER_GEMINI_KEY = "MY_GEMINI_API_KEY"
        private const val PLACEHOLDER_CARTESIA_KEY = "MY_CARTESIA_API_KEY"

        fun sanitize(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return raw.trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .trim()
        }

        fun isValidFormat(key: String): Boolean = isValidGeminiFormat(key)

        fun isValidGeminiFormat(key: String): Boolean {
            return key.isNotBlank() &&
                !key.equals(PLACEHOLDER_GEMINI_KEY, ignoreCase = true) &&
                !key.contains("YOUR_API_KEY", ignoreCase = true) &&
                key.length >= 16
        }

        fun isValidCartesiaFormat(key: String): Boolean {
            return key.isNotBlank() &&
                !key.equals(PLACEHOLDER_CARTESIA_KEY, ignoreCase = true) &&
                !key.contains("YOUR_CARTESIA", ignoreCase = true) &&
                !key.contains("YOUR_API_KEY", ignoreCase = true) &&
                key.length >= 12
        }
    }
}

package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppThemePreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val isLight: Boolean
) {
    COSMIC_GLASS("cosmic_glass", "Cosmic Glass", "Deep space neon glass", false),
    MIDNIGHT_AMOLED("midnight_amoled", "Midnight OLED", "Pure pitch-black studio", false),
    AURORA_ROYAL("aurora_royal", "Aurora Violet", "Luminous royal indigo", false),
    EMERALD_STUDIO("emerald_studio", "Emerald Console", "Warm dark forest console", false),
    DAYLIGHT_FROST("daylight_frost", "Daylight Frost", "Clean bright studio light", true);

    companion object {
        fun fromId(id: String?): AppThemePreset =
            entries.firstOrNull { it.id == id } ?: COSMIC_GLASS
    }
}

enum class AccentColorPreset(
    val id: String,
    val displayName: String
) {
    CYAN("cyan", "Cyan"),
    VIOLET("violet", "Violet"),
    CORAL("coral", "Coral"),
    GOLD("gold", "Gold"),
    EMERALD("emerald", "Mint");

    companion object {
        fun fromId(id: String?): AccentColorPreset =
            entries.firstOrNull { it.id == id } ?: CYAN
    }
}

data class AppSettings(
    val themePreset: AppThemePreset = AppThemePreset.COSMIC_GLASS,
    val accentPreset: AccentColorPreset = AccentColorPreset.CYAN,
    val preferCartesiaCloning: Boolean = true,
    val studioMasteringEnabled: Boolean = true,
    val autoPlayGenerated: Boolean = true,
    val highResRecording: Boolean = true,
    val showWaveforms: Boolean = true,
    val compactCards: Boolean = false,
    val defaultPlaybackSpeed: Float = 1.0f,
    val ultraHumanDefault: Boolean = true,
    val naturalBreathsDefault: Boolean = true
)

class AppSettingsManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    private fun loadSettings(): AppSettings {
        return AppSettings(
            themePreset = AppThemePreset.fromId(prefs.getString(KEY_THEME, null)),
            accentPreset = AccentColorPreset.fromId(prefs.getString(KEY_ACCENT, null)),
            preferCartesiaCloning = prefs.getBoolean(KEY_PREFER_CARTESIA, true),
            studioMasteringEnabled = prefs.getBoolean(KEY_STUDIO_MASTERING, true),
            autoPlayGenerated = prefs.getBoolean(KEY_AUTO_PLAY, true),
            highResRecording = prefs.getBoolean(KEY_HIGH_RES_REC, true),
            showWaveforms = prefs.getBoolean(KEY_SHOW_WAVEFORMS, true),
            compactCards = prefs.getBoolean(KEY_COMPACT_CARDS, false),
            defaultPlaybackSpeed = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f).coerceIn(0.75f, 2.0f),
            ultraHumanDefault = prefs.getBoolean(KEY_ULTRA_HUMAN, true),
            naturalBreathsDefault = prefs.getBoolean(KEY_NATURAL_BREATHS, true)
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putString(KEY_THEME, next.themePreset.id)
            .putString(KEY_ACCENT, next.accentPreset.id)
            .putBoolean(KEY_PREFER_CARTESIA, next.preferCartesiaCloning)
            .putBoolean(KEY_STUDIO_MASTERING, next.studioMasteringEnabled)
            .putBoolean(KEY_AUTO_PLAY, next.autoPlayGenerated)
            .putBoolean(KEY_HIGH_RES_REC, next.highResRecording)
            .putBoolean(KEY_SHOW_WAVEFORMS, next.showWaveforms)
            .putBoolean(KEY_COMPACT_CARDS, next.compactCards)
            .putFloat(KEY_DEFAULT_SPEED, next.defaultPlaybackSpeed)
            .putBoolean(KEY_ULTRA_HUMAN, next.ultraHumanDefault)
            .putBoolean(KEY_NATURAL_BREATHS, next.naturalBreathsDefault)
            .apply()
    }

    fun resetAppearance() {
        update {
            it.copy(
                themePreset = AppThemePreset.COSMIC_GLASS,
                accentPreset = AccentColorPreset.CYAN,
                showWaveforms = true,
                compactCards = false
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "voice_clone_app_settings"
        private const val KEY_THEME = "theme_preset"
        private const val KEY_ACCENT = "accent_preset"
        private const val KEY_PREFER_CARTESIA = "prefer_cartesia_cloning"
        private const val KEY_STUDIO_MASTERING = "studio_mastering"
        private const val KEY_AUTO_PLAY = "auto_play_generated"
        private const val KEY_HIGH_RES_REC = "high_res_recording"
        private const val KEY_SHOW_WAVEFORMS = "show_waveforms"
        private const val KEY_COMPACT_CARDS = "compact_cards"
        private const val KEY_DEFAULT_SPEED = "default_playback_speed"
        private const val KEY_ULTRA_HUMAN = "ultra_human_default"
        private const val KEY_NATURAL_BREATHS = "natural_breaths_default"
    }
}

package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gender: String,
    val accent: String,
    val audioPath: String?,
    val isPrebuilt: Boolean,
    val prebuiltCode: String? = null,
    val recordingLanguage: String = "English",
    val pitchTone: String = "Natural Warm",
    val paceSpeed: String = "1.0x",
    val detectedAccent: String? = null,
    val description: String = "",
    val language: String = "English",
    val voiceTraits: String = "",
    val estimatedAge: String = "",
    val speakingCadence: String = "",
    val confidenceScore: Int = 0,
    val audioMimeType: String = "audio/mp4",
    val pitchCalibration: Float = 1.0f,
    val eqProfile: String = "warm",
    val phoneticNotes: String = "",
    val secondaryPrebuiltCode: String? = null,
    val cartesiaVoiceId: String? = null,
    val cloneProvider: String = "gemini",
    val createdAt: Long = System.currentTimeMillis()
)

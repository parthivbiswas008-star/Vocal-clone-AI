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
    val recordingLanguage: String = "Bangla 🇧🇩",
    val pitchTone: String = "Natural Warm",
    val paceSpeed: String = "1.0x",
    val detectedAccent: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

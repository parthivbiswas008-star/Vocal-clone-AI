package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_audios")
data class GeneratedAudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val text: String,
    val language: String,
    val voiceProfileName: String,
    val audioPath: String,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

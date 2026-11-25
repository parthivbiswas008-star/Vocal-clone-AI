package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    // Voice Profiles
    @Query("SELECT * FROM voice_profiles ORDER BY createdAt DESC")
    fun getAllVoiceProfiles(): Flow<List<VoiceProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfile(profile: VoiceProfileEntity): Long

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteVoiceProfileById(id: Int)

    @Query("SELECT * FROM voice_profiles WHERE id = :id")
    suspend fun getVoiceProfileById(id: Int): VoiceProfileEntity?

    // Generated Audios
    @Query("SELECT * FROM generated_audios ORDER BY createdAt DESC")
    fun getAllGeneratedAudios(): Flow<List<GeneratedAudioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedAudio(audio: GeneratedAudioEntity): Long

    @Query("DELETE FROM generated_audios WHERE id = :id")
    suspend fun deleteGeneratedAudioById(id: Int)

    @Query("SELECT * FROM generated_audios WHERE id = :id")
    suspend fun getGeneratedAudioById(id: Int): GeneratedAudioEntity?
}

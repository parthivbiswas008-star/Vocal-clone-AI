package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    // Voice Profiles (custom clones first, then curated studio characters)
    @Query("SELECT * FROM voice_profiles ORDER BY isPrebuilt ASC, createdAt DESC")
    fun getAllVoiceProfiles(): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles WHERE isPrebuilt = 1")
    suspend fun getPrebuiltVoiceProfiles(): List<VoiceProfileEntity>

    @Query("DELETE FROM voice_profiles WHERE isPrebuilt = 1")
    suspend fun deletePrebuiltVoiceProfiles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfile(profile: VoiceProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfiles(profiles: List<VoiceProfileEntity>)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteVoiceProfileById(id: Int)

    @Query("SELECT * FROM voice_profiles WHERE id = :id")
    suspend fun getVoiceProfileById(id: Int): VoiceProfileEntity?

    @Query("UPDATE voice_profiles SET cartesiaVoiceId = :cartesiaVoiceId, cloneProvider = :cloneProvider WHERE id = :id")
    suspend fun updateCartesiaVoiceId(id: Int, cartesiaVoiceId: String, cloneProvider: String = "cartesia")

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

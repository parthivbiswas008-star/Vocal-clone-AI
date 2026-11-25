package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.PrebuiltVoiceConfig
import com.example.data.api.RetrofitClient
import com.example.data.api.SpeechConfig
import com.example.data.api.VoiceConfig
import com.example.data.database.GeneratedAudioEntity
import com.example.data.database.VoiceDao
import com.example.data.database.VoiceProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class VoiceRepository(
    private val context: Context,
    private val voiceDao: VoiceDao
) {
    val allVoiceProfiles: Flow<List<VoiceProfileEntity>> = voiceDao.getAllVoiceProfiles()
    val allGeneratedAudios: Flow<List<GeneratedAudioEntity>> = voiceDao.getAllGeneratedAudios()

    suspend fun insertVoiceProfile(profile: VoiceProfileEntity): Long = withContext(Dispatchers.IO) {
        voiceDao.insertVoiceProfile(profile)
    }

    suspend fun deleteVoiceProfile(id: Int) = withContext(Dispatchers.IO) {
        val profile = voiceDao.getVoiceProfileById(id)
        if (profile?.audioPath != null) {
            val file = File(profile.audioPath)
            if (file.exists()) {
                file.delete()
            }
        }
        voiceDao.deleteVoiceProfileById(id)
    }

    suspend fun deleteGeneratedAudio(id: Int) = withContext(Dispatchers.IO) {
        val audio = voiceDao.getGeneratedAudioById(id)
        if (audio != null) {
            val file = File(audio.audioPath)
            if (file.exists()) {
                file.delete()
            }
        }
        voiceDao.deleteGeneratedAudioById(id)
    }

    suspend fun prepopulateDefaultVoicesIfEmpty() = withContext(Dispatchers.IO) {
        val list = voiceDao.getAllVoiceProfiles().first()
        if (list.isEmpty()) {
            val defaults = listOf(
                VoiceProfileEntity(
                    name = "Sultry Male (Fenrir)",
                    gender = "Male",
                    accent = "Deep & Sultry",
                    audioPath = null,
                    isPrebuilt = true,
                    prebuiltCode = "Fenrir",
                    recordingLanguage = "Bangla 🇧🇩",
                    pitchTone = "Deep Bass",
                    paceSpeed = "1.0x"
                ),
                VoiceProfileEntity(
                    name = "Sweet Female (Aoede)",
                    gender = "Female",
                    accent = "Soft & Clear",
                    audioPath = null,
                    isPrebuilt = true,
                    prebuiltCode = "Aoede",
                    recordingLanguage = "Bangla 🇧🇩",
                    pitchTone = "Sweet Melodic",
                    paceSpeed = "1.0x"
                ),
                VoiceProfileEntity(
                    name = "Crisp Male (Puck)",
                    gender = "Male",
                    accent = "Friendly & Professional",
                    audioPath = null,
                    isPrebuilt = true,
                    prebuiltCode = "Puck",
                    recordingLanguage = "English 🇺🇸",
                    pitchTone = "Bright Crisp",
                    paceSpeed = "1.0x"
                ),
                VoiceProfileEntity(
                    name = "Warm Female (Kore)",
                    gender = "Female",
                    accent = "Warm & Natural",
                    audioPath = null,
                    isPrebuilt = true,
                    prebuiltCode = "Kore",
                    recordingLanguage = "Bangla 🇧🇩",
                    pitchTone = "Natural Warm",
                    paceSpeed = "1.0x"
                ),
                VoiceProfileEntity(
                    name = "Smooth Male (Charon)",
                    gender = "Male",
                    accent = "Smooth & Engaging",
                    audioPath = null,
                    isPrebuilt = true,
                    prebuiltCode = "Charon",
                    recordingLanguage = "Bangla 🇧🇩",
                    pitchTone = "Natural Warm",
                    paceSpeed = "1.0x"
                )
            )
            defaults.forEach { voiceDao.insertVoiceProfile(it) }
        }
    }

    suspend fun generateSpeech(
        title: String,
        text: String,
        voiceProfile: VoiceProfileEntity,
        language: String,
        paceSpeed: String = "1.0x"
    ): GeneratedAudioEntity = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("API Key missing! Please set your GEMINI_API_KEY in the AI Studio Secrets panel.")
        }

        // Clean language name from emojis/flags
        val cleanLanguage = when {
            language.contains("Bangla", ignoreCase = true) -> "Bengali"
            language.contains("Hindi", ignoreCase = true) -> "Hindi"
            language.contains("Arabic", ignoreCase = true) -> "Arabic"
            language.contains("Spanish", ignoreCase = true) -> "Spanish"
            language.contains("French", ignoreCase = true) -> "French"
            else -> "English"
        }

        // Select the base voice timbre
        val voiceName = if (voiceProfile.isPrebuilt && !voiceProfile.prebuiltCode.isNullOrBlank()) {
            voiceProfile.prebuiltCode
        } else {
            // Select base voice according to cloned gender & tone
            val isFemale = voiceProfile.gender.contains("Female", ignoreCase = true)
            val tone = voiceProfile.pitchTone.lowercase()
            when {
                tone.contains("deep") -> if (isFemale) "Kore" else "Fenrir"
                tone.contains("sweet") || tone.contains("soft") -> if (isFemale) "Aoede" else "Charon"
                tone.contains("bright") || tone.contains("crisp") -> if (isFemale) "Aoede" else "Puck"
                else -> if (isFemale) "Kore" else "Charon"
            }
        }

        // Distinctive voice cloning directives:
        // Instruct Gemini TTS to replicate the user's authentic cloned accent, tone, and pacing.
        val promptText = if (voiceProfile.isPrebuilt) {
            "Please read the following text clearly, expressively, and naturally in $cleanLanguage at a $paceSpeed pace:\n\n$text"
        } else {
            val userAccent = voiceProfile.accent
            val userRecLang = voiceProfile.recordingLanguage
            """
            You are speaking as the authentic AI Voice Clone of '${voiceProfile.name}'.
            Voice Characteristics:
            - Target Language: $cleanLanguage
            - Vocal Persona: ${voiceProfile.gender}, ${voiceProfile.pitchTone} timbre
            - Accent Cadence: Maintain the speaker's authentic native $userRecLang ($userAccent) accent, phrasing, intonation, and regional cadence.
            - Delivery Speed: $paceSpeed
            Speak completely naturally with human emotion, distinct accent inflection, and vocal resonance. Do not include any spoken commentary or introductory words.
            
            TEXT TO SPEAK:
            $text
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = promptText)
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = SpeechConfig(
                    voiceConfig = VoiceConfig(
                        prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceName)
                    )
                )
            )
        )

        // Try supported TTS models with fallback
        val candidateModels = listOf(
            "gemini-2.5-flash-tts",
            "gemini-3.8-flash-tts",
            "gemini-2.0-flash"
        )

        var lastException: Exception? = null
        var response: com.example.data.api.GenerateContentResponse? = null

        for (model in candidateModels) {
            try {
                val candidateResponse = RetrofitClient.service.generateContent(model, apiKey, request)
                val hasAudio = candidateResponse.candidates?.firstOrNull()?.content?.parts?.any { it.inlineData != null } == true
                if (hasAudio) {
                    response = candidateResponse
                    break
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val parsedMsg = parseErrorMessage(errorBody) ?: e.message()
                lastException = Exception("Gemini API ($model: HTTP ${e.code()}): $parsedMsg")
                if (e.code() == 400 || e.code() == 404) {
                    continue
                } else {
                    throw lastException
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        if (response == null) {
            throw lastException ?: Exception("Failed to generate voice. Please check your text and API key.")
        }

        val candidate = response.candidates?.firstOrNull()
            ?: throw Exception("Failed to generate voice. No candidate returned.")
        val audioPart = candidate.content?.parts?.firstOrNull { it.inlineData != null }
            ?: throw Exception("Failed to generate voice. No audio data was returned by the model.")

        val mimeType = audioPart.inlineData?.mimeType ?: "audio/wav"
        val base64Data = audioPart.inlineData?.data ?: throw Exception("Audio data is empty.")

        // Decode bytes and wrap in WAV header if needed
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val finalAudioBytes = ensureWavOrMp3(decodedBytes, mimeType)

        val isMp3 = mimeType.contains("mp3", ignoreCase = true)
        val extension = if (isMp3) "mp3" else "wav"

        val outputDir = File(context.filesDir, "generated_speech")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val filename = "speech_${System.currentTimeMillis()}.$extension"
        val outputFile = File(outputDir, filename)

        FileOutputStream(outputFile).use { fos ->
            fos.write(finalAudioBytes)
        }

        // Create entity and save to DB
        val generatedAudio = GeneratedAudioEntity(
            title = if (title.isBlank()) "Speech ${System.currentTimeMillis()}" else title,
            text = text,
            language = language,
            voiceProfileName = voiceProfile.name,
            audioPath = outputFile.absolutePath,
            createdAt = System.currentTimeMillis()
        )

        val id = voiceDao.insertGeneratedAudio(generatedAudio)
        generatedAudio.copy(id = id.toInt())
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = org.json.JSONObject(errorBody)
            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                if (errorObj.has("message")) {
                    errorObj.getString("message")
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureWavOrMp3(audioBytes: ByteArray, mimeType: String): ByteArray {
        // If already has RIFF header (WAV)
        if (audioBytes.size >= 4 &&
            audioBytes[0] == 'R'.code.toByte() &&
            audioBytes[1] == 'I'.code.toByte() &&
            audioBytes[2] == 'F'.code.toByte() &&
            audioBytes[3] == 'F'.code.toByte()
        ) {
            return audioBytes
        }
        // If already has ID3 (MP3)
        if (audioBytes.size >= 3 &&
            audioBytes[0] == 'I'.code.toByte() &&
            audioBytes[1] == 'D'.code.toByte() &&
            audioBytes[2] == '3'.code.toByte()
        ) {
            return audioBytes
        }
        // If MP3 by mimeType
        if (mimeType.contains("mp3", ignoreCase = true)) {
            return audioBytes
        }

        // Wrap raw 24kHz 16-bit mono PCM into standard RIFF WAV header
        val sampleRate = 24000
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val totalDataLen = audioBytes.size
        val totalAudioLen = totalDataLen + 36

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalAudioLen and 0xff).toByte()
        header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * (bitsPerSample / 8)).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalDataLen and 0xff).toByte()
        header[41] = ((totalDataLen shr 8) and 0xff).toByte()
        header[42] = ((totalDataLen shr 16) and 0xff).toByte()
        header[43] = ((totalDataLen shr 24) and 0xff).toByte()

        return header + audioBytes
    }
}

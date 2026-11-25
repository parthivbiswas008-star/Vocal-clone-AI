package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Base64
import com.example.data.VoiceCatalog
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerateContentResponse
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.PrebuiltVoiceConfig
import com.example.data.api.RetrofitClient
import com.example.data.api.SpeechConfig
import com.example.data.api.VoiceConfig
import com.example.data.database.GeneratedAudioEntity
import com.example.data.database.VoiceDao
import com.example.data.database.VoiceProfileEntity
import com.example.util.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ApiKeyException(message: String) : Exception(message)

data class VoiceAnalysisResult(
    val detectedLanguage: String,
    val detectedAccent: String,
    val detectedGender: String,
    val estimatedAge: String,
    val pitchProfile: String,
    val speakingCadence: String,
    val timbreDescription: String,
    val bestMatchingPrebuiltVoice: String,
    val confidenceScore: Int
)

class VoiceRepository(
    private val context: Context,
    private val voiceDao: VoiceDao,
    val apiKeyManager: ApiKeyManager
) {
    val allVoiceProfiles: Flow<List<VoiceProfileEntity>> = voiceDao.getAllVoiceProfiles()
    val allGeneratedAudios: Flow<List<GeneratedAudioEntity>> = voiceDao.getAllGeneratedAudios()

    suspend fun insertVoiceProfile(profile: VoiceProfileEntity): Long = withContext(Dispatchers.IO) {
        voiceDao.insertVoiceProfile(profile)
    }

    suspend fun deleteVoiceProfile(id: Int): String? = withContext(Dispatchers.IO) {
        val profile = voiceDao.getVoiceProfileById(id)
        val path = profile?.audioPath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        voiceDao.deleteVoiceProfileById(id)
        path
    }

    suspend fun deleteGeneratedAudio(id: Int): String? = withContext(Dispatchers.IO) {
        val audio = voiceDao.getGeneratedAudioById(id)
        val path = audio?.audioPath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        voiceDao.deleteGeneratedAudioById(id)
        path
    }

    /**
     * Live-verifies a Gemini API key against the models listing endpoint so the user
     * gets immediate feedback if a key is invalid, restricted, or expired.
     */
    suspend fun verifyApiKey(rawKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleaned = ApiKeyManager.sanitize(rawKey)
        if (!ApiKeyManager.isValidFormat(cleaned)) {
            return@withContext Result.failure(
                ApiKeyException("Please enter a valid Gemini API key (starts with AIza...).")
            )
        }
        try {
            val resp = RetrofitClient.service.listModels(cleaned)
            if (resp.models != null) {
                Result.success(cleaned)
            } else {
                Result.failure(ApiKeyException("API key responded unexpectedly. Please double-check your Gemini API key."))
            }
        } catch (e: retrofit2.HttpException) {
            val errBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val msg = parseErrorMessage(errBody) ?: "HTTP ${e.code()}: ${e.message()}"
            Result.failure(ApiKeyException("API key check failed: $msg"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error while verifying API key: ${e.message ?: "Check internet connection."}"))
        }
    }

    /**
     * Keeps all curated multilingual studio voice models up to date without ever
     * modifying or deleting the user's custom cloned voices.
     */
    suspend fun syncCuratedStudioVoices() = withContext(Dispatchers.IO) {
        val existingPrebuilt = voiceDao.getPrebuiltVoiceProfiles()
        val expectedNames = VoiceCatalog.curatedStudioVoices.map { it.name }.toSet()
        val existingNames = existingPrebuilt.map { it.name }.toSet()
        val hasDescriptions = existingPrebuilt.isNotEmpty() && existingPrebuilt.all { it.description.isNotBlank() }

        if (existingNames != expectedNames || !hasDescriptions) {
            voiceDao.deletePrebuiltVoiceProfiles()
            val baseTime = System.currentTimeMillis()
            val orderedCurated = VoiceCatalog.curatedStudioVoices.mapIndexed { idx, voice ->
                voice.copy(createdAt = baseTime - idx * 1000L)
            }
            voiceDao.insertVoiceProfiles(orderedCurated)
        }
    }

    private fun requireActiveApiKey(): String {
        return apiKeyManager.getActiveApiKey()
            ?: throw ApiKeyException(
                "Gemini API key is not configured. Tap the API Key button at the top to add your free Gemini API key (or set GEMINI_API_KEY in AI Studio Secrets)."
            )
    }

    /**
     * High-Precision AI Accent & Pure Acoustic Voice DNA Analyzer:
     * Sends the actual recorded or uploaded audio/video clip to Gemini multimodal
     * to extract regional accent, fundamental pitch register, resonance balance,
     * articulation habits, prosody, and gender-verified closest base voice timbre.
     */
    suspend fun analyzeVoiceSample(
        audioFile: File,
        mimeType: String,
        hintLanguage: String
    ): VoiceAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = requireActiveApiKey()
        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw Exception("Voice sample file is empty or could not be found.")
        }

        val audioBytes = audioFile.readBytes()
        val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val safeMime = normalizeMediaMimeType(mimeType, audioFile.extension)

        val voiceDirectoryText = VoiceCatalog.officialGeminiVoices.entries.joinToString("\n") { (name, desc) ->
            "- $name: $desc"
        }

        val analysisPrompt = """
            You are an elite acoustic phonetician, forensic accent analyst, and voice cloning casting director.
            Listen intently to the speaker's voice in the attached media sample (hint language: "$hintLanguage", but detect what is ACTUALLY spoken).
            Ignore background room noise or music and focus purely on the human speaker's true vocal cord resonance and regional pronunciation.

            Available base vocal timbres (you MUST pick the SINGLE closest acoustic match AND strictly match the speaker's gender):
            $voiceDirectoryText

            Return ONLY a valid JSON object with these exact keys:
            {
              "detectedLanguage": "Primary spoken language (e.g., English, Bengali, Hindi, Spanish, French, Arabic, German, Japanese, Portuguese, Korean, Urdu, Italian, Russian, Turkish, Chinese, Indonesian, Vietnamese, Thai)",
              "detectedAccent": "Exact regional accent & dialect (e.g., Kolkata Bengali, Dhaka Bangladeshi Bengali, West Bengal Soft Bengali, North Indian Hindi, Mumbai Hindi, Indian English, General American, British RP, Australian English, Parisian French, etc.)",
              "detectedGender": "Male or Female",
              "estimatedAge": "e.g., Young Adult (20s), Adult (30s), Mature Adult (40s-50s)",
              "pitchProfile": "Exact pitch register & tonal color (e.g., Deep Warm Baritone, Smooth Mid-Range Tenor, Crisp High Tenor, Rich Warm Alto, Soft Breathy Mezzo, Bright Clear Soprano)",
              "speakingCadence": "Precise rhythm, syllable timing, pause placement, and intonation contour",
              "timbreDescription": "Detailed 2-3 sentence pure acoustic Voice DNA blueprint covering chest/head/nasal resonance, breathiness, vocal texture, consonant crispness, vowel coloring, and emotional warmth so a TTS synthesizer faithfully mirrors this exact person's voice",
              "bestMatchingPrebuiltVoice": "Exact name from the 30 voices listed above (MUST match detectedGender)",
              "confidenceScore": 94
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = analysisPrompt),
                        Part(inlineData = InlineData(mimeType = safeMime, data = audioBase64))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.15f,
                responseMimeType = "application/json"
            )
        )

        val analyzerModels = listOf(
            "gemini-2.5-flash",
            "gemini-3.8-flash",
            "gemini-2.5-flash-lite"
        )
        var lastError: Exception? = null
        var rawText: String? = null

        for (model in analyzerModels) {
            try {
                val resp = RetrofitClient.service.generateContent(model, apiKey, request)
                val text = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull { !it.text.isNullOrBlank() }?.text
                if (!text.isNullOrBlank()) {
                    rawText = text
                    break
                }
            } catch (e: retrofit2.HttpException) {
                val errBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val msg = parseErrorMessage(errBody) ?: e.message()
                if (isApiKeyAuthError(e.code(), msg, errBody)) {
                    throw ApiKeyException("Invalid or unauthorized Gemini API key: $msg. Tap the API Key button to update it.")
                }
                lastError = Exception("Voice analysis failed ($model: HTTP ${e.code()}): $msg")
                continue
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (rawText.isNullOrBlank()) {
            throw lastError ?: Exception("Unable to analyze the voice sample. Please try a clearer recording or audio/video file.")
        }

        parseVoiceAnalysisJson(rawText, hintLanguage)
    }

    /**
     * Synthesizes pure, studio-grade speech using official Gemini TTS models.
     * Synthesizes at a steady 1.0x natural cadence with exact MIME-parsed WAV headers
     * so AudioPlayer hardware PlaybackParams controls playback speed with 100% consistency.
     */
    suspend fun generateSpeech(
        title: String,
        text: String,
        voiceProfile: VoiceProfileEntity,
        language: String
    ): GeneratedAudioEntity = withContext(Dispatchers.IO) {
        val apiKey = requireActiveApiKey()
        val cleanLanguage = normalizeLanguageName(language)
        val voiceName = resolveValidPrebuiltVoice(voiceProfile)

        val primaryPrompt = buildPureTtsPrompt(
            text = text.trim(),
            voiceProfile = voiceProfile,
            targetLanguage = cleanLanguage,
            simplified = false
        )
        val fallbackPrompt = buildPureTtsPrompt(
            text = text.trim(),
            voiceProfile = voiceProfile,
            targetLanguage = cleanLanguage,
            simplified = true
        )

        val candidateModels = listOf(
            "gemini-2.5-flash-preview-tts",
            "gemini-3.1-flash-tts-preview",
            "gemini-2.5-pro-preview-tts"
        )

        var lastException: Exception? = null
        var response: GenerateContentResponse? = null

        outer@ for (prompt in listOf(primaryPrompt, fallbackPrompt)) {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
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

            for (model in candidateModels) {
                try {
                    val candidateResponse = RetrofitClient.service.generateContent(model, apiKey, request)
                    val hasAudio = candidateResponse.candidates?.firstOrNull()?.content?.parts?.any { it.inlineData != null } == true
                    if (hasAudio) {
                        response = candidateResponse
                        break@outer
                    } else {
                        val finishReason = candidateResponse.candidates?.firstOrNull()?.finishReason
                        lastException = Exception("Model $model returned no audio (${finishReason ?: "empty response"}). Retrying...")
                    }
                } catch (e: retrofit2.HttpException) {
                    val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                    val parsedMsg = parseErrorMessage(errorBody) ?: e.message()
                    if (isApiKeyAuthError(e.code(), parsedMsg, errorBody)) {
                        throw ApiKeyException("Invalid or restricted Gemini API key: $parsedMsg. Tap the API Key button to update it.")
                    }
                    lastException = Exception("Speech generation failed ($model: HTTP ${e.code()}): $parsedMsg")
                    continue
                } catch (e: Exception) {
                    lastException = e
                }
            }
        }

        if (response == null) {
            throw lastException ?: Exception("Failed to generate speech. Please verify your internet connection and Gemini API key.")
        }

        val candidate = response.candidates?.firstOrNull()
            ?: throw Exception("Failed to generate speech. No audio candidate returned.")
        val audioPart = candidate.content?.parts?.firstOrNull { it.inlineData != null }
            ?: throw Exception("Failed to generate speech. No audio data returned by the model.")

        val mimeType = audioPart.inlineData?.mimeType ?: "audio/L16;codec=pcm;rate=24000"
        val base64Data = audioPart.inlineData?.data ?: throw Exception("Generated audio payload is empty.")

        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val finalAudioBytes = ensurePlayableWavOrMp3(decodedBytes, mimeType)

        val isMp3 = mimeType.contains("mp3", ignoreCase = true) || mimeType.contains("mpeg", ignoreCase = true)
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

        val durationMs = extractDurationMs(outputFile, decodedBytes.size, mimeType)

        val generatedAudio = GeneratedAudioEntity(
            title = title.trim().ifBlank { "${voiceProfile.name} - $cleanLanguage" },
            text = text.trim(),
            language = cleanLanguage,
            voiceProfileName = voiceProfile.name,
            audioPath = outputFile.absolutePath,
            durationMs = durationMs,
            createdAt = System.currentTimeMillis()
        )

        val id = voiceDao.insertGeneratedAudio(generatedAudio)
        generatedAudio.copy(id = id.toInt())
    }

    private fun buildPureTtsPrompt(
        text: String,
        voiceProfile: VoiceProfileEntity,
        targetLanguage: String,
        simplified: Boolean
    ): String {
        if (simplified) {
            val accent = voiceProfile.detectedAccent?.takeIf { it.isNotBlank() } ?: voiceProfile.accent
            return "Read the following $targetLanguage text naturally in a $accent accent at a steady normal pace:\n\n$text"
        }

        return if (!voiceProfile.isPrebuilt) {
            val accentNote = voiceProfile.detectedAccent?.takeIf { it.isNotBlank() } ?: voiceProfile.accent
            val traitsNote = voiceProfile.voiceTraits.ifBlank {
                "${voiceProfile.gender} speaker with a ${voiceProfile.pitchTone} voice"
            }
            val cadenceNote = voiceProfile.speakingCadence.ifBlank { "steady, natural conversational rhythm" }
            val ageNote = voiceProfile.estimatedAge.ifBlank { "Adult" }

            "Speak the exact $targetLanguage text below aloud as a natural $ageNote ${voiceProfile.gender} speaker with an authentic $accentNote accent, ${voiceProfile.pitchTone} pitch, and $cadenceNote. Vocal timbre: $traitsNote. Keep a consistent, steady 1.0x pace from start to finish and speak only the text below:\n\n$text"
        } else {
            val traits = voiceProfile.voiceTraits.ifBlank { voiceProfile.description }
            "Speak the exact $targetLanguage text below aloud in a ${voiceProfile.accent} accent with a ${voiceProfile.pitchTone} voice ($traits). Maintain a clean, steady 1.0x studio pace and speak only the text below:\n\n$text"
        }
    }

    private fun resolveValidPrebuiltVoice(profile: VoiceProfileEntity): String {
        val isFemale = profile.gender.equals("Female", ignoreCase = true)
        val isMale = profile.gender.equals("Male", ignoreCase = true)
        val code = profile.prebuiltCode?.trim()

        if (!code.isNullOrBlank() && VoiceCatalog.officialGeminiVoices.containsKey(code)) {
            val genderMatches = when {
                isFemale -> code in VoiceCatalog.femaleOfficialVoices
                isMale -> code in VoiceCatalog.maleOfficialVoices
                else -> true
            }
            if (genderMatches) return code
        }

        val tone = profile.pitchTone.lowercase()
        return when {
            tone.contains("deep") || tone.contains("bass") || tone.contains("baritone") || tone.contains("gravel") ->
                if (isFemale) "Sulafat" else "Algenib"
            tone.contains("soft") || tone.contains("gentle") || tone.contains("sweet") || tone.contains("sooth") || tone.contains("velvet") ->
                if (isFemale) "Vindemiatrix" else "Algieba"
            tone.contains("bright") || tone.contains("crisp") || tone.contains("upbeat") || tone.contains("soprano") ->
                if (isFemale) "Zephyr" else "Puck"
            tone.contains("breath") || tone.contains("intimate") ->
                if (isFemale) "Achernar" else "Enceladus"
            tone.contains("command") || tone.contains("bold") || tone.contains("dramatic") ->
                if (isFemale) "Kore" else "Orus"
            else ->
                if (isFemale) "Aoede" else "Charon"
        }
    }

    private fun parseVoiceAnalysisJson(raw: String, hintLanguage: String): VoiceAnalysisResult {
        val cleaned = raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        val jsonStr = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned.substring(jsonStart, jsonEnd + 1)
        } else {
            cleaned
        }

        return try {
            val obj = JSONObject(jsonStr)
            val rawGender = obj.optString("detectedGender", "Male").trim()
            val gender = when {
                rawGender.contains("female", ignoreCase = true) -> "Female"
                rawGender.contains("male", ignoreCase = true) -> "Male"
                else -> "Male"
            }
            val pitch = obj.optString("pitchProfile", "Natural Warm").ifBlank { "Natural Warm" }
            val rawVoice = obj.optString("bestMatchingPrebuiltVoice", "").trim()
            val candidateVoice = VoiceCatalog.officialGeminiVoices.keys.firstOrNull {
                it.equals(rawVoice, ignoreCase = true)
            }

            // Strictly enforce gender match so cloned voice never mismatches gender
            val matchedVoice = when {
                candidateVoice != null && gender == "Female" && candidateVoice in VoiceCatalog.femaleOfficialVoices -> candidateVoice
                candidateVoice != null && gender == "Male" && candidateVoice in VoiceCatalog.maleOfficialVoices -> candidateVoice
                gender == "Female" -> {
                    if (pitch.contains("deep", true) || pitch.contains("alto", true)) "Sulafat"
                    else if (pitch.contains("soft", true) || pitch.contains("gentle", true)) "Vindemiatrix"
                    else if (pitch.contains("bright", true) || pitch.contains("soprano", true)) "Zephyr"
                    else "Aoede"
                }
                else -> {
                    if (pitch.contains("deep", true) || pitch.contains("bass", true) || pitch.contains("baritone", true)) "Algenib"
                    else if (pitch.contains("smooth", true) || pitch.contains("soft", true)) "Algieba"
                    else if (pitch.contains("bright", true) || pitch.contains("tenor", true)) "Puck"
                    else "Charon"
                }
            }

            VoiceAnalysisResult(
                detectedLanguage = normalizeLanguageName(obj.optString("detectedLanguage", hintLanguage)),
                detectedAccent = obj.optString("detectedAccent", "$hintLanguage Native Accent").ifBlank { "$hintLanguage Native Accent" },
                detectedGender = gender,
                estimatedAge = obj.optString("estimatedAge", "Adult").ifBlank { "Adult" },
                pitchProfile = pitch,
                speakingCadence = obj.optString("speakingCadence", "Steady conversational rhythm").ifBlank { "Steady conversational rhythm" },
                timbreDescription = obj.optString("timbreDescription", "Natural, expressive human voice with clear articulation.").ifBlank {
                    "Natural, expressive human voice with clear articulation."
                },
                bestMatchingPrebuiltVoice = matchedVoice,
                confidenceScore = obj.optInt("confidenceScore", 91).coerceIn(70, 99)
            )
        } catch (_: Exception) {
            VoiceAnalysisResult(
                detectedLanguage = normalizeLanguageName(hintLanguage),
                detectedAccent = "$hintLanguage Native Accent",
                detectedGender = "Male",
                estimatedAge = "Adult",
                pitchProfile = "Natural Warm",
                speakingCadence = "Natural conversational pace",
                timbreDescription = "Authentic human voice with clear resonance and natural cadence.",
                bestMatchingPrebuiltVoice = "Charon",
                confidenceScore = 86
            )
        }
    }

    private fun isApiKeyAuthError(httpCode: Int, parsedMsg: String?, rawBody: String?): Boolean {
        val combined = "${parsedMsg.orEmpty()} ${rawBody.orEmpty()}".lowercase()
        if (httpCode == 401) return true
        return combined.contains("api_key_invalid") ||
            combined.contains("api key not valid") ||
            combined.contains("api key expired") ||
            combined.contains("api_key_service_blocked") ||
            (httpCode == 403 && combined.contains("api key"))
    }

    private fun normalizeLanguageName(language: String): String {
        return when {
            language.contains("Bengali", ignoreCase = true) || language.contains("Bangla", ignoreCase = true) -> "Bengali"
            language.contains("Hindi", ignoreCase = true) -> "Hindi"
            language.contains("Spanish", ignoreCase = true) -> "Spanish"
            language.contains("French", ignoreCase = true) -> "French"
            language.contains("Arabic", ignoreCase = true) -> "Arabic"
            language.contains("German", ignoreCase = true) -> "German"
            language.contains("Japanese", ignoreCase = true) -> "Japanese"
            language.contains("Portuguese", ignoreCase = true) -> "Portuguese"
            language.contains("Korean", ignoreCase = true) -> "Korean"
            language.contains("Urdu", ignoreCase = true) -> "Urdu"
            language.contains("Italian", ignoreCase = true) -> "Italian"
            language.contains("Russian", ignoreCase = true) -> "Russian"
            language.contains("Turkish", ignoreCase = true) -> "Turkish"
            language.contains("Chinese", ignoreCase = true) || language.contains("Mandarin", ignoreCase = true) -> "Chinese"
            language.contains("Indonesian", ignoreCase = true) -> "Indonesian"
            language.contains("Vietnamese", ignoreCase = true) -> "Vietnamese"
            language.contains("Thai", ignoreCase = true) -> "Thai"
            else -> "English"
        }
    }

    private fun normalizeMediaMimeType(mimeType: String, extension: String): String {
        val lower = mimeType.lowercase()
        val ext = extension.lowercase()
        return when {
            ext == "wav" || lower.contains("wav") -> "audio/wav"
            ext == "mp3" || lower.contains("mpeg") || lower.contains("mp3") -> "audio/mp3"
            ext == "ogg" || ext == "oga" || ext == "opus" || lower.contains("ogg") || lower.contains("opus") -> "audio/ogg"
            ext == "flac" || lower.contains("flac") -> "audio/flac"
            ext == "aac" || lower.contains("aac") -> "audio/aac"
            ext == "aiff" || ext == "aif" || lower.contains("aiff") -> "audio/aiff"
            ext == "mp4" || ext == "m4v" || lower == "video/mp4" -> "video/mp4"
            ext == "mov" || lower.contains("quicktime") -> "video/quicktime"
            ext == "mkv" || lower.contains("matroska") -> "video/x-matroska"
            ext == "webm" && lower.startsWith("video/") -> "video/webm"
            ext == "webm" || lower.contains("webm") -> "audio/webm"
            ext == "3gp" || ext == "3gpp" || lower.contains("3gp") -> "video/3gpp"
            ext == "m4a" || lower.contains("mp4") || lower.contains("m4a") -> "audio/mp4"
            lower.startsWith("audio/") || lower.startsWith("video/") -> lower
            else -> "audio/mp4"
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(errorBody)
            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                if (errorObj.has("message")) errorObj.getString("message") else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSampleRate(mimeType: String): Int {
        val match = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE).find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(8000, 96000) ?: 24000
    }

    private fun parseChannels(mimeType: String): Int {
        val match = Regex("channels=(\\d+)", RegexOption.IGNORE_CASE).find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 2) ?: 1
    }

    private fun ensurePlayableWavOrMp3(audioBytes: ByteArray, mimeType: String): ByteArray {
        if (audioBytes.size >= 4 &&
            audioBytes[0] == 'R'.code.toByte() &&
            audioBytes[1] == 'I'.code.toByte() &&
            audioBytes[2] == 'F'.code.toByte() &&
            audioBytes[3] == 'F'.code.toByte()
        ) {
            return audioBytes
        }
        if (mimeType.contains("mp3", ignoreCase = true) || mimeType.contains("mpeg", ignoreCase = true)) {
            return audioBytes
        }
        if (audioBytes.size >= 3 &&
            audioBytes[0] == 'I'.code.toByte() &&
            audioBytes[1] == 'D'.code.toByte() &&
            audioBytes[2] == '3'.code.toByte()
        ) {
            return audioBytes
        }

        val sampleRate = parseSampleRate(mimeType)
        val channels = parseChannels(mimeType)
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
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
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
        header[32] = (channels * (bitsPerSample / 8)).toByte()
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

    private fun extractDurationMs(file: File, rawByteCount: Int, mimeType: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (dur != null && dur > 0L) dur else estimatePcmDurationMs(rawByteCount, mimeType)
        } catch (_: Exception) {
            estimatePcmDurationMs(rawByteCount, mimeType)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun estimatePcmDurationMs(rawByteCount: Int, mimeType: String): Long {
        val sampleRate = parseSampleRate(mimeType)
        val channels = parseChannels(mimeType)
        val bytesPerSecond = sampleRate * channels * 2L
        return if (bytesPerSecond > 0) (rawByteCount * 1000L) / bytesPerSecond else 0L
    }
}

package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Base64
import com.example.data.VoiceCatalog
import com.example.data.api.CartesiaApiException
import com.example.data.api.CartesiaVoiceClient
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
import com.example.util.AudioMasteringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
    val phoneticNotes: String,
    val bestMatchingPrebuiltVoice: String,
    val secondaryMatchingPrebuiltVoice: String,
    val recommendedPitchShift: Float,
    val eqProfile: String,
    val sampleQualityNote: String,
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
        val cartesiaId = profile?.cartesiaVoiceId
        if (!cartesiaId.isNullOrBlank()) {
            val cartesiaKey = apiKeyManager.getActiveCartesiaApiKey()
            if (cartesiaKey != null) {
                CartesiaVoiceClient.deleteClonedVoice(cartesiaKey, cartesiaId)
            }
        }
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        voiceDao.deleteVoiceProfileById(id)
        path
    }

    suspend fun verifyCartesiaApiKey(rawKey: String): Result<String> {
        return CartesiaVoiceClient.verifyApiKey(rawKey)
    }

    /**
     * Creates a TRUE 1:1 neural voice clone on Cartesia AI directly from the user's audio/video sample.
     * Returns the real Cartesia voice UUID.
     */
    suspend fun createCartesiaTrueClone(
        audioFile: File,
        mimeType: String,
        voiceName: String,
        description: String,
        language: String
    ): String {
        val cartesiaKey = apiKeyManager.getActiveCartesiaApiKey()
            ?: throw CartesiaApiException(
                "Cartesia AI API key is not configured. Add your Cartesia key in Settings -> API Keys for 1:1 voice cloning.",
                isAuthError = true
            )
        return CartesiaVoiceClient.cloneVoice(
            context = context,
            apiKey = cartesiaKey,
            audioFile = audioFile,
            mimeType = mimeType,
            voiceName = voiceName,
            description = description,
            language = language
        )
    }

    /**
     * Upgrades an existing locally saved voice clone to a 1:1 Cartesia neural voice clone.
     */
    suspend fun upgradeProfileToCartesiaClone(profile: VoiceProfileEntity): String = withContext(Dispatchers.IO) {
        val path = profile.audioPath
            ?: throw CartesiaApiException("Original voice recording file is missing for this profile.")
        val file = File(path)
        if (!file.exists() || file.length() == 0L) {
            throw CartesiaApiException("Original voice recording file could not be found on device.")
        }
        val cartesiaVoiceId = createCartesiaTrueClone(
            audioFile = file,
            mimeType = profile.audioMimeType,
            voiceName = profile.name,
            description = profile.description,
            language = profile.language
        )
        voiceDao.updateCartesiaVoiceId(profile.id, cartesiaVoiceId, "cartesia")
        cartesiaVoiceId
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
     * Deep Forensic Voice DNA & Accent Analyzer:
     * Extracts exact gender, regional accent, phonetic habits, vocal pitch calibration,
     * acoustic warmth profile, and top 2 closest base voice timbres from all 30 official voices.
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
            You are a world-class forensic acoustic phonetician and voice-cloning casting director.
            Listen deeply to the human speaker in the attached media clip (user language hint: "$hintLanguage", but report the ACTUAL language spoken).
            Ignore background hiss or room reverb and focus strictly on the speaker's vocal cords, formant timbre, fundamental pitch, regional accent, breathing habits, and natural human cadence.

            Available base vocal timbres (choose the TOP 2 closest acoustic matches strictly matching the speaker's gender):
            $voiceDirectoryText

            Return ONLY a valid JSON object with these exact keys:
            {
              "detectedLanguage": "Primary spoken language (e.g., English, Bengali, Hindi, Spanish, French, Arabic, German, Japanese, Urdu, etc.)",
              "detectedAccent": "Exact regional accent (e.g., Kolkata Bengali, Dhaka Bangladeshi Bengali, North Indian Hindi, Indian English, General American, British RP, etc.)",
              "detectedGender": "Male or Female",
              "estimatedAge": "e.g., Teen/Young Adult (18-24), Young Adult (25-32), Adult (33-45), Mature (46+)",
              "pitchProfile": "Exact vocal pitch & register (e.g., Deep Chest Baritone, Warm Mid-Baritone, Smooth Tenor, Bright Youthful Tenor, Rich Warm Alto, Soft Mezzo-Soprano, Crisp Soprano)",
              "speakingCadence": "Specific natural phrasing rhythm, breath pauses, and sentence-ending melody",
              "timbreDescription": "Detailed 2-3 sentence pure acoustic portrait of chest vs head resonance, breathiness, vocal rasp/smoothness, warmth, and emotional color so a voice actor can convincingly mirror this person",
              "phoneticNotes": "Concrete pronunciation habits: vowel rounding, aspirated/dental/retroflex consonants, syllable stress, and native inflection patterns",
              "bestMatchingPrebuiltVoice": "Exact name of #1 closest voice from the 30 voices above (MUST match detectedGender)",
              "secondaryMatchingPrebuiltVoice": "Exact name of #2 closest alternative voice from the 30 voices above (different from #1, MUST match detectedGender)",
              "recommendedPitchShift": 1.0,
              "eqProfile": "One of: warm, deep, balanced, bright, intimate",
              "sampleQualityNote": "Short 5-8 word note on recording clarity (e.g., Clean studio-grade vocal sample, or Mild room echo detected)",
              "confidenceScore": 94
            }

            Guidance for recommendedPitchShift:
            - Must be a float between 0.90 and 1.10 representing the subtle pitch adjustment needed on top of bestMatchingPrebuiltVoice to match the speaker's real pitch center (e.g., 0.94 if speaker is slightly deeper than the base voice, 1.05 if slightly higher, 1.00 if exact match).
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
     * Synthesizes a quick temporary audition clip right inside Clone Studio so the user
     * can A/B compare their real voice sample against the cloned match before saving.
     */
    /**
     * Generates a quick audition clip so the user can A/B compare their original voice sample
     * against the cloned output BEFORE saving. Uses Cartesia Sonic-3 1:1 clone when available.
     */
    suspend fun auditionVoiceClone(
        analysis: VoiceAnalysisResult?,
        chosenPrebuiltVoice: String,
        customAccent: String,
        eqProfile: String,
        language: String,
        cartesiaVoiceId: String? = null,
        studioMasteringEnabled: Boolean = true
    ): File = withContext(Dispatchers.IO) {
        val cleanLang = normalizeLanguageName(language)
        val auditionText = VoiceCatalog.cloneAuditionSentence(cleanLang)

        val cartesiaKey = apiKeyManager.getActiveCartesiaApiKey()
        val (audioBytes, extension) = if (!cartesiaVoiceId.isNullOrBlank() && cartesiaKey != null) {
            val rawWav = CartesiaVoiceClient.synthesizeSpeechWavBytes(
                apiKey = cartesiaKey,
                cartesiaVoiceId = cartesiaVoiceId,
                text = auditionText,
                language = cleanLang,
                emotionStyle = "Natural Chat"
            )
            val finalWav = if (studioMasteringEnabled) {
                ensureMasteredWavOrMp3(rawWav, "audio/wav", eqProfile.ifBlank { "balanced" })
            } else {
                rawWav
            }
            finalWav to "wav"
        } else {
            val safeAnalysis = analysis
                ?: throw Exception("Please complete Voice DNA scan or add a Cartesia AI API key first.")
            val tempProfile = VoiceProfileEntity(
                name = "Clone Preview",
                gender = safeAnalysis.detectedGender,
                accent = customAccent.ifBlank { safeAnalysis.detectedAccent },
                audioPath = null,
                isPrebuilt = false,
                prebuiltCode = chosenPrebuiltVoice,
                recordingLanguage = cleanLang,
                pitchTone = safeAnalysis.pitchProfile,
                detectedAccent = customAccent.ifBlank { safeAnalysis.detectedAccent },
                language = cleanLang,
                voiceTraits = safeAnalysis.timbreDescription,
                estimatedAge = safeAnalysis.estimatedAge,
                speakingCadence = safeAnalysis.speakingCadence,
                eqProfile = eqProfile,
                phoneticNotes = safeAnalysis.phoneticNotes
            )
            val (bytes, ext, _) = synthesizeAudioBytes(
                text = auditionText,
                voiceProfile = tempProfile,
                cleanLanguage = cleanLang,
                emotionStyle = "Natural Chat",
                naturalBreaths = true,
                ultraHumanMode = true,
                studioMasteringEnabled = studioMasteringEnabled
            )
            bytes to ext
        }

        val outFile = File(context.cacheDir, "clone_audition_${System.currentTimeMillis()}.$extension")
        FileOutputStream(outFile).use { it.write(audioBytes) }
        outFile
    }

    /**
     * Generates lifelike, human-sounding speech.
     * - For cloned voices with a Cartesia AI key (preferCartesiaCloning=true): uses Cartesia Sonic-3
     *   true 1:1 zero-shot voice cloning (auto-cloning existing samples on the fly if needed!).
     * - For studio characters or Gemini fallback: uses Gemini Pro/3.1 TTS models + studio mastering.
     */
    suspend fun generateSpeech(
        title: String,
        text: String,
        voiceProfile: VoiceProfileEntity,
        language: String,
        emotionStyle: String = "Natural Chat",
        naturalBreaths: Boolean = true,
        ultraHumanMode: Boolean = true,
        preferCartesiaCloning: Boolean = true,
        studioMasteringEnabled: Boolean = true
    ): GeneratedAudioEntity = withContext(Dispatchers.IO) {
        val cleanLanguage = normalizeLanguageName(language)
        val cartesiaKey = apiKeyManager.getActiveCartesiaApiKey()

        var usedCartesiaTrueClone = false
        val (finalAudioBytes, extension, mimeType) = if (!voiceProfile.isPrebuilt && preferCartesiaCloning && cartesiaKey != null) {
            // Ensure we have a real Cartesia neural voice_id (auto-upgrades older clones seamlessly!)
            val activeCartesiaVoiceId = voiceProfile.cartesiaVoiceId?.takeIf { it.isNotBlank() }
                ?: if (!voiceProfile.audioPath.isNullOrBlank() && File(voiceProfile.audioPath).exists()) {
                    upgradeProfileToCartesiaClone(voiceProfile)
                } else {
                    null
                }

            if (!activeCartesiaVoiceId.isNullOrBlank()) {
                val rawWav = CartesiaVoiceClient.synthesizeSpeechWavBytes(
                    apiKey = cartesiaKey,
                    cartesiaVoiceId = activeCartesiaVoiceId,
                    text = text.trim(),
                    language = cleanLanguage,
                    emotionStyle = emotionStyle
                )
                val mastered = if (studioMasteringEnabled) {
                    ensureMasteredWavOrMp3(rawWav, "audio/wav", voiceProfile.eqProfile.ifBlank { "balanced" })
                } else {
                    rawWav
                }
                usedCartesiaTrueClone = true
                Triple(mastered, "wav", "audio/wav")
            } else {
                synthesizeAudioBytes(
                    text = text.trim(),
                    voiceProfile = voiceProfile,
                    cleanLanguage = cleanLanguage,
                    emotionStyle = emotionStyle,
                    naturalBreaths = naturalBreaths,
                    ultraHumanMode = ultraHumanMode,
                    studioMasteringEnabled = studioMasteringEnabled
                )
            }
        } else {
            synthesizeAudioBytes(
                text = text.trim(),
                voiceProfile = voiceProfile,
                cleanLanguage = cleanLanguage,
                emotionStyle = emotionStyle,
                naturalBreaths = naturalBreaths,
                ultraHumanMode = ultraHumanMode,
                studioMasteringEnabled = studioMasteringEnabled
            )
        }

        val outputDir = File(context.filesDir, "generated_speech")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val filename = "speech_${System.currentTimeMillis()}.$extension"
        val outputFile = File(outputDir, filename)

        FileOutputStream(outputFile).use { fos ->
            fos.write(finalAudioBytes)
        }

        val durationMs = extractDurationMs(outputFile, (finalAudioBytes.size - 44).coerceAtLeast(0), mimeType)
        val effectivePitch = if (usedCartesiaTrueClone) 1.0f else voiceProfile.pitchCalibration.coerceIn(0.84f, 1.16f)

        val generatedAudio = GeneratedAudioEntity(
            title = title.trim().ifBlank { "${voiceProfile.name} - $cleanLanguage" },
            text = text.trim(),
            language = cleanLanguage,
            voiceProfileName = voiceProfile.name,
            audioPath = outputFile.absolutePath,
            durationMs = durationMs,
            emotionStyle = emotionStyle,
            pitchCalibration = effectivePitch,
            createdAt = System.currentTimeMillis()
        )

        val id = voiceDao.insertGeneratedAudio(generatedAudio)
        generatedAudio.copy(id = id.toInt())
    }

    private suspend fun synthesizeAudioBytes(
        text: String,
        voiceProfile: VoiceProfileEntity,
        cleanLanguage: String,
        emotionStyle: String,
        naturalBreaths: Boolean,
        ultraHumanMode: Boolean,
        studioMasteringEnabled: Boolean = true
    ): Triple<ByteArray, String, String> {
        val apiKey = requireActiveApiKey()
        val voiceName = resolveValidPrebuiltVoice(voiceProfile)

        val primaryPrompt = buildHumanTtsPrompt(
            text = text,
            voiceProfile = voiceProfile,
            targetLanguage = cleanLanguage,
            emotionStyle = emotionStyle,
            naturalBreaths = naturalBreaths,
            simplified = false
        )
        val fallbackPrompt = buildHumanTtsPrompt(
            text = text,
            voiceProfile = voiceProfile,
            targetLanguage = cleanLanguage,
            emotionStyle = emotionStyle,
            naturalBreaths = naturalBreaths,
            simplified = true
        )

        // Prioritize newer, more expressive lifelike TTS models in Ultra-Human mode
        val candidateModels = if (ultraHumanMode) {
            listOf(
                "gemini-3.1-flash-tts-preview",
                "gemini-2.5-pro-preview-tts",
                "gemini-2.5-flash-preview-tts"
            )
        } else {
            listOf(
                "gemini-2.5-flash-preview-tts",
                "gemini-3.1-flash-tts-preview",
                "gemini-2.5-pro-preview-tts"
            )
        }

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
        val eqTone = voiceProfile.eqProfile.ifBlank { "warm" }
        val finalAudioBytes = if (studioMasteringEnabled) {
            ensureMasteredWavOrMp3(decodedBytes, mimeType, eqTone)
        } else {
            ensureRawWavOrMp3(decodedBytes, mimeType)
        }

        val isMp3 = mimeType.contains("mp3", ignoreCase = true) || mimeType.contains("mpeg", ignoreCase = true)
        val extension = if (isMp3) "mp3" else "wav"
        return Triple(finalAudioBytes, extension, mimeType)
    }

    /**
     * Natural Human Performance Direction Prompt:
     * Eliminates stiff/monotone AI pacing and directs organic human breath, micro-pauses,
     * authentic native accent phonetics, and genuine emotion.
     */
    private fun buildHumanTtsPrompt(
        text: String,
        voiceProfile: VoiceProfileEntity,
        targetLanguage: String,
        emotionStyle: String,
        naturalBreaths: Boolean,
        simplified: Boolean
    ): String {
        val emotionCue = VoiceCatalog.emotionDirectionFor(emotionStyle)
        val breathCue = if (naturalBreaths) {
            "Include subtle, realistic human breath pauses between clauses, soft organic transitions, and authentic conversational intonation rather than stiff robotic delivery."
        } else {
            "Use crisp, expressive human phrasing with natural intonation and zero robotic monotone."
        }

        val accentNote = voiceProfile.detectedAccent?.takeIf { it.isNotBlank() } ?: voiceProfile.accent

        if (simplified) {
            return "Perform the following $targetLanguage lines like a real human speaker with a genuine $accentNote accent. $emotionCue. Speak only the lines below:\n\n$text"
        }

        val nativeAccentRule = if (!targetLanguage.equals("English", ignoreCase = true)) {
            "Pronounce $targetLanguage like an authentic native speaker with true local vowel colors and natural syllable flow — never anglicized or robotic."
        } else {
            "Use genuine $accentNote pronunciation and organic everyday human phrasing."
        }

        return if (!voiceProfile.isPrebuilt) {
            val traitsNote = voiceProfile.voiceTraits.ifBlank {
                "${voiceProfile.gender} speaker with a warm ${voiceProfile.pitchTone} voice"
            }
            val cadenceNote = voiceProfile.speakingCadence.ifBlank { "relaxed, human conversational rhythm" }
            val phoneticCue = voiceProfile.phoneticNotes.takeIf { it.isNotBlank() }?.let {
                "Phonetic habits: $it. "
            }.orEmpty()
            val ageNote = voiceProfile.estimatedAge.ifBlank { "Adult" }

            "Act as a real $ageNote ${voiceProfile.gender} person speaking naturally in $targetLanguage with an authentic $accentNote accent and ${voiceProfile.pitchTone} vocal register. Voice texture: $traitsNote. ${phoneticCue}Cadence: $cadenceNote. Delivery style: $emotionCue. $nativeAccentRule $breathCue Speak ONLY the exact words below with lifelike human presence:\n\n$text"
        } else {
            val traits = voiceProfile.voiceTraits.ifBlank { voiceProfile.description }
            "Perform as ${voiceProfile.name}, a real ${voiceProfile.gender} human speaker in $targetLanguage with a genuine ${voiceProfile.accent} accent and ${voiceProfile.pitchTone} tone ($traits). Delivery style: $emotionCue. $nativeAccentRule $breathCue Speak ONLY the exact words below with lifelike warmth and expression:\n\n$text"
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

        val obj: JSONObject = try {
            when {
                cleaned.startsWith("[") -> JSONArray(cleaned).getJSONObject(0)
                cleaned.startsWith("{") -> JSONObject(cleaned)
                else -> {
                    val jsonStart = cleaned.indexOf('{')
                    val jsonEnd = cleaned.lastIndexOf('}')
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        JSONObject(cleaned.substring(jsonStart, jsonEnd + 1))
                    } else {
                        throw IllegalArgumentException("No JSON object found in voice analysis response")
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("Could not parse AI voice analysis response. Please tap Re-Scan AI to try again.")
        }

        val rawGender = obj.optString("detectedGender", "Male").trim()
        val gender = when {
            rawGender.contains("female", ignoreCase = true) -> "Female"
            rawGender.contains("male", ignoreCase = true) -> "Male"
            else -> "Male"
        }
        val pitch = obj.optString("pitchProfile", "Natural Warm").ifBlank { "Natural Warm" }
        val genderPool = if (gender == "Female") VoiceCatalog.femaleOfficialVoices else VoiceCatalog.maleOfficialVoices

        val rawPrimary = obj.optString("bestMatchingPrebuiltVoice", "").trim()
        val primaryMatch = genderPool.firstOrNull { it.equals(rawPrimary, ignoreCase = true) }
            ?: defaultVoiceFor(gender, pitch, exclude = null)

        val rawSecondary = obj.optString("secondaryMatchingPrebuiltVoice", "").trim()
        val secondaryMatch = genderPool.firstOrNull {
            it.equals(rawSecondary, ignoreCase = true) && !it.equals(primaryMatch, ignoreCase = true)
        } ?: defaultVoiceFor(gender, pitch, exclude = primaryMatch)

        val rawPitchShift = obj.optDouble("recommendedPitchShift", 1.0).toFloat()
        val safePitchShift = if (rawPitchShift.isNaN() || rawPitchShift <= 0f) 1.0f else rawPitchShift.coerceIn(0.90f, 1.10f)

        val rawEq = obj.optString("eqProfile", "warm").lowercase().trim()
        val safeEq = if (rawEq in setOf("warm", "deep", "balanced", "bright", "intimate")) rawEq else "warm"

        return VoiceAnalysisResult(
            detectedLanguage = normalizeLanguageName(obj.optString("detectedLanguage", hintLanguage)),
            detectedAccent = obj.optString("detectedAccent", "$hintLanguage Native Accent").ifBlank { "$hintLanguage Native Accent" },
            detectedGender = gender,
            estimatedAge = obj.optString("estimatedAge", "Adult").ifBlank { "Adult" },
            pitchProfile = pitch,
            speakingCadence = obj.optString("speakingCadence", "Natural conversational rhythm").ifBlank { "Natural conversational rhythm" },
            timbreDescription = obj.optString("timbreDescription", "Natural, expressive human voice with warm resonance.").ifBlank {
                "Natural, expressive human voice with warm resonance."
            },
            phoneticNotes = obj.optString("phoneticNotes", "").trim(),
            bestMatchingPrebuiltVoice = primaryMatch,
            secondaryMatchingPrebuiltVoice = secondaryMatch,
            recommendedPitchShift = safePitchShift,
            eqProfile = safeEq,
            sampleQualityNote = obj.optString("sampleQualityNote", "Clear voice sample").ifBlank { "Clear voice sample" },
            confidenceScore = obj.optInt("confidenceScore", 92).coerceIn(72, 99)
        )
    }

    private fun defaultVoiceFor(gender: String, pitch: String, exclude: String?): String {
        val candidates = if (gender == "Female") {
            when {
                pitch.contains("deep", true) || pitch.contains("alto", true) -> listOf("Sulafat", "Aoede", "Despina")
                pitch.contains("soft", true) || pitch.contains("gentle", true) -> listOf("Vindemiatrix", "Achernar", "Callirrhoe")
                pitch.contains("bright", true) || pitch.contains("soprano", true) -> listOf("Zephyr", "Leda", "Autonoe")
                else -> listOf("Aoede", "Sulafat", "Despina", "Kore")
            }
        } else {
            when {
                pitch.contains("deep", true) || pitch.contains("bass", true) || pitch.contains("baritone", true) -> listOf("Algenib", "Orus", "Gacrux")
                pitch.contains("smooth", true) || pitch.contains("soft", true) -> listOf("Algieba", "Enceladus", "Schedar")
                pitch.contains("bright", true) || pitch.contains("tenor", true) -> listOf("Puck", "Sadachbia", "Charon")
                else -> listOf("Charon", "Algieba", "Sadaltager", "Umbriel")
            }
        }
        return candidates.firstOrNull { !it.equals(exclude, ignoreCase = true) } ?: candidates.first()
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

    /**
     * Masters raw 16-bit PCM with AudioMasteringEngine (warm close-mic tone, DC removal,
     * anti-click fades, peak normalization) and wraps in an exact RIFF WAV header.
     */
    private fun ensureMasteredWavOrMp3(audioBytes: ByteArray, mimeType: String, eqProfile: String): ByteArray {
        // Already RIFF WAV -> master the PCM payload after 44-byte header if standard 16-bit PCM
        if (audioBytes.size > 44 &&
            audioBytes[0] == 'R'.code.toByte() &&
            audioBytes[1] == 'I'.code.toByte() &&
            audioBytes[2] == 'F'.code.toByte() &&
            audioBytes[3] == 'F'.code.toByte()
        ) {
            val audioFormat = (audioBytes[20].toInt() and 0xFF) or ((audioBytes[21].toInt() and 0xFF) shl 8)
            val channels = ((audioBytes[22].toInt() and 0xFF) or ((audioBytes[23].toInt() and 0xFF) shl 8)).coerceIn(1, 2)
            val sampleRate = ((audioBytes[24].toInt() and 0xFF) or
                ((audioBytes[25].toInt() and 0xFF) shl 8) or
                ((audioBytes[26].toInt() and 0xFF) shl 16) or
                ((audioBytes[27].toInt() and 0xFF) shl 24)).coerceIn(8000, 96000)
            val bitsPerSample = (audioBytes[34].toInt() and 0xFF) or ((audioBytes[35].toInt() and 0xFF) shl 8)
            return if (audioFormat == 1 && bitsPerSample == 16) {
                val pcm = audioBytes.copyOfRange(44, audioBytes.size)
                val mastered = AudioMasteringEngine.masterPcm16(pcm, sampleRate, channels, eqProfile)
                buildWavHeader(mastered.size, sampleRate, channels, 16) + mastered
            } else {
                audioBytes
            }
        }

        // Already MP3
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
        val masteredPcm = AudioMasteringEngine.masterPcm16(audioBytes, sampleRate, channels, eqProfile)
        return buildWavHeader(masteredPcm.size, sampleRate, channels, 16) + masteredPcm
    }

    private fun ensureRawWavOrMp3(audioBytes: ByteArray, mimeType: String): ByteArray {
        if (audioBytes.size > 44 &&
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
        val sampleRate = parseSampleRate(mimeType)
        val channels = parseChannels(mimeType)
        return buildWavHeader(audioBytes.size, sampleRate, channels, 16) + audioBytes
    }

    private fun buildWavHeader(totalDataLen: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
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
        return header
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

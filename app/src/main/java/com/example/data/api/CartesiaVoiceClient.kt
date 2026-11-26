package com.example.data.api

import android.content.Context
import com.example.util.MediaAudioTranscoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class CartesiaApiException(
    message: String,
    val isAuthError: Boolean = false
) : Exception(message)

/**
 * Production REST client for Cartesia AI (Sonic-3 / Sonic-2) true zero-shot 1:1 voice cloning
 * and ultra-lifelike speech synthesis.
 */
object CartesiaVoiceClient {
    private const val BASE_URL = "https://api.cartesia.ai"
    private const val CARTESIA_VERSION = "2025-04-16"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Verifies a user-provided Cartesia AI API key by querying GET /voices?limit=1.
     */
    suspend fun verifyApiKey(rawKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = rawKey.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (cleanKey.length < 12) {
            return@withContext Result.failure(
                CartesiaApiException("Cartesia API key looks too short. Copy the full key from play.cartesia.ai.", isAuthError = true)
            )
        }
        try {
            val request = Request.Builder()
                .url("$BASE_URL/voices?limit=1")
                .get()
                .header("Authorization", "Bearer $cleanKey")
                .header("X-API-Key", cleanKey)
                .header("Cartesia-Version", CARTESIA_VERSION)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> Result.success(cleanKey)
                    response.code == 401 || response.code == 403 -> {
                        Result.failure(
                            CartesiaApiException(
                                parseCartesiaError(bodyStr)
                                    ?: "Invalid Cartesia AI API key (HTTP ${response.code}). Please verify your key on play.cartesia.ai.",
                                isAuthError = true
                            )
                        )
                    }
                    else -> {
                        Result.failure(
                            CartesiaApiException(
                                "Cartesia verification failed (HTTP ${response.code}): ${parseCartesiaError(bodyStr) ?: response.message}"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(CartesiaApiException("Network error verifying Cartesia API key: ${e.message}"))
        }
    }

    /**
     * Uploads a user's voice recording or audio/video clip to Cartesia's POST /voices/clone endpoint
     * and returns the real neural voice UUID so synthesized speech matches the original speaker 1:1.
     */
    suspend fun cloneVoice(
        context: Context,
        apiKey: String,
        audioFile: File,
        mimeType: String,
        voiceName: String,
        description: String,
        language: String
    ): String = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw CartesiaApiException("Voice sample file is empty or missing.")
        }

        // Cartesia /voices/clone accepts clean WAV/MP3/M4A/OGG best.
        // If user uploaded video (MP4/MOV/WebM) or mobile codec (AMR/3GP/M4A), transcode to clean PCM WAV
        // so Cartesia captures 100% pure acoustic identity without container rejection.
        val prepared = prepareCleanClipForCartesia(context, audioFile, mimeType)
        val uploadFile = prepared.first
        val uploadMime = prepared.second
        val shouldDeleteTemp = prepared.third

        val langCode = mapLanguageToCartesiaIso(language)
        val safeName = voiceName.trim().ifBlank { "Cloned Voice" }.take(64)
        val safeDesc = description.trim().ifBlank { "1:1 voice clone created in AI Voice Clone Maker" }.take(240)

        try {
            // Attempt 1: Full similarity + enhance form (highest 1:1 fidelity on Cartesia 2025-04-16)
            val primaryAttempt = executeCloneRequest(
                apiKey = apiKey,
                clipFile = uploadFile,
                clipMime = uploadMime,
                name = safeName,
                description = safeDesc,
                languageCode = langCode,
                includeSimilarityExtras = true
            )
            if (primaryAttempt.isSuccess) {
                return@withContext primaryAttempt.getOrThrow()
            }

            val firstErr = primaryAttempt.exceptionOrNull()
            if (firstErr is CartesiaApiException && firstErr.isAuthError) {
                throw firstErr
            }

            // Attempt 2: Minimal standard form (forward-compatible across all Cartesia API versions)
            val fallbackAttempt = executeCloneRequest(
                apiKey = apiKey,
                clipFile = uploadFile,
                clipMime = uploadMime,
                name = safeName,
                description = safeDesc,
                languageCode = langCode,
                includeSimilarityExtras = false
            )
            return@withContext fallbackAttempt.getOrThrow()
        } finally {
            if (shouldDeleteTemp && uploadFile.exists()) {
                uploadFile.delete()
            }
        }
    }

    private fun executeCloneRequest(
        apiKey: String,
        clipFile: File,
        clipMime: String,
        name: String,
        description: String,
        languageCode: String,
        includeSimilarityExtras: Boolean
    ): Result<String> {
        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "clip",
                clipFile.name,
                clipFile.asRequestBody(clipMime.toMediaType())
            )
            .addFormDataPart("name", name)
            .addFormDataPart("description", description)
            .addFormDataPart("language", languageCode)

        if (includeSimilarityExtras) {
            multipartBuilder
                .addFormDataPart("mode", "similarity")
                .addFormDataPart("enhance", "true")
        }

        val request = Request.Builder()
            .url("$BASE_URL/voices/clone")
            .post(multipartBuilder.build())
            .header("Authorization", "Bearer $apiKey")
            .header("X-API-Key", apiKey)
            .header("Cartesia-Version", CARTESIA_VERSION)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val voiceId = json.optString("id", "").trim()
                    if (voiceId.isBlank()) {
                        Result.failure(CartesiaApiException("Cartesia clone succeeded but returned no voice ID."))
                    } else {
                        Result.success(voiceId)
                    }
                } else {
                    val isAuth = response.code == 401 || response.code == 403
                    val msg = parseCartesiaError(bodyStr) ?: "HTTP ${response.code}: ${response.message}"
                    Result.failure(CartesiaApiException("Cartesia voice cloning failed: $msg", isAuthError = isAuth))
                }
            }
        } catch (e: Exception) {
            Result.failure(CartesiaApiException("Cartesia voice cloning network error: ${e.message}"))
        }
    }

    /**
     * Synthesizes speech in a cloned Cartesia voice using Sonic-3 (with Sonic-2 fallback)
     * and returns high-definition 44.1 kHz 16-bit PCM WAV bytes.
     */
    suspend fun synthesizeSpeechWavBytes(
        apiKey: String,
        cartesiaVoiceId: String,
        text: String,
        language: String,
        emotionStyle: String = "Natural Chat"
    ): ByteArray = withContext(Dispatchers.IO) {
        val langCode = mapLanguageToCartesiaIso(language)
        val cartesiaEmotion = mapEmotionToCartesia(emotionStyle)

        // Progressive fallback matrix:
        // 1) sonic-3 with emotion + language (highest human realism & 42+ language support)
        // 2) sonic-3 clean (if specific emotion tag isn't accepted by account tier)
        // 3) sonic-2 standard (universal multilingual fallback)
        val attempts = listOf(
            Triple("sonic-3", true, langCode),
            Triple("sonic-3", false, langCode),
            Triple("sonic-2", false, langCode),
            Triple("sonic-3", false, null)
        )

        var lastError: Exception? = null

        for ((modelId, includeEmotion, lang) in attempts) {
            val payload = JSONObject().apply {
                put("model_id", modelId)
                put("transcript", text.trim())
                put("voice", JSONObject().apply {
                    put("mode", "id")
                    put("id", cartesiaVoiceId)
                })
                put("output_format", JSONObject().apply {
                    put("container", "wav")
                    put("encoding", "pcm_s16le")
                    put("sample_rate", 44100)
                })
                if (!lang.isNullOrBlank()) {
                    put("language", lang)
                }
                if (includeEmotion && modelId == "sonic-3") {
                    put("generation_config", JSONObject().apply {
                        put("speed", 1.0)
                        put("volume", 1.0)
                        if (cartesiaEmotion != null) {
                            put("emotion", cartesiaEmotion)
                        }
                    })
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL/tts/bytes")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Authorization", "Bearer $apiKey")
                .header("X-API-Key", apiKey)
                .header("Cartesia-Version", CARTESIA_VERSION)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.size > 44) {
                            return@withContext bytes
                        }
                        lastError = CartesiaApiException("Cartesia returned an empty audio stream.")
                    } else {
                        val errBody = response.body?.string().orEmpty()
                        val msg = parseCartesiaError(errBody) ?: "HTTP ${response.code}: ${response.message}"
                        if (response.code == 401 || response.code == 403) {
                            throw CartesiaApiException(
                                "Cartesia API key rejected: $msg. Update it in Settings -> API Keys.",
                                isAuthError = true
                            )
                        }
                        if (response.code == 402 || response.code == 429) {
                            throw CartesiaApiException("Cartesia account limit or quota reached: $msg")
                        }
                        lastError = CartesiaApiException("Cartesia TTS ($modelId) failed: $msg")
                    }
                }
            } catch (e: CartesiaApiException) {
                if (e.isAuthError) throw e
                lastError = e
            } catch (e: Exception) {
                lastError = CartesiaApiException("Cartesia network error: ${e.message}")
            }
        }

        throw lastError ?: CartesiaApiException("Cartesia speech synthesis failed.")
    }

    /**
     * Best-effort deletion of a cloned voice from Cartesia when deleted locally.
     */
    suspend fun deleteClonedVoice(apiKey: String, cartesiaVoiceId: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/voices/$cartesiaVoiceId")
                .delete()
                .header("Authorization", "Bearer $apiKey")
                .header("X-API-Key", apiKey)
                .header("Cartesia-Version", CARTESIA_VERSION)
                .build()
            client.newCall(request).execute().close()
        } catch (_: Exception) {
        }
    }

    /**
     * Converts any recorded/uploaded clip (M4A, MP4 video, WebM, 3GP, AMR, etc.) into a clean,
     * uncompressed 16-bit PCM WAV file (trimmed cleanly up to 30 seconds for optimal Cartesia cloning)
     * so Cartesia /voices/clone succeeds on 100% of Android recordings and uploaded files.
     */
    private fun prepareCleanClipForCartesia(
        context: Context,
        sourceFile: File,
        mimeType: String
    ): Triple<File, String, Boolean> {
        val ext = sourceFile.extension.lowercase()
        // If already a clean WAV or MP3 under 12MB, send directly
        if ((ext == "wav" || ext == "mp3") && sourceFile.length() <= 12L * 1024L * 1024L) {
            val cleanMime = if (ext == "wav") "audio/wav" else "audio/mpeg"
            return Triple(sourceFile, cleanMime, false)
        }

        // Decode audio track via MediaAudioTranscoder into pure 16-bit mono WAV
        val decodedWav = MediaAudioTranscoder.transcodeToMonoWav(
            context = context,
            sourceFile = sourceFile,
            filePrefix = "cartesia_clean_clip_",
            maxDurationSeconds = 30,
            maxOutputSampleRate = 0
        )
        if (decodedWav != null && decodedWav.exists() && decodedWav.length() > 1000L) {
            return Triple(decodedWav, "audio/wav", true)
        }

        // Fallback: upload original file with normalized MIME
        val fallbackMime = when {
            ext == "m4a" || mimeType.contains("mp4") -> "audio/mp4"
            ext == "ogg" || ext == "opus" -> "audio/ogg"
            ext == "flac" -> "audio/flac"
            mimeType.startsWith("audio/") -> mimeType
            else -> "audio/mp4"
        }
        return Triple(sourceFile, fallbackMime, false)
    }

    private fun mapLanguageToCartesiaIso(language: String): String {
        return when {
            language.contains("Bengali", true) || language.contains("Bangla", true) -> "bn"
            language.contains("Hindi", true) -> "hi"
            language.contains("Spanish", true) -> "es"
            language.contains("French", true) -> "fr"
            language.contains("Arabic", true) -> "ar"
            language.contains("German", true) -> "de"
            language.contains("Japanese", true) -> "ja"
            language.contains("Portuguese", true) -> "pt"
            language.contains("Korean", true) -> "ko"
            language.contains("Urdu", true) -> "ur"
            language.contains("Italian", true) -> "it"
            language.contains("Russian", true) -> "ru"
            language.contains("Turkish", true) -> "tr"
            language.contains("Chinese", true) || language.contains("Mandarin", true) -> "zh"
            language.contains("Indonesian", true) -> "id"
            language.contains("Vietnamese", true) -> "vi"
            language.contains("Thai", true) -> "th"
            else -> "en"
        }
    }

    private fun mapEmotionToCartesia(emotionStyle: String): String? {
        return when (emotionStyle.lowercase().trim()) {
            "natural chat" -> "neutral"
            "warm story" -> "content"
            "emotional" -> "sympathetic"
            "cheerful" -> "happy"
            "dramatic" -> "confident"
            "calm & soft" -> "calm"
            "podcast host" -> "enthusiastic"
            else -> null
        }
    }

    private fun parseCartesiaError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = JSONObject(body)
            when {
                json.has("error") -> json.optString("error").ifBlank { null }
                json.has("message") -> json.optString("message").ifBlank { null }
                json.has("detail") -> json.optString("detail").ifBlank { null }
                else -> null
            }
        } catch (_: Exception) {
            body.take(180).trim().ifBlank { null }
        }
    }
}

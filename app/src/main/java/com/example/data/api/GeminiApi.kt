package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @param:Json(name = "contents") val contents: List<Content>,
    @param:Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @param:Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @param:Json(name = "mimeType") val mimeType: String,
    @param:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @param:Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @param:Json(name = "speechConfig") val speechConfig: SpeechConfig? = null,
    @param:Json(name = "temperature") val temperature: Float? = null,
    @param:Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    @param:Json(name = "voiceConfig") val voiceConfig: VoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    @param:Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: PrebuiltVoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    @param:Json(name = "voiceName") val voiceName: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @param:Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @param:Json(name = "content") val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            // BASIC avoids logging megabytes of Base64 audio strings to Logcat
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val service: GeminiApiService by lazy {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

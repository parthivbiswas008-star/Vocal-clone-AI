package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.VoiceCatalog
import com.example.data.database.GeneratedAudioEntity
import com.example.data.database.VoiceDatabase
import com.example.data.database.VoiceProfileEntity
import com.example.data.repository.ApiKeyException
import com.example.data.repository.VoiceAnalysisResult
import com.example.data.repository.VoiceRepository
import com.example.util.ApiKeyManager
import com.example.util.AudioFileSaver
import com.example.util.AudioPlayer
import com.example.util.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class VoiceViewModel(
    application: Application,
    private val repository: VoiceRepository,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) : AndroidViewModel(application) {

    val voiceProfiles: StateFlow<List<VoiceProfileEntity>> = repository.allVoiceProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedAudios: StateFlow<List<GeneratedAudioEntity>> = repository.allGeneratedAudios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selected Voice & Language in Studio ---
    private val _selectedVoiceId = MutableStateFlow<Int?>(null)
    val selectedVoiceId: StateFlow<Int?> = _selectedVoiceId

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    // --- API Key State ---
    private val _hasApiKey = MutableStateFlow(repository.apiKeyManager.hasValidKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey

    private val _isUsingCustomApiKey = MutableStateFlow(repository.apiKeyManager.isUsingCustomKey())
    val isUsingCustomApiKey: StateFlow<Boolean> = _isUsingCustomApiKey

    private val _hasBuiltInApiKey = MutableStateFlow(repository.apiKeyManager.hasBuiltInKey())
    val hasBuiltInApiKey: StateFlow<Boolean> = _hasBuiltInApiKey

    private val _maskedApiKey = MutableStateFlow(repository.apiKeyManager.getMaskedKeyPreview())
    val maskedApiKey: StateFlow<String> = _maskedApiKey

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog

    private val _isVerifyingApiKey = MutableStateFlow(false)
    val isVerifyingApiKey: StateFlow<Boolean> = _isVerifyingApiKey

    private val _apiKeyDialogError = MutableStateFlow<String?>(null)
    val apiKeyDialogError: StateFlow<String?> = _apiKeyDialogError

    // --- Live Recording & File Upload State ---
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec

    private val _recordingAmplitudes = MutableStateFlow<List<Int>>(emptyList())
    val recordingAmplitudes: StateFlow<List<Int>> = _recordingAmplitudes

    private val _tempSampleFile = MutableStateFlow<File?>(null)
    val tempSampleFile: StateFlow<File?> = _tempSampleFile

    private val _tempSampleMimeType = MutableStateFlow("audio/mp4")
    val tempSampleMimeType: StateFlow<String> = _tempSampleMimeType

    private val _tempSampleDisplayName = MutableStateFlow<String?>(null)
    val tempSampleDisplayName: StateFlow<String?> = _tempSampleDisplayName

    private val _tempSampleMeta = MutableStateFlow<String?>(null)
    val tempSampleMeta: StateFlow<String?> = _tempSampleMeta

    private var recordingJob: Job? = null
    private var analysisJob: Job? = null

    // --- AI Accent & Voice DNA Analysis State ---
    private val _isAnalyzingVoice = MutableStateFlow(false)
    val isAnalyzingVoice: StateFlow<Boolean> = _isAnalyzingVoice

    private val _voiceAnalysis = MutableStateFlow<VoiceAnalysisResult?>(null)
    val voiceAnalysis: StateFlow<VoiceAnalysisResult?> = _voiceAnalysis

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError

    private val _analysisNeedsApiKey = MutableStateFlow(false)
    val analysisNeedsApiKey: StateFlow<Boolean> = _analysisNeedsApiKey

    // --- Speech Generation State ---
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError

    private val _generationSuccess = MutableStateFlow(false)
    val generationSuccess: StateFlow<Boolean> = _generationSuccess

    // --- Download / Share / Status Feedback ---
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage

    // --- Global Audio Player & Consistent Speed State ---
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying
    val playPosition: StateFlow<Int> = audioPlayer.currentPosition
    val playDuration: StateFlow<Int> = audioPlayer.duration
    val playingPath: StateFlow<String?> = audioPlayer.playingAudioPath
    val playbackSpeed: StateFlow<Float> = audioPlayer.playbackSpeed

    init {
        viewModelScope.launch {
            repository.syncCuratedStudioVoices()
        }
    }

    // --- API Key Actions ---
    fun openApiKeyDialog() {
        _apiKeyDialogError.value = null
        refreshApiKeyState()
        _showApiKeyDialog.value = true
    }

    fun dismissApiKeyDialog() {
        _showApiKeyDialog.value = false
        _apiKeyDialogError.value = null
    }

    fun verifyAndSaveApiKey(rawKey: String) {
        viewModelScope.launch {
            _isVerifyingApiKey.value = true
            _apiKeyDialogError.value = null
            val result = repository.verifyApiKey(rawKey)
            result.onSuccess { cleanKey ->
                repository.apiKeyManager.saveCustomApiKey(cleanKey)
                refreshApiKeyState()
                _analysisNeedsApiKey.value = false
                _showApiKeyDialog.value = false
                _userMessage.value = "Gemini API key verified and saved!"
            }.onFailure { err ->
                _apiKeyDialogError.value = err.message ?: "Could not verify this API key."
            }
            _isVerifyingApiKey.value = false
        }
    }

    fun clearCustomApiKey() {
        repository.apiKeyManager.clearCustomApiKey()
        refreshApiKeyState()
        _apiKeyDialogError.value = null
        _userMessage.value = if (_hasBuiltInApiKey.value) {
            "Custom key removed. Using built-in secret key."
        } else {
            "Saved API key removed."
        }
    }

    private fun refreshApiKeyState() {
        _hasApiKey.value = repository.apiKeyManager.hasValidKey()
        _isUsingCustomApiKey.value = repository.apiKeyManager.isUsingCustomKey()
        _hasBuiltInApiKey.value = repository.apiKeyManager.hasBuiltInKey()
        _maskedApiKey.value = repository.apiKeyManager.getMaskedKeyPreview()
    }

    // --- Studio Voice & Language Selection ---
    fun selectVoiceProfile(profileId: Int, syncLanguage: Boolean = true) {
        _selectedVoiceId.value = profileId
        if (syncLanguage) {
            val profile = voiceProfiles.value.firstOrNull { it.id == profileId }
            if (profile != null && VoiceCatalog.supportedLanguages.any { it.displayName.equals(profile.language, true) }) {
                val matched = VoiceCatalog.supportedLanguages.first { it.displayName.equals(profile.language, true) }
                _selectedLanguage.value = matched.displayName
            }
        }
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- Live Voice Recording ---
    fun startRecording() {
        if (_isRecording.value) return
        analysisJob?.cancel()
        audioPlayer.stop()
        clearTempSampleOnly()

        try {
            val cacheDir = getApplication<Application>().cacheDir
            val tempFile = File(cacheDir, "live_voice_sample_${System.currentTimeMillis()}.m4a")
            _tempSampleFile.value = tempFile
            _tempSampleMimeType.value = "audio/mp4"
            _tempSampleDisplayName.value = "Live Microphone Recording"
            _tempSampleMeta.value = null
            audioRecorder.start(tempFile)
            _isRecording.value = true
            _recordingDurationSec.value = 0
            _recordingAmplitudes.value = emptyList()

            recordingJob = viewModelScope.launch {
                val amps = mutableListOf<Int>()
                var ticks = 0
                while (_isRecording.value) {
                    delay(100)
                    ticks++
                    if (ticks % 10 == 0) {
                        _recordingDurationSec.value = ticks / 10
                    }
                    val amp = audioRecorder.getMaxAmplitude()
                    amps.add(amp)
                    if (amps.size > 40) {
                        amps.removeAt(0)
                    }
                    _recordingAmplitudes.value = amps.toList()
                }
            }
        } catch (e: Exception) {
            _analysisError.value = "Failed to start recording: ${e.message}"
        }
    }

    fun stopRecordingAndAnalyze(hintLanguage: String) {
        if (!_isRecording.value) return
        val capturedSeconds = _recordingDurationSec.value
        audioRecorder.stop()
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        val file = _tempSampleFile.value
        if (file == null || !file.exists() || file.length() == 0L) {
            _analysisError.value = "Recorded audio is empty. Please speak clearly into the microphone."
            return
        }

        val durationMs = AudioFileSaver.extractMediaDurationMs(file).takeIf { it > 0 } ?: (capturedSeconds * 1000L)
        _tempSampleMeta.value = formatSampleMetadata(durationMs, file.length())

        if (durationMs < 2000L) {
            _userMessage.value = "Tip: Recording 8–15 seconds produces a much purer voice clone."
        }
        analyzeCurrentSample(hintLanguage)
    }

    // --- Voice File Upload (Audio & Video Clips) ---
    fun importUploadedAudio(uri: Uri, hintLanguage: String) {
        audioPlayer.stop()
        analysisJob?.cancel()
        if (_isRecording.value) {
            audioRecorder.stop()
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }

        analysisJob = viewModelScope.launch {
            _isAnalyzingVoice.value = true
            _analysisError.value = null
            _analysisNeedsApiKey.value = false
            _voiceAnalysis.value = null

            val importResult = AudioFileSaver.importAudioFromUri(getApplication(), uri)
            importResult.onSuccess { imported ->
                clearTempSampleOnly()
                _tempSampleFile.value = imported.file
                _tempSampleMimeType.value = imported.mimeType
                _tempSampleDisplayName.value = imported.displayName
                _tempSampleMeta.value = formatSampleMetadata(imported.durationMs, imported.sizeBytes)
                runSampleAnalysisInternal(imported.file, imported.mimeType, hintLanguage)
            }.onFailure { err ->
                _isAnalyzingVoice.value = false
                _analysisError.value = err.message ?: "Failed to load selected file."
            }
        }
    }

    fun analyzeCurrentSample(hintLanguage: String) {
        val file = _tempSampleFile.value ?: return
        val mime = _tempSampleMimeType.value
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            runSampleAnalysisInternal(file, mime, hintLanguage)
        }
    }

    private suspend fun runSampleAnalysisInternal(file: File, mime: String, hintLanguage: String) {
        _isAnalyzingVoice.value = true
        _analysisError.value = null
        _analysisNeedsApiKey.value = false
        try {
            val result = repository.analyzeVoiceSample(file, mime, hintLanguage)
            _voiceAnalysis.value = result
        } catch (e: ApiKeyException) {
            _analysisNeedsApiKey.value = true
            _analysisError.value = e.message
            openApiKeyDialog()
        } catch (e: Exception) {
            _analysisError.value = e.message ?: "AI voice analysis failed."
        } finally {
            _isAnalyzingVoice.value = false
        }
    }

    fun saveClonedVoiceProfile(
        name: String,
        fallbackGender: String,
        customAccentOverride: String,
        hintLanguage: String,
        customDescription: String,
        onComplete: (Int) -> Unit = {}
    ) {
        if (_isRecording.value) {
            audioRecorder.stop()
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }

        val tempFile = _tempSampleFile.value
        if (tempFile == null || !tempFile.exists() || tempFile.length() == 0L) {
            _analysisError.value = "Please record your voice or upload an audio/video file first."
            return
        }

        val analysis = _voiceAnalysis.value
        val mime = _tempSampleMimeType.value

        viewModelScope.launch {
            try {
                val savedFile = withContext(Dispatchers.IO) {
                    val profilesDir = File(getApplication<Application>().filesDir, "voice_profiles")
                    if (!profilesDir.exists()) profilesDir.mkdirs()
                    val ext = tempFile.extension.ifBlank { "m4a" }
                    val dest = File(profilesDir, "clone_${System.currentTimeMillis()}.$ext")
                    tempFile.copyTo(dest, overwrite = true)
                    tempFile.delete()
                    dest
                }

                val resolvedLanguage = analysis?.detectedLanguage ?: hintLanguage
                val resolvedAccent = customAccentOverride.trim().ifBlank {
                    analysis?.detectedAccent ?: "$resolvedLanguage Native Accent"
                }
                val resolvedGender = analysis?.detectedGender ?: fallbackGender
                val resolvedPitch = analysis?.pitchProfile ?: "Natural Warm"
                val resolvedTraits = analysis?.timbreDescription
                    ?: "Authentic personal voice clone with natural warmth and regional cadence."
                val resolvedDescription = customDescription.trim().ifBlank {
                    "Cloned $resolvedAccent voice ($resolvedPitch). $resolvedTraits"
                }

                val profile = VoiceProfileEntity(
                    name = name.trim().ifBlank { "My Cloned Voice" },
                    gender = resolvedGender,
                    accent = resolvedAccent,
                    audioPath = savedFile.absolutePath,
                    isPrebuilt = false,
                    prebuiltCode = analysis?.bestMatchingPrebuiltVoice,
                    recordingLanguage = resolvedLanguage,
                    pitchTone = resolvedPitch,
                    paceSpeed = "1.0x",
                    detectedAccent = resolvedAccent,
                    description = resolvedDescription,
                    language = resolvedLanguage,
                    voiceTraits = resolvedTraits,
                    estimatedAge = analysis?.estimatedAge ?: "Adult",
                    speakingCadence = analysis?.speakingCadence ?: "Natural conversational rhythm",
                    confidenceScore = analysis?.confidenceScore ?: 88,
                    audioMimeType = mime
                )

                val newId = repository.insertVoiceProfile(profile).toInt()
                _selectedVoiceId.value = newId
                if (VoiceCatalog.supportedLanguages.any { it.displayName.equals(resolvedLanguage, true) }) {
                    _selectedLanguage.value = resolvedLanguage
                }
                _tempSampleFile.value = null
                _tempSampleDisplayName.value = null
                _tempSampleMeta.value = null
                _voiceAnalysis.value = null
                _analysisError.value = null
                _analysisNeedsApiKey.value = false
                _recordingAmplitudes.value = emptyList()
                _recordingDurationSec.value = 0
                onComplete(newId)
            } catch (e: Exception) {
                _analysisError.value = "Failed to save voice clone: ${e.message}"
            }
        }
    }

    fun cancelCloningSession() {
        analysisJob?.cancel()
        analysisJob = null
        if (_isRecording.value) {
            audioRecorder.stop()
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }
        audioPlayer.stop()
        clearTempSampleOnly()
        _isAnalyzingVoice.value = false
        _recordingAmplitudes.value = emptyList()
        _recordingDurationSec.value = 0
    }

    private fun clearTempSampleOnly() {
        _tempSampleFile.value?.let {
            if (it.exists()) it.delete()
        }
        _tempSampleFile.value = null
        _tempSampleDisplayName.value = null
        _tempSampleMeta.value = null
        _voiceAnalysis.value = null
        _analysisError.value = null
        _analysisNeedsApiKey.value = false
    }

    fun playPreviewSample() {
        _tempSampleFile.value?.let { file ->
            if (file.exists()) {
                // Play raw sample at authentic 1.0x so user hears true recording quality
                audioPlayer.play(file.absolutePath, useCustomSpeed = false)
            }
        }
    }

    // --- Speech Generation ---
    fun generateVoice(
        title: String,
        text: String,
        voiceProfile: VoiceProfileEntity,
        language: String
    ) {
        if (text.isBlank()) {
            _generationError.value = "Please enter text to synthesize."
            return
        }

        if (!repository.apiKeyManager.hasValidKey()) {
            openApiKeyDialog()
            _generationError.value = "Please add your Gemini API key to generate speech."
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationSuccess.value = false
            try {
                val created = repository.generateSpeech(title, text, voiceProfile, language)
                _generationSuccess.value = true
                audioPlayer.play(created.audioPath, useCustomSpeed = true)
            } catch (e: ApiKeyException) {
                _generationError.value = e.message
                openApiKeyDialog()
            } catch (e: Exception) {
                _generationError.value = e.message ?: "Speech generation failed."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearGenerationState() {
        _generationError.value = null
        _generationSuccess.value = false
    }

    // --- Download to Internal Storage ---
    fun downloadAudio(audio: GeneratedAudioEntity) {
        viewModelScope.launch {
            val result = AudioFileSaver.saveAudioToDownloads(
                context = getApplication(),
                sourcePath = audio.audioPath,
                displayTitle = audio.title
            )
            result.onSuccess { savedLocation ->
                _userMessage.value = "Saved to $savedLocation"
            }.onFailure { err ->
                _userMessage.value = "Download failed: ${err.message}"
            }
        }
    }

    // --- Audio Playback ---
    fun playAudio(path: String, useCustomSpeed: Boolean = true) {
        audioPlayer.play(path, useCustomSpeed = useCustomSpeed)
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    fun seekAudio(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        analysisJob?.cancel()
        recordingJob?.cancel()
        audioPlayer.stop()
        if (_isRecording.value) {
            audioRecorder.stop()
        }
    }

    // --- Profile / Audio Deletion ---
    fun deleteVoiceProfile(id: Int) {
        viewModelScope.launch {
            if (_selectedVoiceId.value == id) {
                _selectedVoiceId.value = null
            }
            val deletedPath = repository.deleteVoiceProfile(id)
            if (deletedPath != null && playingPath.value == deletedPath) {
                audioPlayer.stop()
            }
        }
    }

    fun deleteGeneratedAudio(id: Int) {
        viewModelScope.launch {
            val deletedPath = repository.deleteGeneratedAudio(id)
            if (deletedPath != null && playingPath.value == deletedPath) {
                audioPlayer.stop()
            }
        }
    }

    private fun formatSampleMetadata(durationMs: Long, sizeBytes: Long): String {
        val sizeStr = when {
            sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f))
            sizeBytes >= 1024 -> "${sizeBytes / 1024} KB"
            else -> "$sizeBytes B"
        }
        return if (durationMs > 0) {
            val totalSec = (durationMs / 1000).toInt()
            val durStr = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
            "$durStr • $sizeStr"
        } else {
            sizeStr
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VoiceViewModel::class.java)) {
                val db = VoiceDatabase.getDatabase(application)
                val apiKeyManager = ApiKeyManager(application)
                val repository = VoiceRepository(application, db.voiceDao(), apiKeyManager)
                val recorder = AudioRecorder(application)
                val player = AudioPlayer(application)
                @Suppress("UNCHECKED_CAST")
                return VoiceViewModel(application, repository, recorder, player) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

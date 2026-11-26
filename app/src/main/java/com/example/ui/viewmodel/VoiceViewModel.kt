package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.VoiceCatalog
import com.example.data.api.CartesiaApiException
import com.example.data.database.GeneratedAudioEntity
import com.example.data.database.VoiceDatabase
import com.example.data.database.VoiceProfileEntity
import com.example.data.repository.ApiKeyException
import com.example.data.repository.VoiceAnalysisResult
import com.example.data.repository.VoiceRepository
import com.example.util.AccentColorPreset
import com.example.util.ApiKeyManager
import com.example.util.AppSettings
import com.example.util.AppSettingsManager
import com.example.util.AppThemePreset
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

enum class ApiKeyDialogTab { CARTESIA, GEMINI }

class VoiceViewModel(
    application: Application,
    private val repository: VoiceRepository,
    private val settingsManager: AppSettingsManager,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) : AndroidViewModel(application) {

    val voiceProfiles: StateFlow<List<VoiceProfileEntity>> = repository.allVoiceProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedAudios: StateFlow<List<GeneratedAudioEntity>> = repository.allGeneratedAudios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- App Settings (Theme, Accent, Mastering, Engine, Recording Quality) ---
    val appSettings: StateFlow<AppSettings> = settingsManager.settings

    // --- Selected Voice, Language & Human Realism Controls in Studio ---
    private val _selectedVoiceId = MutableStateFlow<Int?>(null)
    val selectedVoiceId: StateFlow<Int?> = _selectedVoiceId

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    private val _selectedEmotionStyle = MutableStateFlow("Natural Chat")
    val selectedEmotionStyle: StateFlow<String> = _selectedEmotionStyle

    private val _naturalBreathsEnabled = MutableStateFlow(settingsManager.settings.value.naturalBreathsDefault)
    val naturalBreathsEnabled: StateFlow<Boolean> = _naturalBreathsEnabled

    private val _ultraHumanMode = MutableStateFlow(settingsManager.settings.value.ultraHumanDefault)
    val ultraHumanMode: StateFlow<Boolean> = _ultraHumanMode

    // --- Gemini API Key State ---
    private val _hasApiKey = MutableStateFlow(repository.apiKeyManager.hasValidKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey

    private val _isUsingCustomApiKey = MutableStateFlow(repository.apiKeyManager.isUsingCustomKey())
    val isUsingCustomApiKey: StateFlow<Boolean> = _isUsingCustomApiKey

    private val _hasBuiltInApiKey = MutableStateFlow(repository.apiKeyManager.hasBuiltInKey())
    val hasBuiltInApiKey: StateFlow<Boolean> = _hasBuiltInApiKey

    private val _maskedApiKey = MutableStateFlow(repository.apiKeyManager.getMaskedKeyPreview())
    val maskedApiKey: StateFlow<String> = _maskedApiKey

    // --- Cartesia AI API Key State (True 1:1 Voice Cloning) ---
    private val _hasCartesiaApiKey = MutableStateFlow(repository.apiKeyManager.hasValidCartesiaKey())
    val hasCartesiaApiKey: StateFlow<Boolean> = _hasCartesiaApiKey

    private val _isUsingCustomCartesiaKey = MutableStateFlow(repository.apiKeyManager.isUsingCustomCartesiaKey())
    val isUsingCustomCartesiaKey: StateFlow<Boolean> = _isUsingCustomCartesiaKey

    private val _hasBuiltInCartesiaKey = MutableStateFlow(repository.apiKeyManager.hasBuiltInCartesiaKey())
    val hasBuiltInCartesiaKey: StateFlow<Boolean> = _hasBuiltInCartesiaKey

    private val _maskedCartesiaKey = MutableStateFlow(repository.apiKeyManager.getMaskedCartesiaKeyPreview())
    val maskedCartesiaKey: StateFlow<String> = _maskedCartesiaKey

    // --- Unified API Key Dialog State ---
    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog

    private val _apiKeyDialogTab = MutableStateFlow(ApiKeyDialogTab.CARTESIA)
    val apiKeyDialogTab: StateFlow<ApiKeyDialogTab> = _apiKeyDialogTab

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

    // Temp Cartesia 1:1 Voice ID created during live audition/saving
    private val _tempCartesiaVoiceId = MutableStateFlow<String?>(null)
    val tempCartesiaVoiceId: StateFlow<String?> = _tempCartesiaVoiceId

    private val _isCreatingClone = MutableStateFlow(false)
    val isCreatingClone: StateFlow<Boolean> = _isCreatingClone

    private val _upgradingProfileId = MutableStateFlow<Int?>(null)
    val upgradingProfileId: StateFlow<Int?> = _upgradingProfileId

    private var recordingJob: Job? = null
    private var analysisJob: Job? = null
    private var auditionJob: Job? = null

    // --- AI Accent & Voice DNA Analysis State ---
    private val _isAnalyzingVoice = MutableStateFlow(false)
    val isAnalyzingVoice: StateFlow<Boolean> = _isAnalyzingVoice

    private val _voiceAnalysis = MutableStateFlow<VoiceAnalysisResult?>(null)
    val voiceAnalysis: StateFlow<VoiceAnalysisResult?> = _voiceAnalysis

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError

    private val _analysisNeedsApiKey = MutableStateFlow(false)
    val analysisNeedsApiKey: StateFlow<Boolean> = _analysisNeedsApiKey

    // --- Clone Live Audition (A/B Compare Before Saving) ---
    private val _isAuditioningClone = MutableStateFlow(false)
    val isAuditioningClone: StateFlow<Boolean> = _isAuditioningClone

    private val _auditionAudioFile = MutableStateFlow<File?>(null)
    val auditionAudioFile: StateFlow<File?> = _auditionAudioFile

    private val _auditionError = MutableStateFlow<String?>(null)
    val auditionError: StateFlow<String?> = _auditionError

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

    // --- Storage / Cache Stats ---
    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes

    // --- Global Audio Player & Consistent Speed State ---
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying
    val playPosition: StateFlow<Int> = audioPlayer.currentPosition
    val playDuration: StateFlow<Int> = audioPlayer.duration
    val playingPath: StateFlow<String?> = audioPlayer.playingAudioPath
    val playbackSpeed: StateFlow<Float> = audioPlayer.playbackSpeed

    init {
        audioPlayer.setPlaybackSpeed(settingsManager.settings.value.defaultPlaybackSpeed)
        viewModelScope.launch {
            repository.syncCuratedStudioVoices()
            refreshCacheSize()
        }
    }

    // --- Settings Actions ---
    fun setThemePreset(preset: AppThemePreset) {
        settingsManager.update { it.copy(themePreset = preset) }
    }

    fun setAccentPreset(preset: AccentColorPreset) {
        settingsManager.update { it.copy(accentPreset = preset) }
    }

    fun setPreferCartesiaCloning(enabled: Boolean) {
        settingsManager.update { it.copy(preferCartesiaCloning = enabled) }
    }

    fun setStudioMasteringEnabled(enabled: Boolean) {
        settingsManager.update { it.copy(studioMasteringEnabled = enabled) }
    }

    fun setAutoPlayGenerated(enabled: Boolean) {
        settingsManager.update { it.copy(autoPlayGenerated = enabled) }
    }

    fun setHighResRecording(enabled: Boolean) {
        settingsManager.update { it.copy(highResRecording = enabled) }
    }

    fun setShowWaveforms(enabled: Boolean) {
        settingsManager.update { it.copy(showWaveforms = enabled) }
    }

    fun setCompactCards(enabled: Boolean) {
        settingsManager.update { it.copy(compactCards = enabled) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 2.0f)
        settingsManager.update { it.copy(defaultPlaybackSpeed = clamped) }
        audioPlayer.setPlaybackSpeed(clamped)
    }

    fun setUltraHumanDefault(enabled: Boolean) {
        settingsManager.update { it.copy(ultraHumanDefault = enabled) }
        _ultraHumanMode.value = enabled
    }

    fun setNaturalBreathsDefault(enabled: Boolean) {
        settingsManager.update { it.copy(naturalBreathsDefault = enabled) }
        _naturalBreathsEnabled.value = enabled
    }

    fun resetAppearanceToDefaults() {
        settingsManager.resetAppearance()
        _userMessage.value = "Appearance reset to Cosmic Glass defaults."
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            val bytes = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            _cacheSizeBytes.value = bytes
        }
    }

    fun clearTempAudioCache() {
        viewModelScope.launch {
            audioPlayer.stop()
            withContext(Dispatchers.IO) {
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name.startsWith("clone_audition_") ||
                            file.name.startsWith("cartesia_clean_clip_") ||
                            file.name.startsWith("live_voice_sample_") ||
                            file.name.startsWith("uploaded_voice_"))
                    ) {
                        file.delete()
                    }
                }
            }
            refreshCacheSize()
            _userMessage.value = "Temporary audio cache cleared!"
        }
    }

    // --- API Key Actions (Gemini + Cartesia AI) ---
    fun openApiKeyDialog(initialTab: ApiKeyDialogTab = if (!_hasCartesiaApiKey.value) ApiKeyDialogTab.CARTESIA else ApiKeyDialogTab.GEMINI) {
        _apiKeyDialogError.value = null
        _apiKeyDialogTab.value = initialTab
        refreshApiKeyState()
        _showApiKeyDialog.value = true
    }

    fun selectApiKeyDialogTab(tab: ApiKeyDialogTab) {
        _apiKeyDialogTab.value = tab
        _apiKeyDialogError.value = null
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
                _apiKeyDialogError.value = err.message ?: "Could not verify this Gemini API key."
            }
            _isVerifyingApiKey.value = false
        }
    }

    fun clearCustomApiKey() {
        repository.apiKeyManager.clearCustomApiKey()
        refreshApiKeyState()
        _apiKeyDialogError.value = null
        _userMessage.value = if (_hasBuiltInApiKey.value) {
            "Custom Gemini key removed. Using built-in secret key."
        } else {
            "Saved Gemini API key removed."
        }
    }

    fun verifyAndSaveCartesiaApiKey(rawKey: String) {
        viewModelScope.launch {
            _isVerifyingApiKey.value = true
            _apiKeyDialogError.value = null
            val result = repository.verifyCartesiaApiKey(rawKey)
            result.onSuccess { cleanKey ->
                repository.apiKeyManager.saveCustomCartesiaApiKey(cleanKey)
                refreshApiKeyState()
                _analysisNeedsApiKey.value = false
                _showApiKeyDialog.value = false
                _userMessage.value = "Cartesia AI key verified! True 1:1 voice cloning is now active."
            }.onFailure { err ->
                _apiKeyDialogError.value = err.message ?: "Could not verify this Cartesia AI API key."
            }
            _isVerifyingApiKey.value = false
        }
    }

    fun clearCustomCartesiaApiKey() {
        repository.apiKeyManager.clearCustomCartesiaApiKey()
        refreshApiKeyState()
        _apiKeyDialogError.value = null
        _userMessage.value = if (_hasBuiltInCartesiaKey.value) {
            "Custom Cartesia key removed. Using built-in secret key."
        } else {
            "Saved Cartesia API key removed."
        }
    }

    private fun refreshApiKeyState() {
        _hasApiKey.value = repository.apiKeyManager.hasValidKey()
        _isUsingCustomApiKey.value = repository.apiKeyManager.isUsingCustomKey()
        _hasBuiltInApiKey.value = repository.apiKeyManager.hasBuiltInKey()
        _maskedApiKey.value = repository.apiKeyManager.getMaskedKeyPreview()

        _hasCartesiaApiKey.value = repository.apiKeyManager.hasValidCartesiaKey()
        _isUsingCustomCartesiaKey.value = repository.apiKeyManager.isUsingCustomCartesiaKey()
        _hasBuiltInCartesiaKey.value = repository.apiKeyManager.hasBuiltInCartesiaKey()
        _maskedCartesiaKey.value = repository.apiKeyManager.getMaskedCartesiaKeyPreview()
    }

    // --- Studio Controls ---
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

    fun selectEmotionStyle(styleId: String) {
        _selectedEmotionStyle.value = styleId
    }

    fun toggleNaturalBreaths() {
        val next = !_naturalBreathsEnabled.value
        _naturalBreathsEnabled.value = next
        settingsManager.update { it.copy(naturalBreathsDefault = next) }
    }

    fun toggleUltraHumanMode() {
        val next = !_ultraHumanMode.value
        _ultraHumanMode.value = next
        settingsManager.update { it.copy(ultraHumanDefault = next) }
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
        auditionJob?.cancel()
        audioPlayer.stop()
        clearTempSampleOnly()

        try {
            val cacheDir = getApplication<Application>().cacheDir
            val tempFile = File(cacheDir, "live_voice_sample_${System.currentTimeMillis()}.m4a")
            _tempSampleFile.value = tempFile
            _tempSampleMimeType.value = "audio/mp4"
            _tempSampleDisplayName.value = "Live Microphone Recording"
            _tempSampleMeta.value = null
            audioRecorder.start(tempFile, highRes = settingsManager.settings.value.highResRecording)
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

        if (durationMs < 2500L) {
            _userMessage.value = "Tip: Recording 10–15 seconds produces a remarkably identical voice clone."
        }
        analyzeCurrentSample(hintLanguage)
    }

    // --- Voice File Upload (Audio & Video Clips) ---
    fun importUploadedAudio(uri: Uri, hintLanguage: String) {
        audioPlayer.stop()
        analysisJob?.cancel()
        auditionJob?.cancel()
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
            _tempCartesiaVoiceId.value = null
            clearAuditionFileOnly()

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
        auditionJob?.cancel()
        clearAuditionFileOnly()
        analysisJob = viewModelScope.launch {
            runSampleAnalysisInternal(file, mime, hintLanguage)
        }
    }

    private suspend fun runSampleAnalysisInternal(file: File, mime: String, hintLanguage: String) {
        // If user has Cartesia key but no Gemini key, don't block with Gemini API error — Cartesia can clone 1:1 directly!
        val hasGemini = repository.apiKeyManager.hasValidKey()
        val hasCartesia = repository.apiKeyManager.hasValidCartesiaKey()
        if (!hasGemini && hasCartesia) {
            _isAnalyzingVoice.value = false
            _analysisError.value = null
            _analysisNeedsApiKey.value = false
            return
        }

        _isAnalyzingVoice.value = true
        _analysisError.value = null
        _analysisNeedsApiKey.value = false
        try {
            val result = repository.analyzeVoiceSample(file, mime, hintLanguage)
            _voiceAnalysis.value = result
        } catch (e: ApiKeyException) {
            _analysisNeedsApiKey.value = true
            _analysisError.value = e.message
            openApiKeyDialog(if (!hasCartesia) ApiKeyDialogTab.CARTESIA else ApiKeyDialogTab.GEMINI)
        } catch (e: Exception) {
            _analysisError.value = e.message ?: "AI voice analysis failed."
        } finally {
            _isAnalyzingVoice.value = false
        }
    }

    // --- Instant A/B Clone Match Audition Before Saving ---
    fun runCloneAudition(
        voiceName: String,
        chosenBaseVoice: String,
        customAccent: String,
        eqProfile: String,
        pitchCalibration: Float,
        hintLanguage: String
    ) {
        val tempFile = _tempSampleFile.value
        val analysis = _voiceAnalysis.value
        val useCartesia = settingsManager.settings.value.preferCartesiaCloning &&
            repository.apiKeyManager.hasValidCartesiaKey()

        if (!useCartesia && analysis == null) {
            _auditionError.value = "Complete Voice DNA scan or add a Cartesia AI API key first."
            return
        }

        auditionJob?.cancel()
        auditionJob = viewModelScope.launch {
            _isAuditioningClone.value = true
            _auditionError.value = null
            try {
                val targetLang = analysis?.detectedLanguage?.ifBlank { hintLanguage } ?: hintLanguage
                var activeCartesiaId: String? = _tempCartesiaVoiceId.value

                if (useCartesia && activeCartesiaId.isNullOrBlank() && tempFile != null && tempFile.exists()) {
                    activeCartesiaId = repository.createCartesiaTrueClone(
                        audioFile = tempFile,
                        mimeType = _tempSampleMimeType.value,
                        voiceName = voiceName.trim().ifBlank { "My Cloned Voice" },
                        description = "1:1 instant voice clone ($targetLang)",
                        language = targetLang
                    )
                    _tempCartesiaVoiceId.value = activeCartesiaId
                }

                val file = repository.auditionVoiceClone(
                    analysis = analysis,
                    chosenPrebuiltVoice = chosenBaseVoice.ifBlank { analysis?.bestMatchingPrebuiltVoice ?: "Charon" },
                    customAccent = customAccent,
                    eqProfile = eqProfile,
                    language = targetLang,
                    cartesiaVoiceId = activeCartesiaId,
                    studioMasteringEnabled = settingsManager.settings.value.studioMasteringEnabled
                )
                clearAuditionFileOnly()
                _auditionAudioFile.value = file
                val effectivePitch = if (!activeCartesiaId.isNullOrBlank()) 1.0f else pitchCalibration
                audioPlayer.play(
                    filePath = file.absolutePath,
                    useCustomSpeed = false,
                    pitchCalibration = effectivePitch
                )
            } catch (e: CartesiaApiException) {
                _auditionError.value = e.message
                if (e.isAuthError) openApiKeyDialog(ApiKeyDialogTab.CARTESIA)
            } catch (e: ApiKeyException) {
                _auditionError.value = e.message
                openApiKeyDialog(ApiKeyDialogTab.GEMINI)
            } catch (e: Exception) {
                _auditionError.value = e.message ?: "Could not generate clone preview."
            } finally {
                _isAuditioningClone.value = false
            }
        }
    }

    fun playAuditionClip(pitchCalibration: Float) {
        _auditionAudioFile.value?.let { file ->
            if (file.exists()) {
                val effectivePitch = if (!_tempCartesiaVoiceId.value.isNullOrBlank()) 1.0f else pitchCalibration
                audioPlayer.play(
                    filePath = file.absolutePath,
                    useCustomSpeed = false,
                    pitchCalibration = effectivePitch
                )
            }
        }
    }

    fun saveClonedVoiceProfile(
        name: String,
        fallbackGender: String,
        customAccentOverride: String,
        hintLanguage: String,
        customDescription: String,
        selectedBaseVoiceOverride: String?,
        pitchCalibration: Float,
        eqProfile: String,
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
        val shouldUseCartesia = settingsManager.settings.value.preferCartesiaCloning &&
            repository.apiKeyManager.hasValidCartesiaKey()

        viewModelScope.launch {
            _isCreatingClone.value = true
            _analysisError.value = null
            try {
                val resolvedLanguage = analysis?.detectedLanguage ?: hintLanguage
                val resolvedAccent = customAccentOverride.trim().ifBlank {
                    analysis?.detectedAccent ?: "$resolvedLanguage Native Accent"
                }
                val resolvedGender = analysis?.detectedGender ?: fallbackGender
                val resolvedPitch = analysis?.pitchProfile ?: "Natural Exact Match"
                val resolvedTraits = analysis?.timbreDescription
                    ?: "True 1:1 personal voice clone capturing exact speaker timbre, resonance, and accent."
                val resolvedDescription = customDescription.trim().ifBlank {
                    if (shouldUseCartesia) {
                        "True 1:1 Cartesia neural voice clone ($resolvedAccent)."
                    } else {
                        "Cloned $resolvedAccent voice ($resolvedPitch). $resolvedTraits"
                    }
                }

                // Create TRUE 1:1 Cartesia voice clone directly from the user's audio clip when Cartesia key is active!
                var cartesiaVoiceId: String? = _tempCartesiaVoiceId.value
                if (shouldUseCartesia && cartesiaVoiceId.isNullOrBlank()) {
                    cartesiaVoiceId = repository.createCartesiaTrueClone(
                        audioFile = tempFile,
                        mimeType = mime,
                        voiceName = name.trim().ifBlank { "My Cloned Voice" },
                        description = resolvedDescription,
                        language = resolvedLanguage
                    )
                }

                val savedFile = withContext(Dispatchers.IO) {
                    val profilesDir = File(getApplication<Application>().filesDir, "voice_profiles")
                    if (!profilesDir.exists()) profilesDir.mkdirs()
                    val ext = tempFile.extension.ifBlank { "m4a" }
                    val dest = File(profilesDir, "clone_${System.currentTimeMillis()}.$ext")
                    tempFile.copyTo(dest, overwrite = true)
                    tempFile.delete()
                    dest
                }

                val chosenCode = selectedBaseVoiceOverride?.takeIf { it.isNotBlank() }
                    ?: analysis?.bestMatchingPrebuiltVoice

                val isCartesiaClone = !cartesiaVoiceId.isNullOrBlank()
                val profile = VoiceProfileEntity(
                    name = name.trim().ifBlank { "My Cloned Voice" },
                    gender = resolvedGender,
                    accent = resolvedAccent,
                    audioPath = savedFile.absolutePath,
                    isPrebuilt = false,
                    prebuiltCode = chosenCode,
                    recordingLanguage = resolvedLanguage,
                    pitchTone = resolvedPitch,
                    paceSpeed = "1.0x",
                    detectedAccent = resolvedAccent,
                    description = resolvedDescription,
                    language = resolvedLanguage,
                    voiceTraits = resolvedTraits,
                    estimatedAge = analysis?.estimatedAge ?: "Adult",
                    speakingCadence = analysis?.speakingCadence ?: "Natural conversational rhythm",
                    confidenceScore = if (isCartesiaClone) 99 else (analysis?.confidenceScore ?: 88),
                    audioMimeType = mime,
                    pitchCalibration = if (isCartesiaClone) 1.0f else pitchCalibration.coerceIn(0.88f, 1.12f),
                    eqProfile = eqProfile.ifBlank { analysis?.eqProfile ?: "warm" },
                    phoneticNotes = analysis?.phoneticNotes.orEmpty(),
                    secondaryPrebuiltCode = analysis?.secondaryMatchingPrebuiltVoice,
                    cartesiaVoiceId = cartesiaVoiceId,
                    cloneProvider = if (isCartesiaClone) "cartesia" else "gemini"
                )

                val newId = repository.insertVoiceProfile(profile).toInt()
                _selectedVoiceId.value = newId
                if (VoiceCatalog.supportedLanguages.any { it.displayName.equals(resolvedLanguage, true) }) {
                    _selectedLanguage.value = resolvedLanguage
                }
                clearAuditionFileOnly()
                _tempCartesiaVoiceId.value = null
                _tempSampleFile.value = null
                _tempSampleDisplayName.value = null
                _tempSampleMeta.value = null
                _voiceAnalysis.value = null
                _analysisError.value = null
                _analysisNeedsApiKey.value = false
                _recordingAmplitudes.value = emptyList()
                _recordingDurationSec.value = 0
                refreshCacheSize()
                onComplete(newId)
            } catch (e: CartesiaApiException) {
                _analysisError.value = e.message
                if (e.isAuthError) openApiKeyDialog(ApiKeyDialogTab.CARTESIA)
            } catch (e: Exception) {
                _analysisError.value = "Failed to save voice clone: ${e.message}"
            } finally {
                _isCreatingClone.value = false
            }
        }
    }

    fun upgradeExistingCloneToCartesia(profile: VoiceProfileEntity) {
        if (!repository.apiKeyManager.hasValidCartesiaKey()) {
            openApiKeyDialog(ApiKeyDialogTab.CARTESIA)
            _userMessage.value = "Add your Cartesia AI API key first to enable 1:1 exact voice cloning."
            return
        }
        viewModelScope.launch {
            _upgradingProfileId.value = profile.id
            try {
                repository.upgradeProfileToCartesiaClone(profile)
                _userMessage.value = "${profile.name} upgraded to 1:1 Cartesia True Clone!"
            } catch (e: CartesiaApiException) {
                _userMessage.value = e.message ?: "Cartesia upgrade failed."
                if (e.isAuthError) openApiKeyDialog(ApiKeyDialogTab.CARTESIA)
            } catch (e: Exception) {
                _userMessage.value = "Upgrade failed: ${e.message}"
            } finally {
                _upgradingProfileId.value = null
            }
        }
    }

    fun cancelCloningSession() {
        analysisJob?.cancel()
        analysisJob = null
        auditionJob?.cancel()
        auditionJob = null
        if (_isRecording.value) {
            audioRecorder.stop()
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }
        audioPlayer.stop()
        clearAuditionFileOnly()
        clearTempSampleOnly()
        _tempCartesiaVoiceId.value = null
        _isCreatingClone.value = false
        _isAnalyzingVoice.value = false
        _isAuditioningClone.value = false
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
        _tempCartesiaVoiceId.value = null
        _voiceAnalysis.value = null
        _analysisError.value = null
        _analysisNeedsApiKey.value = false
    }

    private fun clearAuditionFileOnly() {
        _auditionAudioFile.value?.let {
            if (it.exists()) it.delete()
        }
        _auditionAudioFile.value = null
        _auditionError.value = null
    }

    fun playPreviewSample() {
        _tempSampleFile.value?.let { file ->
            if (file.exists()) {
                audioPlayer.play(file.absolutePath, useCustomSpeed = false, pitchCalibration = 1.0f)
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

        val currentSettings = settingsManager.settings.value
        val canUseCartesiaForThisVoice = !voiceProfile.isPrebuilt &&
            currentSettings.preferCartesiaCloning &&
            repository.apiKeyManager.hasValidCartesiaKey()

        if (!canUseCartesiaForThisVoice && !repository.apiKeyManager.hasValidKey()) {
            openApiKeyDialog(if (!voiceProfile.isPrebuilt) ApiKeyDialogTab.CARTESIA else ApiKeyDialogTab.GEMINI)
            _generationError.value = if (!voiceProfile.isPrebuilt) {
                "Add a Cartesia AI key (for 1:1 cloning) or Gemini API key to generate speech."
            } else {
                "Please add your Gemini API key to generate speech with studio voices."
            }
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationSuccess.value = false
            try {
                val created = repository.generateSpeech(
                    title = title,
                    text = text,
                    voiceProfile = voiceProfile,
                    language = language,
                    emotionStyle = _selectedEmotionStyle.value,
                    naturalBreaths = _naturalBreathsEnabled.value,
                    ultraHumanMode = _ultraHumanMode.value,
                    preferCartesiaCloning = currentSettings.preferCartesiaCloning,
                    studioMasteringEnabled = currentSettings.studioMasteringEnabled
                )
                _generationSuccess.value = true
                if (currentSettings.autoPlayGenerated) {
                    audioPlayer.play(
                        filePath = created.audioPath,
                        useCustomSpeed = true,
                        pitchCalibration = created.pitchCalibration
                    )
                }
            } catch (e: CartesiaApiException) {
                _generationError.value = e.message
                if (e.isAuthError) openApiKeyDialog(ApiKeyDialogTab.CARTESIA)
            } catch (e: ApiKeyException) {
                _generationError.value = e.message
                openApiKeyDialog(ApiKeyDialogTab.GEMINI)
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
    fun playAudio(
        path: String,
        useCustomSpeed: Boolean = true,
        pitchCalibration: Float = 1.0f
    ) {
        audioPlayer.play(
            filePath = path,
            useCustomSpeed = useCustomSpeed,
            pitchCalibration = pitchCalibration
        )
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    fun seekAudio(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
    }

    fun seekAudioFraction(
        path: String,
        fraction: Float,
        useCustomSpeed: Boolean = true,
        pitchCalibration: Float = 1.0f
    ) {
        audioPlayer.seekOrPlayFraction(
            filePath = path,
            fraction = fraction,
            useCustomSpeed = useCustomSpeed,
            pitchCalibration = pitchCalibration
        )
    }

    override fun onCleared() {
        super.onCleared()
        analysisJob?.cancel()
        auditionJob?.cancel()
        recordingJob?.cancel()
        audioPlayer.stop()
        clearAuditionFileOnly()
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
                val settingsManager = AppSettingsManager(application)
                val repository = VoiceRepository(application, db.voiceDao(), apiKeyManager)
                val recorder = AudioRecorder(application)
                val player = AudioPlayer(application)
                @Suppress("UNCHECKED_CAST")
                return VoiceViewModel(application, repository, settingsManager, recorder, player) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

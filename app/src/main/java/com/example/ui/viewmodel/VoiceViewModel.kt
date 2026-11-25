package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.GeneratedAudioEntity
import com.example.data.database.VoiceDatabase
import com.example.data.database.VoiceProfileEntity
import com.example.data.repository.VoiceRepository
import com.example.util.AudioPlayer
import com.example.util.AudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

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

    // --- Voice Recording State ---
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec

    private val _recordingAmplitudes = MutableStateFlow<List<Int>>(emptyList())
    val recordingAmplitudes: StateFlow<List<Int>> = _recordingAmplitudes

    private val _tempRecordFile = MutableStateFlow<File?>(null)
    val tempRecordFile: StateFlow<File?> = _tempRecordFile

    private var recordingJob: Job? = null

    // --- Speech Generation State ---
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError

    private val _generationSuccess = MutableStateFlow(false)
    val generationSuccess: StateFlow<Boolean> = _generationSuccess

    // --- Audio Player State ---
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying
    val playPosition: StateFlow<Int> = audioPlayer.currentPosition
    val playDuration: StateFlow<Int> = audioPlayer.duration
    val playingPath: StateFlow<String?> = audioPlayer.playingAudioPath

    init {
        viewModelScope.launch {
            repository.prepopulateDefaultVoicesIfEmpty()
        }
    }

    // --- Recording Control ---
    fun startRecording() {
        if (_isRecording.value) return

        try {
            val cacheDir = getApplication<Application>().cacheDir
            val tempFile = File(cacheDir, "temp_voice_sample_${System.currentTimeMillis()}.mp4")
            _tempRecordFile.value = tempFile
            audioRecorder.start(tempFile)
            _isRecording.value = true
            _recordingDurationSec.value = 0
            _recordingAmplitudes.value = emptyList()

            recordingJob = viewModelScope.launch {
                val amps = mutableListOf<Int>()
                while (_isRecording.value) {
                    delay(100)
                    _recordingDurationSec.value += 1
                    val amp = audioRecorder.getMaxAmplitude()
                    amps.add(amp)
                    if (amps.size > 40) {
                        amps.removeAt(0)
                    }
                    _recordingAmplitudes.value = amps.toList()
                }
            }
        } catch (e: Exception) {
            _generationError.value = "Failed to start recording: ${e.message}"
        }
    }

    fun stopRecordingOnly() {
        if (!_isRecording.value) return
        audioRecorder.stop()
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
    }

    fun stopRecordingAndSave(
        name: String,
        gender: String,
        accent: String,
        recordingLanguage: String = "Bangla 🇧🇩",
        pitchTone: String = "Natural Warm",
        paceSpeed: String = "1.0x",
        onComplete: () -> Unit = {}
    ) {
        if (_isRecording.value) {
            audioRecorder.stop()
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }

        val tempFile = _tempRecordFile.value ?: return
        if (!tempFile.exists() || tempFile.length() == 0L) {
            _generationError.value = "Recorded file is empty or corrupted."
            return
        }

        viewModelScope.launch {
            try {
                val profilesDir = File(getApplication<Application>().filesDir, "voice_profiles")
                if (!profilesDir.exists()) {
                    profilesDir.mkdirs()
                }

                val destFile = File(profilesDir, "profile_${System.currentTimeMillis()}.mp4")
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()

                val profile = VoiceProfileEntity(
                    name = name.ifBlank { "Custom Voice ${System.currentTimeMillis()}" },
                    gender = gender,
                    accent = accent,
                    audioPath = destFile.absolutePath,
                    isPrebuilt = false,
                    recordingLanguage = recordingLanguage,
                    pitchTone = pitchTone,
                    paceSpeed = paceSpeed
                )

                repository.insertVoiceProfile(profile)
                _tempRecordFile.value = null
                onComplete()
            } catch (e: Exception) {
                _generationError.value = "Failed to save voice profile: ${e.message}"
            }
        }
    }

    fun cancelRecording() {
        if (_isRecording.value) {
            audioRecorder.stop()
            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
        }
        _tempRecordFile.value?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        _tempRecordFile.value = null
        _recordingAmplitudes.value = emptyList()
        _recordingDurationSec.value = 0
    }

    fun playPreviewRecording() {
        _tempRecordFile.value?.let { file ->
            if (file.exists()) {
                audioPlayer.play(file.absolutePath)
            }
        }
    }

    // --- Voice generation ---
    fun generateVoice(
        title: String,
        text: String,
        voiceProfile: VoiceProfileEntity,
        language: String,
        paceSpeed: String = "1.0x"
    ) {
        if (text.isBlank()) {
            _generationError.value = "Please enter some text to speak!"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationSuccess.value = false
            try {
                repository.generateSpeech(title, text, voiceProfile, language, paceSpeed)
                _generationSuccess.value = true
            } catch (e: Exception) {
                _generationError.value = e.message ?: "An unknown error occurred."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearGenerationState() {
        _generationError.value = null
        _generationSuccess.value = false
    }

    // --- Audio Playback ---
    fun playAudio(path: String) {
        audioPlayer.play(path)
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    fun seekAudio(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        if (_isRecording.value) {
            audioRecorder.stop()
        }
    }

    // --- Profile/Audio Deletion ---
    fun deleteVoiceProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteVoiceProfile(id)
        }
    }

    fun deleteGeneratedAudio(id: Int) {
        viewModelScope.launch {
            repository.deleteGeneratedAudio(id)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VoiceViewModel::class.java)) {
                val db = VoiceDatabase.getDatabase(application)
                val repository = VoiceRepository(application, db.voiceDao())
                val recorder = AudioRecorder(application)
                val player = AudioPlayer(application)
                @Suppress("UNCHECKED_CAST")
                return VoiceViewModel(application, repository, recorder, player) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

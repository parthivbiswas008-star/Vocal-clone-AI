package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUseCustomSpeed: Boolean = true
    private var currentPitchCalibration: Float = 1.0f

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _playingAudioPath = MutableStateFlow<String?>(null)
    val playingAudioPath: StateFlow<String?> = _playingAudioPath

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private var progressTracker: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = clamped
        if (!currentUseCustomSpeed) return
        mediaPlayer?.let { player ->
            try {
                val wasPlaying = player.isPlaying
                val params = player.playbackParams
                params.speed = clamped
                params.pitch = currentPitchCalibration.coerceIn(0.84f, 1.16f)
                player.playbackParams = params
                if (!wasPlaying && player.isPlaying) {
                    player.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun play(
        filePath: String,
        useCustomSpeed: Boolean = true,
        pitchCalibration: Float = 1.0f,
        onFinished: () -> Unit = {}
    ) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            return
        }

        val clampedPitch = pitchCalibration.coerceIn(0.84f, 1.16f)

        if (_playingAudioPath.value == filePath && mediaPlayer != null) {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.pause()
                        _isPlaying.value = false
                        stopTracker()
                    } else {
                        currentUseCustomSpeed = useCustomSpeed
                        currentPitchCalibration = clampedPitch
                        val targetSpeed = if (useCustomSpeed) _playbackSpeed.value else 1.0f
                        applyParamsToPlayer(player, targetSpeed, clampedPitch)
                        player.start()
                        _isPlaying.value = true
                        startTracker()
                    }
                    return
                } catch (_: Exception) {
                    // Recreate player cleanly below
                }
            }
        }

        stop()
        currentUseCustomSpeed = useCustomSpeed
        currentPitchCalibration = clampedPitch
        _playingAudioPath.value = filePath
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(filePath)
                prepare()
                _duration.value = this.duration
                val targetSpeed = if (useCustomSpeed) _playbackSpeed.value else 1.0f
                applyParamsToPlayer(this, targetSpeed, clampedPitch)
                if (!isPlaying) {
                    start()
                }
                _isPlaying.value = true

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    _playingAudioPath.value = null
                    stopTracker()
                    onFinished()
                }

                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
            }
            startTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    private fun applyParamsToPlayer(player: MediaPlayer, speed: Float, pitch: Float) {
        try {
            val params = player.playbackParams
            params.speed = speed.coerceIn(0.5f, 2.0f)
            params.pitch = pitch.coerceIn(0.84f, 1.16f)
            player.playbackParams = params
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        stopTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = 0
        _playingAudioPath.value = null
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startTracker() {
        stopTracker()
        progressTracker = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition
                            handler.postDelayed(this, 100)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        handler.post(progressTracker!!)
    }

    private fun stopTracker() {
        progressTracker?.let {
            handler.removeCallbacks(it)
        }
        progressTracker = null
    }
}

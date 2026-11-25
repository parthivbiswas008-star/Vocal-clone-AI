package com.example.util

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _playingAudioPath = MutableStateFlow<String?>(null)
    val playingAudioPath: StateFlow<String?> = _playingAudioPath

    private var progressTracker: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    fun play(filePath: String, onFinished: () -> Unit = {}) {
        val file = File(filePath)
        if (!file.exists()) {
            return
        }

        if (_playingAudioPath.value == filePath) {
            // Already playing or paused this file
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    stopTracker()
                } else {
                    it.start()
                    _isPlaying.value = true
                    startTracker()
                }
                return
            }
        }

        // Play new file
        stop()
        _playingAudioPath.value = filePath
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
            _isPlaying.value = true
            _duration.value = this.duration

            setOnCompletionListener {
                _isPlaying.value = false
                _currentPosition.value = 0
                _playingAudioPath.value = null
                stopTracker()
                onFinished()
            }
        }
        startTracker()
    }

    fun stop() {
        stopTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _playingAudioPath.value = null
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    private fun startTracker() {
        stopTracker()
        progressTracker = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    try {
                        if (it.isPlaying) {
                            _currentPosition.value = it.currentPosition
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

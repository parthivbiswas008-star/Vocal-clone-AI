package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null

    fun start(outputFile: File, highRes: Boolean = true) {
        stop()
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(if (highRes) 48000 else 24000)
                setAudioEncodingBitRate(if (highRes) 128000 else 64000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
        } catch (e: Exception) {
            try {
                recorder.reset()
                recorder.release()
            } catch (_: Exception) {
            }
            mediaRecorder = null
            throw e
        }
    }

    fun stop() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                    // May throw if stopped immediately after start; still release cleanly
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }
}

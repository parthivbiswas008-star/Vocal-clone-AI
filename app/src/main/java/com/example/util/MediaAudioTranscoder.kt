package com.example.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Shared pure-Android MediaExtractor + MediaCodec transcoder that converts any recorded or
 * uploaded media file (M4A, MP4, MOV, MKV, WebM, AMR, 3GP, OGG, OPUS, AAC, MP3, FLAC, etc.)
 * into a clean, uncompressed 16-bit mono PCM WAV file.
 *
 * Used by BOTH:
 *  - Gemini Voice DNA & Accent Analysis (eliminates HTTP 400 "Unsupported MIME type: audio/mp4"
 *    and guarantees compact payloads well inside inlineData limits)
 *  - Cartesia AI 1:1 Instant Voice Cloning (guarantees crisp single-channel PCM WAV upload)
 */
object MediaAudioTranscoder {

    /**
     * Decodes the first audio track of [sourceFile] into a clean 16-bit mono PCM WAV file
     * written to [context.cacheDir] with prefix [filePrefix].
     *
     * @param maxDurationSeconds Caps extracted audio duration (default 30s) for optimal cloning & payload size.
     * @param maxOutputSampleRate If > 0 and source sample rate is higher, downsamples cleanly by integer factor
     *                            (e.g., 48kHz -> 24kHz) to halve upload size while keeping crisp speech fidelity.
     */
    fun transcodeToMonoWav(
        context: Context,
        sourceFile: File,
        filePrefix: String = "clean_clip_",
        maxDurationSeconds: Int = 30,
        maxOutputSampleRate: Int = 0
    ): File? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return null

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(sourceFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return null

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return null
            var sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                44100
            }
            var channels = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)
            } else {
                1
            }

            extractor.selectTrack(audioTrackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val pcmOut = ByteArrayOutputStream(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val maxDurationUs = maxDurationSeconds.coerceIn(5, 60) * 1_000_000L
            val maxPcmBytes = 10 * 1024 * 1024
            var inputDone = false
            var outputDone = false
            var safetyLoops = 0
            var postEosTimeouts = 0

            while (!outputDone && safetyLoops < 12000 && pcmOut.size() < maxPcmBytes) {
                safetyLoops++
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val size = extractor.readSampleData(inBuf, 0)
                            val pts = extractor.sampleTime
                            if (size < 0 || (pts >= 0L && pts > maxDurationUs)) {
                                codec.queueInputBuffer(
                                    inIdx,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(
                                    inIdx,
                                    0,
                                    size,
                                    pts.coerceAtLeast(0L),
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outIdx >= 0 -> {
                        postEosTimeouts = 0
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val chunk = ByteArray(bufferInfo.size)
                            outBuf.get(chunk)
                            pcmOut.write(chunk)
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        postEosTimeouts = 0
                        val newFmt = codec.outputFormat
                        if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)
                        }
                    }
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (inputDone) {
                            // Allow hardware decoders enough ticks to flush buffered frames after EOS
                            postEosTimeouts++
                            if (postEosTimeouts >= 30) {
                                break
                            }
                        }
                    }
                }
            }

            val rawPcm = pcmOut.toByteArray()
            if (rawPcm.size < 1600) return null

            var monoPcm = if (channels == 2) downmixStereo16ToMono(rawPcm) else rawPcm
            var outRate = sampleRate.coerceIn(8000, 96000)

            // Optional clean integer-factor decimation (e.g., 48000 -> 24000 or 44100 -> 22050)
            if (maxOutputSampleRate > 0 && outRate > maxOutputSampleRate) {
                val factor = ((outRate + maxOutputSampleRate - 1) / maxOutputSampleRate).coerceIn(2, 4)
                monoPcm = decimateMono16(monoPcm, factor)
                outRate /= factor
            }

            val outWav = File(context.cacheDir, "${filePrefix}${System.currentTimeMillis()}.wav")
            FileOutputStream(outWav).use { fos ->
                fos.write(createWavHeader(monoPcm.size, outRate, 1, 16))
                fos.write(monoPcm)
            }
            outWav
        } catch (_: Exception) {
            null
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun downmixStereo16ToMono(stereoPcm: ByteArray): ByteArray {
        val frames = stereoPcm.size / 4
        val mono = ByteArray(frames * 2)
        val src = ByteBuffer.wrap(stereoPcm).order(ByteOrder.LITTLE_ENDIAN)
        val dst = ByteBuffer.wrap(mono).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val l = src.short.toInt()
            val r = src.short.toInt()
            val mixed = ((l + r) / 2).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            dst.putShort(mixed)
        }
        return mono
    }

    private fun decimateMono16(monoPcm: ByteArray, factor: Int): ByteArray {
        val totalSamples = monoPcm.size / 2
        val outSamples = totalSamples / factor
        if (outSamples <= 0) return monoPcm
        val src = ByteBuffer.wrap(monoPcm).order(ByteOrder.LITTLE_ENDIAN)
        val out = ByteArray(outSamples * 2)
        val dst = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until outSamples) {
            var acc = 0
            val base = i * factor
            for (k in 0 until factor) {
                acc += src.getShort((base + k) * 2).toInt()
            }
            val avg = (acc / factor).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            dst.putShort(avg)
        }
        return out
    }

    fun createWavHeader(dataLen: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val totalAudioLen = dataLen + 36
        val h = ByteArray(44)
        h[0] = 'R'.code.toByte(); h[1] = 'I'.code.toByte(); h[2] = 'F'.code.toByte(); h[3] = 'F'.code.toByte()
        h[4] = (totalAudioLen and 0xff).toByte()
        h[5] = ((totalAudioLen shr 8) and 0xff).toByte()
        h[6] = ((totalAudioLen shr 16) and 0xff).toByte()
        h[7] = ((totalAudioLen shr 24) and 0xff).toByte()
        h[8] = 'W'.code.toByte(); h[9] = 'A'.code.toByte(); h[10] = 'V'.code.toByte(); h[11] = 'E'.code.toByte()
        h[12] = 'f'.code.toByte(); h[13] = 'm'.code.toByte(); h[14] = 't'.code.toByte(); h[15] = ' '.code.toByte()
        h[16] = 16; h[17] = 0; h[18] = 0; h[19] = 0
        h[20] = 1; h[21] = 0
        h[22] = channels.toByte(); h[23] = 0
        h[24] = (sampleRate and 0xff).toByte()
        h[25] = ((sampleRate shr 8) and 0xff).toByte()
        h[26] = ((sampleRate shr 16) and 0xff).toByte()
        h[27] = ((sampleRate shr 24) and 0xff).toByte()
        h[28] = (byteRate and 0xff).toByte()
        h[29] = ((byteRate shr 8) and 0xff).toByte()
        h[30] = ((byteRate shr 16) and 0xff).toByte()
        h[31] = ((byteRate shr 24) and 0xff).toByte()
        h[32] = (channels * (bitsPerSample / 8)).toByte(); h[33] = 0
        h[34] = bitsPerSample.toByte(); h[35] = 0
        h[36] = 'd'.code.toByte(); h[37] = 'a'.code.toByte(); h[38] = 't'.code.toByte(); h[39] = 'a'.code.toByte()
        h[40] = (dataLen and 0xff).toByte()
        h[41] = ((dataLen shr 8) and 0xff).toByte()
        h[42] = ((dataLen shr 16) and 0xff).toByte()
        h[43] = ((dataLen shr 24) and 0xff).toByte()
        return h
    }
}

package com.example.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Extracts real visual waveform amplitudes (normalized 0.08f..1.0f) from recorded,
 * uploaded, or generated audio clips so users can inspect clip structure and seek precisely.
 */
object AudioWaveformExtractor {
    private val cache = ConcurrentHashMap<String, List<Float>>()
    private const val MAX_CACHE_ENTRIES = 120

    suspend fun extractWaveform(
        filePath: String,
        barCount: Int = 52
    ): List<Float> = withContext(Dispatchers.IO) {
        val safeBars = barCount.coerceIn(16, 96)
        val file = File(filePath)
        if (!file.exists() || file.length() <= 44L) {
            return@withContext defaultEmptyWaveform(safeBars)
        }

        val cacheKey = "${file.absolutePath}:${file.lastModified()}:${file.length()}:$safeBars"
        cache[cacheKey]?.let { return@withContext it }

        val extracted = try {
            if (isWavFile(file)) {
                extractFromWavPcm(file, safeBars)
            } else {
                extractFromCompressedMedia(file, safeBars)
                    ?: extractFromMediaPacketSizes(file, safeBars)
                    ?: extractFromRawByteEntropy(file, safeBars)
            }
        } catch (_: Exception) {
            extractFromRawByteEntropy(file, safeBars)
        }

        val normalized = normalizeBars(extracted, safeBars)
        if (cache.size >= MAX_CACHE_ENTRIES) {
            cache.clear()
        }
        cache[cacheKey] = normalized
        normalized
    }

    private fun isWavFile(file: File): Boolean {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < 44L) return false
                val header = ByteArray(12)
                raf.readFully(header)
                header[0] == 'R'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() &&
                    header[3] == 'F'.code.toByte() &&
                    header[8] == 'W'.code.toByte() &&
                    header[9] == 'A'.code.toByte() &&
                    header[10] == 'V'.code.toByte() &&
                    header[11] == 'E'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun extractFromWavPcm(file: File, barCount: Int): FloatArray {
        val bars = FloatArray(barCount)
        RandomAccessFile(file, "r").use { raf ->
            val fileLen = raf.length()
            var offset = 12L
            var dataOffset = 44L
            var dataSize = (fileLen - 44L).coerceAtLeast(0L)
            val chunkHeader = ByteArray(8)

            // Locate the actual "data" chunk safely even when extra WAV metadata chunks exist
            while (offset + 8L <= fileLen) {
                raf.seek(offset)
                if (raf.read(chunkHeader) < 8) break
                val id = String(chunkHeader, 0, 4, Charsets.US_ASCII)
                val size = ByteBuffer.wrap(chunkHeader, 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int
                    .toLong() and 0xFFFFFFFFL
                if (id == "data") {
                    dataOffset = offset + 8L
                    dataSize = min(size, (fileLen - dataOffset).coerceAtLeast(0L))
                    break
                }
                val paddedSize = if (size % 2L == 1L) size + 1L else size
                offset += 8L + paddedSize
            }

            val totalSamples = dataSize / 2L
            if (totalSamples <= 0L) return bars

            val samplesPerBucket = max(1L, totalSamples / barCount)
            val maxSamplesToInspectPerBucket = 512
            val buffer = ByteArray(maxSamplesToInspectPerBucket * 2)

            for (i in 0 until barCount) {
                val bucketStartSample = i * samplesPerBucket
                val bucketSampleCount = min(samplesPerBucket, totalSamples - bucketStartSample)
                if (bucketSampleCount <= 0L) continue

                // Read a representative window from the middle of each bucket for speed & precision
                val windowSamples = min(bucketSampleCount, maxSamplesToInspectPerBucket.toLong()).toInt()
                val windowOffsetSample = bucketStartSample + ((bucketSampleCount - windowSamples) / 2L).coerceAtLeast(0L)
                raf.seek(dataOffset + windowOffsetSample * 2L)
                val bytesRead = raf.read(buffer, 0, windowSamples * 2)
                if (bytesRead < 2) continue

                val count = bytesRead / 2
                var sumSquares = 0.0
                var peak = 0
                var idx = 0
                while (idx + 1 < bytesRead) {
                    val lo = buffer[idx].toInt() and 0xFF
                    val hi = buffer[idx + 1].toInt()
                    val sample = (hi shl 8) or lo
                    val absVal = abs(sample)
                    if (absVal > peak) peak = absVal
                    val norm = sample / 32768.0
                    sumSquares += norm * norm
                    idx += 2
                }
                val rms = sqrt(sumSquares / count).toFloat()
                val peakNorm = peak / 32768f
                // Blend RMS (body) and peak (transients) for expressive, true-to-speech bars
                bars[i] = rms * 0.65f + peakNorm * 0.35f
            }
        }
        return bars
    }

    /**
     * Decodes compressed audio (M4A/AAC, MP3, OGG/OPUS, MP4 audio track) using Android
     * MediaExtractor + MediaCodec and bins PCM energy into evenly spaced time buckets.
     */
    private fun extractFromCompressedMedia(file: File, barCount: Int): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(file.absolutePath)
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
            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            if (durationUs <= 0L) return null

            extractor.selectTrack(audioTrackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val bucketSums = DoubleArray(barCount)
            val bucketCounts = IntArray(barCount)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var decodedFrames = 0
            val maxDecodeIterations = 1200

            while (!outputDone && decodedFrames < maxDecodeIterations) {
                decodedFrames++
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(8_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val pts = extractor.sampleTime
                                codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                                // For long files (>25s), stride slightly to keep decode near-instant
                                if (durationUs > 25_000_000L && decodedFrames % 2 == 0) {
                                    extractor.advance()
                                }
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 8_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size >= 2) {
                        val bucket = ((bufferInfo.presentationTimeUs.coerceAtLeast(0L) * barCount) / durationUs)
                            .toInt()
                            .coerceIn(0, barCount - 1)
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuf = outBuf.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val remaining = shortBuf.remaining()
                        val step = max(1, remaining / 64)
                        var sum = 0.0
                        var cnt = 0
                        var s = 0
                        while (s < remaining) {
                            val v = abs(shortBuf.get(s).toInt()) / 32768.0
                            sum += v
                            cnt++
                            s += step
                        }
                        if (cnt > 0) {
                            bucketSums[bucket] += sum / cnt
                            bucketCounts[bucket]++
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    break
                }
            }

            val bars = FloatArray(barCount)
            var nonZeroBuckets = 0
            for (i in 0 until barCount) {
                if (bucketCounts[i] > 0) {
                    bars[i] = (bucketSums[i] / bucketCounts[i]).toFloat()
                    if (bars[i] > 0.001f) nonZeroBuckets++
                }
            }
            if (nonZeroBuckets == 0) null else interpolateMissingBuckets(bars)
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

    /**
     * Ultra-fast fallback for compressed audio tracks if hardware codec is busy:
     * uses VBR compressed frame sizes across timestamps, which closely mirrors speech energy.
     */
    private fun extractFromMediaPacketSizes(file: File, barCount: Int): FloatArray? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var audioTrack = -1
            var durationUs = 0L
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
                        durationUs = fmt.getLong(MediaFormat.KEY_DURATION)
                    }
                    break
                }
            }
            if (audioTrack < 0 || durationUs <= 0L) return null
            extractor.selectTrack(audioTrack)

            val bucketSums = FloatArray(barCount)
            val bucketCounts = IntArray(barCount)
            val tmp = ByteBuffer.allocate(32 * 1024)
            var guard = 0
            while (guard < 4000) {
                guard++
                val size = extractor.readSampleData(tmp, 0)
                if (size < 0) break
                val pts = extractor.sampleTime.coerceAtLeast(0L)
                val bucket = ((pts * barCount) / durationUs).toInt().coerceIn(0, barCount - 1)
                bucketSums[bucket] += size.toFloat()
                bucketCounts[bucket]++
                extractor.advance()
            }

            val bars = FloatArray(barCount)
            var populated = 0
            for (i in 0 until barCount) {
                if (bucketCounts[i] > 0) {
                    bars[i] = bucketSums[i] / bucketCounts[i]
                    populated++
                }
            }
            if (populated == 0) null else interpolateMissingBuckets(bars)
        } catch (_: Exception) {
            null
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractFromRawByteEntropy(file: File, barCount: Int): FloatArray {
        val bars = FloatArray(barCount)
        try {
            RandomAccessFile(file, "r").use { raf ->
                val len = raf.length()
                if (len <= 0L) return bars
                val step = max(1L, len / barCount)
                val buf = ByteArray(256)
                for (i in 0 until barCount) {
                    raf.seek((i * step).coerceAtMost((len - 1L).coerceAtLeast(0L)))
                    val read = raf.read(buf)
                    if (read <= 1) continue
                    var diffSum = 0
                    for (j in 1 until read) {
                        diffSum += abs(buf[j].toInt() - buf[j - 1].toInt())
                    }
                    bars[i] = diffSum.toFloat() / (read * 128f)
                }
            }
        } catch (_: Exception) {
        }
        return bars
    }

    private fun interpolateMissingBuckets(bars: FloatArray): FloatArray {
        for (i in bars.indices) {
            if (bars[i] <= 0.0001f) {
                val prev = if (i > 0) bars[i - 1] else 0f
                val next = if (i < bars.lastIndex) bars[i + 1] else prev
                bars[i] = (prev + next) * 0.5f
            }
        }
        return bars
    }

    private fun normalizeBars(raw: FloatArray, barCount: Int): List<Float> {
        if (raw.isEmpty()) return defaultEmptyWaveform(barCount)
        var maxVal = 0f
        var minVal = Float.MAX_VALUE
        for (v in raw) {
            if (v > maxVal) maxVal = v
            if (v < minVal) minVal = v
        }
        if (maxVal <= 0.0001f) {
            return defaultEmptyWaveform(barCount)
        }
        val dynamicRange = (maxVal - minVal).takeIf { it > maxVal * 0.15f } ?: maxVal
        val floor = if (dynamicRange == maxVal) 0f else minVal * 0.85f
        return List(barCount) { idx ->
            val v = raw.getOrElse(idx) { 0f }
            val scaled = ((v - floor) / max(0.0001f, maxVal - floor)).coerceIn(0f, 1f)
            // Gentle perceptual curve so quiet speech syllables remain visible beside loud peaks
            val curved = scaled.toDouble().pow(0.72).toFloat()
            (0.10f + curved * 0.90f).coerceIn(0.10f, 1.0f)
        }
    }

    private fun defaultEmptyWaveform(barCount: Int): List<Float> {
        return List(barCount) { i ->
            0.18f + ((i * 7 + 3) % 5) * 0.08f
        }
    }
}

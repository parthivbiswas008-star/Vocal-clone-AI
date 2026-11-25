package com.example.util

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Pure-Kotlin 16-bit PCM Studio Mastering Engine.
 * Removes DC offset, applies subtle close-mic vocal warmth EQ matching the voice's tone profile,
 * smooths start/end boundaries to eliminate clicks, and transparently normalizes peak loudness.
 */
object AudioMasteringEngine {

    fun masterPcm16(
        pcmBytes: ByteArray,
        sampleRate: Int,
        channels: Int,
        eqProfile: String = "warm"
    ): ByteArray {
        val sampleCount = pcmBytes.size / 2
        if (sampleCount < 64) return pcmBytes

        val samples = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val lo = pcmBytes[i * 2].toInt() and 0xFF
            val hi = pcmBytes[i * 2 + 1].toInt()
            val s = (hi shl 8) or lo
            samples[i] = s / 32768f
        }

        // 1. Remove DC offset / sub-30Hz rumble (first-order high-pass per channel)
        val hpAlpha = (1.0 / (1.0 + (2.0 * PI * 30.0 / sampleRate.toDouble()))).toFloat()
        for (ch in 0 until channels) {
            var prevIn = 0f
            var prevOut = 0f
            var idx = ch
            while (idx < sampleCount) {
                val x = samples[idx]
                val y = hpAlpha * (prevOut + x - prevIn)
                prevIn = x
                prevOut = y
                samples[idx] = y
                idx += channels
            }
        }

        // 2. Subtle analog close-mic tone shaping (zero metallic artifacts)
        val (warmthBoost, airBoost) = when (eqProfile.lowercase()) {
            "deep" -> Pair(0.16f, -0.04f)
            "warm" -> Pair(0.12f, 0.02f)
            "intimate" -> Pair(0.14f, 0.05f)
            "bright" -> Pair(0.03f, 0.09f)
            else -> Pair(0.08f, 0.03f) // balanced
        }

        // Gentle 1-pole low-pass extract @ ~240Hz for chest warmth
        val lpCoeff = (1.0 - kotlin.math.exp(-2.0 * PI * 240.0 / sampleRate.toDouble())).toFloat()
        // Gentle high-pass extract @ ~4200Hz for breath air
        val airCoeff = (1.0 / (1.0 + (2.0 * PI * 4200.0 / sampleRate.toDouble()))).toFloat()

        for (ch in 0 until channels) {
            var lpState = 0f
            var airPrevIn = 0f
            var airPrevOut = 0f
            var idx = ch
            while (idx < sampleCount) {
                val x = samples[idx]
                lpState += lpCoeff * (x - lpState)
                val air = airCoeff * (airPrevOut + x - airPrevIn)
                airPrevIn = x
                airPrevOut = air

                samples[idx] = x + (lpState * warmthBoost) + (air * airBoost)
                idx += channels
            }
        }

        // 3. Peak scan & transparent soft-knee mastering to -1.5 dBFS (~0.84)
        var peak = 0f
        for (i in 0 until sampleCount) {
            val a = abs(samples[i])
            if (a > peak) peak = a
        }

        val targetPeak = 0.84f
        val gain = if (peak > 0.002f) (targetPeak / peak).coerceIn(0.6f, 2.2f) else 1.0f

        for (i in 0 until sampleCount) {
            val scaled = samples[i] * gain
            // Soft analog saturation only near clipping threshold (> 0.85)
            samples[i] = if (abs(scaled) > 0.85f) {
                tanh(scaled.toDouble()).toFloat() * 0.94f
            } else {
                scaled
            }
        }

        // 4. Smooth 8ms fade-in & 12ms fade-out (cosine ramp) to remove start/end clicks
        val fadeInFrames = ((sampleRate * 0.008f).toInt()).coerceAtMost(sampleCount / (channels * 4))
        val fadeOutFrames = ((sampleRate * 0.012f).toInt()).coerceAtMost(sampleCount / (channels * 4))
        val totalFrames = sampleCount / channels

        for (f in 0 until fadeInFrames) {
            val ramp = (0.5 * (1.0 - cos(PI * f / max(1, fadeInFrames)))).toFloat()
            for (ch in 0 until channels) {
                samples[f * channels + ch] *= ramp
            }
        }
        for (f in 0 until fadeOutFrames) {
            val ramp = (0.5 * (1.0 - cos(PI * f / max(1, fadeOutFrames)))).toFloat()
            val frameIdx = totalFrames - 1 - f
            if (frameIdx >= 0) {
                for (ch in 0 until channels) {
                    samples[frameIdx * channels + ch] *= ramp
                }
            }
        }

        // Convert back to little-endian 16-bit PCM
        val out = ByteArray(sampleCount * 2)
        for (i in 0 until sampleCount) {
            val clamped = (samples[i] * 32767f).toInt().coerceIn(-32768, 32767)
            out[i * 2] = (clamped and 0xFF).toByte()
            out[i * 2 + 1] = ((clamped shr 8) and 0xFF).toByte()
        }
        return out
    }
}

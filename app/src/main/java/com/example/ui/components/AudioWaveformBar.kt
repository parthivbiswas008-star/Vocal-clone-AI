package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.formatAudioTime
import com.example.ui.theme.CosmicCyan
import com.example.ui.theme.CosmicMutedText
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.CosmicWhite
import com.example.ui.theme.LiquidGlassBorder
import com.example.ui.theme.LiquidGlassSurfaceElevated
import com.example.util.AudioWaveformExtractor

/**
 * Interactive visual waveform display for audio playback.
 * Renders real extracted amplitude bars, highlights played progress with a glowing
 * CosmicCyan/CosmicViolet gradient, shows a live playhead cursor, and supports tap/drag seeking.
 */
@Composable
fun AudioWaveformPlayerBar(
    audioPath: String,
    isActiveTrack: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 52,
    waveformHeight: Dp = 48.dp,
    showTimeLabels: Boolean = true,
    speedLabel: String? = null,
    accentBrush: Brush = Brush.verticalGradient(listOf(CosmicCyan, CosmicViolet)),
    testTagName: String = "audio_waveform_bar"
) {
    var amplitudes by remember(audioPath, barCount) { mutableStateOf<List<Float>?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(audioPath, barCount) {
        amplitudes = AudioWaveformExtractor.extractWaveform(audioPath, barCount)
    }

    val playbackFraction = remember(isActiveTrack, currentPositionMs, durationMs) {
        if (isActiveTrack && durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    val displayedFraction = if (isDragging) dragFraction else playbackFraction

    // Subtle shimmer while extracting amplitudes (typically <5ms for WAV)
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_shimmer_alpha"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(waveformHeight.coerceAtLeast(48.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(LiquidGlassSurfaceElevated)
                .border(
                    width = 0.9.dp,
                    color = if (isActiveTrack && isPlaying) CosmicCyan.copy(alpha = 0.55f) else LiquidGlassBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .semantics {
                    contentDescription = "Audio waveform display. Tap or drag to seek playback."
                }
                .testTag(testTagName)
                .pointerInput(audioPath, durationMs) {
                    detectTapGestures { offset ->
                        if (size.width > 0) {
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeekFraction(fraction)
                        }
                    }
                }
                .pointerInput(audioPath, durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            if (size.width > 0) {
                                dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            onSeekFraction(dragFraction)
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bars = amplitudes
                val count = bars?.size ?: barCount
                val totalWidth = size.width
                val totalHeight = size.height
                if (count <= 0 || totalWidth <= 0f || totalHeight <= 0f) return@Canvas

                val slotWidth = totalWidth / count
                val barWidth = (slotWidth * 0.58f).coerceAtLeast(2.2f)
                val centerY = totalHeight / 2f
                val playheadX = totalWidth * displayedFraction

                // Subtle horizontal baseline
                drawLine(
                    color = CosmicWhite.copy(alpha = 0.08f),
                    start = Offset(0f, centerY),
                    end = Offset(totalWidth, centerY),
                    strokeWidth = 1f
                )

                for (i in 0 until count) {
                    val amp = bars?.get(i) ?: (0.20f + ((i * 5 + 1) % 6) * 0.08f)
                    val barHeight = (totalHeight * amp.coerceIn(0.10f, 1.0f)).coerceAtLeast(4f)
                    val x = i * slotWidth + (slotWidth - barWidth) / 2f
                    val barCenterX = x + barWidth / 2f
                    val isPlayed = (isActiveTrack || isDragging) && barCenterX <= playheadX

                    if (bars == null) {
                        drawRoundRect(
                            color = CosmicWhite.copy(alpha = shimmerAlpha * 0.45f),
                            topLeft = Offset(x, centerY - barHeight / 2f),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    } else if (isPlayed) {
                        drawRoundRect(
                            brush = accentBrush,
                            topLeft = Offset(x, centerY - barHeight / 2f),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    } else {
                        drawRoundRect(
                            color = if (isActiveTrack) {
                                CosmicWhite.copy(alpha = 0.30f)
                            } else {
                                CosmicCyan.copy(alpha = 0.30f)
                            },
                            topLeft = Offset(x, centerY - barHeight / 2f),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }

                // Glowing playhead indicator when playing, paused mid-clip, or dragging
                if ((isActiveTrack && displayedFraction > 0f) || isDragging) {
                    val clampedX = playheadX.coerceIn(2f, totalWidth - 2f)
                    drawLine(
                        color = CosmicCyan.copy(alpha = 0.9f),
                        start = Offset(clampedX, 0f),
                        end = Offset(clampedX, totalHeight),
                        strokeWidth = 2.2f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.2f,
                        center = Offset(clampedX, centerY)
                    )
                    drawCircle(
                        color = CosmicCyan,
                        radius = 2.4f,
                        center = Offset(clampedX, centerY)
                    )
                }
            }
        }

        if (showTimeLabels) {
            Spacer(modifier = Modifier.height(4.dp))
            val displayedPosMs = if (isDragging && durationMs > 0) {
                (dragFraction * durationMs).toInt()
            } else if (isActiveTrack) {
                currentPositionMs
            } else {
                0
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatAudioTime(displayedPosMs),
                    fontSize = 10.sp,
                    color = if (isActiveTrack || isDragging) CosmicCyan else CosmicMutedText,
                    fontWeight = if (isActiveTrack || isDragging) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = buildString {
                        if (!speedLabel.isNullOrBlank()) {
                            append(speedLabel)
                            append(" • ")
                        }
                        append(if (durationMs > 0) formatAudioTime(durationMs) else "--:--")
                    },
                    fontSize = 10.sp,
                    color = if (isActiveTrack) CosmicCyan else CosmicMutedText
                )
            }
        }
    }
}

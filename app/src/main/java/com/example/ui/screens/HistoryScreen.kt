package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VoiceCatalog
import com.example.data.database.GeneratedAudioEntity
import com.example.ui.components.AudioWaveformPlayerBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel
import com.example.util.AudioFileSaver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HISTORY_SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@Composable
fun HistoryScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyList by viewModel.generatedAudios.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    // Global Player & Speed states
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playPosition by viewModel.playPosition.collectAsStateWithLifecycle()
    val playDuration by viewModel.playDuration.collectAsStateWithLifecycle()
    val playingPath by viewModel.playingPath.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUserMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x189D4EDD), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1500F5D4), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.2f),
                        radius = size.width * 0.6f
                    )
                )
            }
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CosmicViolet)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Audio History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicWhite
            )
        }
        Text(
            text = "Play, download to internal storage, or share your generated clips",
            fontSize = 11.sp,
            color = CosmicMutedText,
            modifier = Modifier.padding(start = 18.dp, top = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Global Consistent Speed Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LiquidGlassSurface)
                .border(1.dp, LiquidGlassBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Speed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CosmicCyan)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HISTORY_SPEED_OPTIONS.forEach { speed ->
                    val isSelected = kotlin.math.abs(playbackSpeed - speed) < 0.01f
                    val label = if (speed == 1.0f) "1x" else if (speed == 2.0f) "2x" else "${speed}x"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CosmicViolet else LiquidGlassSurfaceElevated)
                            .border(1.dp, if (isSelected) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setPlaybackSpeed(speed) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CosmicWhite else CosmicMutedText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (historyList.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = CosmicMutedText.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No generated voices yet.",
                        color = CosmicWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Head to Studio to generate your first AI speech clip.",
                        color = CosmicMutedText,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(historyList, key = { it.id }) { audio ->
                    val isActiveTrack = playingPath == audio.audioPath
                    val isCurrentlyPlaying = isPlaying && isActiveTrack

                    LiquidGlassHistoryItem(
                        audio = audio,
                        isActiveTrack = isActiveTrack,
                        isPlaying = isCurrentlyPlaying,
                        playPosition = if (isActiveTrack) playPosition else 0,
                        playDuration = if (isActiveTrack && playDuration > 0) playDuration else audio.durationMs.toInt(),
                        playbackSpeed = playbackSpeed,
                        onPlayClick = {
                            viewModel.playAudio(
                                path = audio.audioPath,
                                useCustomSpeed = true,
                                pitchCalibration = audio.pitchCalibration
                            )
                        },
                        onSeekFraction = { fraction ->
                            viewModel.seekAudioFraction(
                                path = audio.audioPath,
                                fraction = fraction,
                                useCustomSpeed = true,
                                pitchCalibration = audio.pitchCalibration
                            )
                        },
                        onDownloadClick = { viewModel.downloadAudio(audio) },
                        onShareClick = {
                            AudioFileSaver.shareAudioFile(context, audio.audioPath, audio.title)
                                .onFailure { err ->
                                    Toast.makeText(context, "Unable to share: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                        },
                        onDeleteClick = { viewModel.deleteGeneratedAudio(audio.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidGlassHistoryItem(
    audio: GeneratedAudioEntity,
    isActiveTrack: Boolean,
    isPlaying: Boolean,
    playPosition: Int,
    playDuration: Int,
    playbackSpeed: Float,
    onPlayClick: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(audio.createdAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
        sdf.format(Date(audio.createdAt))
    }
    val langFlag = remember(audio.language) {
        VoiceCatalog.flagForLanguage(audio.language)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LiquidGlassSurface)
            .border(1.dp, if (isPlaying) LiquidBorderBrush else SolidColor(LiquidGlassBorder), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) CosmicCyan else LiquidGlassSurfaceElevated)
                            .border(1.dp, CosmicCyan, CircleShape)
                            .clickable { onPlayClick() }
                            .testTag("history_play_${audio.id}")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) DeepSpaceBackground else CosmicCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${audio.voiceProfileName} • $langFlag ${audio.language}",
                                fontSize = 11.sp,
                                color = CosmicCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (audio.emotionStyle.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CosmicViolet.copy(alpha = 0.28f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = audio.emotionStyle,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CosmicWhite,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (audio.durationMs > 0) {
                                "$dateStr • ${formatAudioTime(audio.durationMs.toInt())}"
                            } else {
                                dateStr
                            },
                            fontSize = 10.sp,
                            color = CosmicMutedText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Script Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LiquidGlassSurfaceElevated)
                    .padding(10.dp)
            ) {
                Text(
                    text = audio.text,
                    fontSize = 12.sp,
                    color = CosmicWhite.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Visual Waveform Display (tap or drag to seek)
            AudioWaveformPlayerBar(
                audioPath = audio.audioPath,
                isActiveTrack = isActiveTrack,
                isPlaying = isPlaying,
                currentPositionMs = playPosition,
                durationMs = playDuration,
                onSeekFraction = onSeekFraction,
                barCount = 50,
                waveformHeight = 48.dp,
                showTimeLabels = true,
                speedLabel = if (isActiveTrack) "${playbackSpeed}x" else null,
                testTagName = "history_waveform_${audio.id}"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Download to Internal Storage, Share, Delete
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicCyan.copy(alpha = 0.16f))
                        .border(1.dp, CosmicCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { onDownloadClick() }
                        .padding(vertical = 8.dp)
                        .testTag("history_download_${audio.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = CosmicCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CosmicCyan)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LiquidGlassSurfaceElevated)
                        .border(1.dp, LiquidGlassBorder, RoundedCornerShape(10.dp))
                        .clickable { onShareClick() }
                        .padding(vertical = 8.dp)
                        .testTag("history_share_${audio.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = CosmicWhite, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Share", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CosmicWhite)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SignalRed.copy(alpha = 0.14f))
                        .border(1.dp, SignalRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onDeleteClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("history_delete_${audio.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SignalRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

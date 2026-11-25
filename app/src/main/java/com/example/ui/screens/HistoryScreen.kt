package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.database.GeneratedAudioEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyList by viewModel.generatedAudios.collectAsState()

    // Player states
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playPosition by viewModel.playPosition.collectAsState()
    val playDuration by viewModel.playDuration.collectAsState()
    val playingPath by viewModel.playingPath.collectAsState()

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
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CosmicViolet)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ইতিহাস (History)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicWhite
            )
        }
        Text(
            text = "আপনার জেনারেট করা সকল এআই ভয়েস ফাইলসমূহ",
            fontSize = 12.sp,
            color = CosmicMutedText,
            modifier = Modifier.padding(start = 18.dp, top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                        text = "ইতিহাস খালি! এখনো কোনো ভয়েস জেনারেট করা হয়নি।",
                        color = CosmicMutedText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(historyList) { audio ->
                    val isCurrentlyPlaying = isPlaying && playingPath == audio.audioPath

                    LiquidGlassHistoryItem(
                        audio = audio,
                        isPlaying = isCurrentlyPlaying,
                        playPosition = if (isCurrentlyPlaying) playPosition else 0,
                        playDuration = if (isCurrentlyPlaying) playDuration else 0,
                        onPlayClick = { viewModel.playAudio(audio.audioPath) },
                        onSeek = { viewModel.seekAudio(it) },
                        onShareClick = {
                            val file = File(audio.audioPath)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "অডিও শেয়ার করুন"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "শেয়ার করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "অডিও ফাইলটি খুঁজে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show()
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
    isPlaying: Boolean,
    playPosition: Int,
    playDuration: Int,
    onPlayClick: () -> Unit,
    onSeek: (Int) -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(audio.createdAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(audio.createdAt))
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Play Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) CosmicCyan else LiquidGlassSurfaceElevated)
                            .border(1.dp, CosmicCyan, CircleShape)
                            .clickable { onPlayClick() }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = if (isPlaying) DeepSpaceBackground else CosmicCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = audio.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${audio.voiceProfileName} • ${audio.language}",
                                fontSize = 11.sp,
                                color = CosmicCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• $dateStr",
                                fontSize = 10.sp,
                                color = CosmicMutedText
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShareClick, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = CosmicCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SignalRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Text preview
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

            // Seek slider when playing
            AnimatedVisibility(visible = isPlaying && playDuration > 0) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Slider(
                        value = playPosition.toFloat(),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..playDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = CosmicCyan,
                            activeTrackColor = CosmicCyan,
                            inactiveTrackColor = LiquidGlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format("%02d:%02d", (playPosition / 1000) / 60, (playPosition / 1000) % 60),
                            fontSize = 10.sp,
                            color = CosmicMutedText
                        )
                        Text(
                            text = String.format("%02d:%02d", (playDuration / 1000) / 60, (playDuration / 1000) % 60),
                            fontSize = 10.sp,
                            color = CosmicMutedText
                        )
                    }
                }
            }
        }
    }
}

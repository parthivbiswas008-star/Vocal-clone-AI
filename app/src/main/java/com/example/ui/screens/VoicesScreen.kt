package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VoiceCatalog
import com.example.data.database.VoiceProfileEntity
import com.example.data.repository.VoiceAnalysisResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel
import java.util.Locale

private enum class CloneInputMode { LIVE_RECORD, UPLOAD_FILE }

private val PRIMARY_FILTER_LANGUAGES = setOf(
    "English", "Bengali", "Hindi", "Spanish", "French", "Arabic", "German", "Japanese", "Urdu"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesScreen(
    viewModel: VoiceViewModel,
    onUseVoiceInStudio: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()

    // Live recording & upload states
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val recordingAmplitudes by viewModel.recordingAmplitudes.collectAsStateWithLifecycle()
    val tempSampleFile by viewModel.tempSampleFile.collectAsStateWithLifecycle()
    val tempSampleDisplayName by viewModel.tempSampleDisplayName.collectAsStateWithLifecycle()
    val tempSampleMeta by viewModel.tempSampleMeta.collectAsStateWithLifecycle()

    // AI Accent & Voice DNA Analysis states
    val isAnalyzingVoice by viewModel.isAnalyzingVoice.collectAsStateWithLifecycle()
    val voiceAnalysis by viewModel.voiceAnalysis.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val analysisNeedsApiKey by viewModel.analysisNeedsApiKey.collectAsStateWithLifecycle()

    // Audio preview player states
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playingPath by viewModel.playingPath.collectAsStateWithLifecycle()

    // Filter state
    var activeFilter by remember { mutableStateOf("All") }

    val filteredVoices by remember(voiceProfiles, activeFilter) {
        derivedStateOf {
            when (activeFilter) {
                "All" -> voiceProfiles
                "My Clones" -> voiceProfiles.filter { !it.isPrebuilt }
                "More" -> voiceProfiles.filter {
                    it.isPrebuilt && PRIMARY_FILTER_LANGUAGES.none { primary ->
                        it.language.equals(primary, ignoreCase = true)
                    }
                }
                else -> voiceProfiles.filter { it.language.equals(activeFilter, ignoreCase = true) }
            }
        }
    }

    // Clone Modal state
    var showCloneDialog by remember { mutableStateOf(false) }
    var cloneInputMode by remember { mutableStateOf(CloneInputMode.LIVE_RECORD) }
    var newVoiceName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }
    var selectedHintLanguage by remember { mutableStateOf("English") }
    var customAccentOverride by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }
    var selectedScriptIndex by remember { mutableStateOf(0) }

    // Keep accent & gender auto-populated when AI Accent Detection finishes
    LaunchedEffect(voiceAnalysis) {
        voiceAnalysis?.let { result ->
            customAccentOverride = result.detectedAccent
            selectedGender = result.detectedGender
        }
    }

    // Zero-permission SAF Multi-Format Media Picker (audio, WhatsApp opus/ogg, voice memos, video clips)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importUploadedAudio(uri, selectedHintLanguage)
        }
    }

    // Microphone Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required for live voice recording.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1800F5D4), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x159D4EDD), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.75f),
                        radius = size.width * 0.7f
                    )
                )
            }
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CosmicCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Models & Cloning",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite
                    )
                }
                Text(
                    text = "${voiceProfiles.size} multilingual voices • Live mic & multi-format upload",
                    fontSize = 11.sp,
                    color = CosmicMutedText,
                    modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(LiquidActiveButtonBrush)
                    .clickable {
                        viewModel.cancelCloningSession()
                        newVoiceName = ""
                        selectedGender = "Male"
                        selectedHintLanguage = "English"
                        customAccentOverride = ""
                        customDescription = ""
                        selectedScriptIndex = 0
                        cloneInputMode = CloneInputMode.LIVE_RECORD
                        showCloneDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("add_voice_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Clone Voice", tint = DeepSpaceBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clone Voice", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepSpaceBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips with Country Flags
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(VoiceCatalog.voiceFilterOptions, key = { it.key }) { option ->
                val isSelected = activeFilter == option.key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) CosmicCyan else LiquidGlassSurface)
                        .border(
                            1.dp,
                            if (isSelected) CosmicCyan else LiquidGlassBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { activeFilter = option.key }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                        .testTag("filter_${option.key.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = option.labelWithFlag,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) DeepSpaceBackground else CosmicWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredVoices.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = CosmicCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (activeFilter == "My Clones") {
                            "No cloned voices yet. Tap 'Clone Voice' to record live or upload any voice/video file!"
                        } else {
                            "No voices available in this category."
                        },
                        color = CosmicMutedText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredVoices, key = { it.id }) { profile ->
                    CharacterVoiceCard(
                        profile = profile,
                        isPlayingSample = isPlaying && playingPath != null && playingPath == profile.audioPath,
                        onUseClick = { onUseVoiceInStudio(profile.id) },
                        onPlaySample = {
                            profile.audioPath?.let { viewModel.playAudio(it, useCustomSpeed = false) }
                        },
                        onDelete = { viewModel.deleteVoiceProfile(profile.id) }
                    )
                }
            }
        }
    }

    // --- Voice Clone Studio Modal (Live Record + Multi-Format File Upload + Pure Voice DNA AI) ---
    if (showCloneDialog) {
        val scripts = VoiceCatalog.cloneReadingScripts[selectedHintLanguage]
            ?: VoiceCatalog.cloneReadingScripts["English"]!!
        val activeScript = scripts.getOrNull(selectedScriptIndex) ?: scripts.first()

        AlertDialog(
            onDismissRequest = {
                viewModel.cancelCloningSession()
                showCloneDialog = false
            },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, LiquidBorderBrush, RoundedCornerShape(24.dp)),
            confirmButton = {},
            containerColor = DeepSpaceLuminous,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Pure Voice Clone Studio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicWhite
                        )
                        Text(
                            text = "Live Recording or Any Audio/Video Upload + Accent AI",
                            fontSize = 11.sp,
                            color = CosmicCyan
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.cancelCloningSession()
                            showCloneDialog = false
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CosmicMutedText)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 580.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Voice Name
                    Text("Voice Name", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = newVoiceName,
                        onValueChange = { newVoiceName = it },
                        placeholder = { Text("e.g., My Voice, Alex Narrator...", color = CosmicMutedText, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmicWhite,
                            unfocusedTextColor = CosmicWhite,
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = LiquidGlassBorder,
                            focusedContainerColor = LiquidGlassSurface,
                            unfocusedContainerColor = LiquidGlassSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("clone_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Sample Language Selector with Country Flags (All 18 Languages)
                    Text("Sample Language", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        items(VoiceCatalog.supportedLanguages, key = { it.code }) { lang ->
                            val isSel = selectedHintLanguage == lang.displayName
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) CosmicCyan else LiquidGlassSurface)
                                    .border(1.dp, if (isSel) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedHintLanguage = lang.displayName
                                        selectedScriptIndex = 0
                                    }
                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lang.labelWithFlag,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) DeepSpaceBackground else CosmicWhite
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Input Mode Switcher: Live Voice Record vs Upload Media File
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isLive = cloneInputMode == CloneInputMode.LIVE_RECORD
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isLive) CosmicViolet else LiquidGlassSurface)
                                .border(1.dp, if (isLive) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(14.dp))
                                .clickable { cloneInputMode = CloneInputMode.LIVE_RECORD }
                                .padding(vertical = 10.dp)
                                .testTag("mode_live_record"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = CosmicWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Record", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CosmicWhite)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!isLive) CosmicViolet else LiquidGlassSurface)
                                .border(1.dp, if (!isLive) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(14.dp))
                                .clickable { cloneInputMode = CloneInputMode.UPLOAD_FILE }
                                .padding(vertical = 10.dp)
                                .testTag("mode_upload_file"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = CosmicWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload File", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CosmicWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (cloneInputMode == CloneInputMode.LIVE_RECORD) {
                        // Reading script tabs
                        if (scripts.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                items(scripts.indices.toList()) { index ->
                                    val isSel = selectedScriptIndex == index
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) LiquidGlassSurfaceElevated else LiquidGlassSurface)
                                            .border(1.dp, if (isSel) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(10.dp))
                                            .clickable { selectedScriptIndex = index }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = scripts[index].title,
                                            fontSize = 11.sp,
                                            color = if (isSel) CosmicCyan else CosmicMutedText,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Script Prompt Card
                        val hintFlag = VoiceCatalog.flagForLanguage(selectedHintLanguage)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(LiquidGlassSurfaceElevated)
                                .border(1.dp, LiquidBorderBrush, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$hintFlag Read aloud clearly (8–20 seconds):", fontSize = 11.sp, color = CosmicCyan, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "“${activeScript.text}”",
                                    fontSize = 13.sp,
                                    color = CosmicWhite,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Waveform Visualizer & Capture Info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LiquidGlassSurface)
                                .border(1.dp, LiquidGlassBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRecording) {
                                LiveRecordingWaveform(amplitudes = recordingAmplitudes)
                            } else if (tempSampleFile != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = tempSampleDisplayName ?: "Sample Ready",
                                            color = CosmicCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!tempSampleMeta.isNullOrBlank()) {
                                        Text(
                                            text = tempSampleMeta!!,
                                            color = CosmicMutedText,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            } else {
                                Text("Tap the microphone to record pure 48kHz studio sample", color = CosmicMutedText, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isRecording) {
                                String.format(Locale.US, "Recording: 00:%02d  (Tap Stop to run Accent AI)", recordingDurationSec)
                            } else if (tempSampleFile != null) {
                                "Voice captured! Preview or refine detected Voice DNA below."
                            } else {
                                "Tip: Speak naturally in a quiet spot for ultra-pure voice cloning"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRecording) SignalRed else CosmicMutedText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isRecording) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(LiquidRecordButtonBrush)
                                        .border(2.dp, Color(0xFFFF6688), CircleShape)
                                        .clickable {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                viewModel.startRecording()
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                        .testTag("start_record_button")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Start Recording", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(LiquidGlassSurfaceElevated)
                                        .border(2.dp, SignalRed, CircleShape)
                                        .clickable { viewModel.stopRecordingAndAnalyze(selectedHintLanguage) }
                                        .testTag("stop_record_button")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop and Analyze", tint = SignalRed, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    } else {
                        // MULTI-FORMAT UPLOAD FILE MODE (Audio, WhatsApp Voice Notes, Video Clips)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(LiquidGlassSurfaceElevated)
                                .border(1.2.dp, LiquidBorderBrush, RoundedCornerShape(16.dp))
                                .clickable {
                                    mediaPickerLauncher.launch(
                                        arrayOf("audio/*", "video/*", "application/ogg", "application/octet-stream")
                                    )
                                }
                                .padding(18.dp)
                                .testTag("pick_audio_file_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(CosmicCyan.copy(alpha = 0.16f))
                                        .border(1.dp, CosmicCyan, CircleShape)
                                ) {
                                    Icon(Icons.Default.AudioFile, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(26.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tempSampleDisplayName ?: "Upload Voice / Video File",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicWhite,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!tempSampleMeta.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tempSampleMeta!!,
                                        fontSize = 11.sp,
                                        color = CosmicCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MP3, WAV, M4A, OPUS, OGG, AAC, FLAC, AMR, 3GP, MP4, MOV, WebM (max 18 MB)",
                                    fontSize = 10.sp,
                                    color = CosmicMutedText,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Preview & Re-Scan buttons if we have a recorded or uploaded sample
                    if (!isRecording && tempSampleFile != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LiquidGlassSurfaceElevated)
                                    .border(1.dp, CosmicCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.playPreviewSample() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val previewPlaying = isPlaying && playingPath == tempSampleFile?.absolutePath
                                    Icon(
                                        imageVector = if (previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Preview Sample",
                                        tint = CosmicCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (previewPlaying) "Playing..." else "Preview (1x)",
                                        color = CosmicCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LiquidGlassSurface)
                                    .border(1.dp, LiquidGlassBorder, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.analyzeCurrentSample(selectedHintLanguage) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = "Re-analyze", tint = CosmicViolet, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Re-Scan AI", color = CosmicWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. AI Accent Detection & Pure Voice DNA Panel
                    if (isAnalyzingVoice) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(LiquidGlassSurfaceElevated)
                                .border(1.dp, CosmicCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = CosmicCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Pure Voice DNA & Accent Scanning...", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Extracting vocal timbre, regional accent, pitch & cadence", color = CosmicMutedText, fontSize = 10.sp)
                                }
                            }
                        }
                    } else if (analysisError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SignalRed.copy(alpha = 0.14f))
                                .border(1.dp, SignalRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = analysisError ?: "Analysis error",
                                color = CosmicWhite,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            if (analysisNeedsApiKey) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CosmicCyan)
                                        .clickable { viewModel.openApiKeyDialog() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Configure Gemini API Key",
                                        color = DeepSpaceBackground,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else if (voiceAnalysis != null) {
                        AccentDetectionResultCard(analysis = voiceAnalysis!!)
                    }

                    // Editable Detected Accent & Optional Bio
                    if (tempSampleFile != null && !isRecording) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Detected / Custom Accent", color = CosmicCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = customAccentOverride,
                            onValueChange = { customAccentOverride = it },
                            placeholder = { Text("e.g., Kolkata Bengali, Indian English, British RP...", color = CosmicMutedText, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CosmicWhite,
                                unfocusedTextColor = CosmicWhite,
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = LiquidGlassBorder,
                                focusedContainerColor = LiquidGlassSurface,
                                unfocusedContainerColor = LiquidGlassSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Short Description (Optional)", color = CosmicCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = customDescription,
                            onValueChange = { customDescription = it },
                            placeholder = { Text("Auto-filled from AI Voice DNA if left blank", color = CosmicMutedText, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CosmicWhite,
                                unfocusedTextColor = CosmicWhite,
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = LiquidGlassBorder,
                                focusedContainerColor = LiquidGlassSurface,
                                unfocusedContainerColor = LiquidGlassSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Save Voice Clone Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isAnalyzingVoice) SolidColor(LiquidGlassSurfaceElevated) else LiquidActiveButtonBrush)
                                .clickable(enabled = !isAnalyzingVoice) {
                                    if (newVoiceName.isBlank()) {
                                        Toast.makeText(context, "Please enter a name for your cloned voice.", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    viewModel.saveClonedVoiceProfile(
                                        name = newVoiceName,
                                        fallbackGender = selectedGender,
                                        customAccentOverride = customAccentOverride,
                                        hintLanguage = selectedHintLanguage,
                                        customDescription = customDescription
                                    ) { newId ->
                                        showCloneDialog = false
                                        Toast.makeText(context, "Voice clone created and selected!", Toast.LENGTH_SHORT).show()
                                        onUseVoiceInStudio(newId)
                                    }
                                }
                                .testTag("save_clone_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = DeepSpaceBackground, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save & Use Cloned Voice", color = DeepSpaceBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun AccentDetectionResultCard(
    analysis: VoiceAnalysisResult,
    modifier: Modifier = Modifier
) {
    val langFlag = VoiceCatalog.flagForLanguage(analysis.detectedLanguage)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicCyan.copy(alpha = 0.08f))
            .border(1.dp, LiquidBorderBrush, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pure Voice DNA Detected", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${analysis.confidenceScore}% Match", color = CosmicCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnalysisPill(label = "Accent", value = analysis.detectedAccent, modifier = Modifier.weight(1.3f))
                AnalysisPill(label = "Language", value = "$langFlag ${analysis.detectedLanguage}", modifier = Modifier.weight(0.95f))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnalysisPill(label = "Pitch", value = analysis.pitchProfile, modifier = Modifier.weight(1f))
                AnalysisPill(label = "Profile", value = "${analysis.detectedGender} • ${analysis.estimatedAge}", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = analysis.timbreDescription,
                color = CosmicWhite.copy(alpha = 0.9f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AnalysisPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LiquidGlassSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label.uppercase(), fontSize = 8.sp, color = CosmicMutedText, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 11.sp, color = CosmicWhite, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CharacterVoiceCard(
    profile: VoiceProfileEntity,
    isPlayingSample: Boolean,
    onUseClick: () -> Unit,
    onPlaySample: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langFlag = VoiceCatalog.flagForLanguage(profile.language)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LiquidGlassSurface)
            .border(
                1.dp,
                if (!profile.isPrebuilt) LiquidBorderBrush else SolidColor(LiquidGlassBorder),
                RoundedCornerShape(18.dp)
            )
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (!profile.isPrebuilt) CosmicCyan.copy(alpha = 0.2f) else CosmicViolet.copy(alpha = 0.22f))
                            .border(1.5.dp, if (!profile.isPrebuilt) CosmicCyan else CosmicViolet, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (!profile.isPrebuilt) Icons.Default.RecordVoiceOver else Icons.Default.SpatialAudio,
                            contentDescription = null,
                            tint = if (!profile.isPrebuilt) CosmicCyan else CosmicViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!profile.isPrebuilt) CosmicCyan.copy(alpha = 0.2f) else CosmicViolet.copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (!profile.isPrebuilt) "$langFlag CLONE" else "$langFlag ${profile.language.uppercase()}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!profile.isPrebuilt) CosmicCyan else CosmicWhite
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${profile.gender} • ${profile.detectedAccent ?: profile.accent} • ${profile.pitchTone}",
                            fontSize = 11.sp,
                            color = CosmicCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!profile.isPrebuilt) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Voice", tint = SignalRed, modifier = Modifier.size(19.dp))
                    }
                }
            }

            if (profile.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.description,
                    fontSize = 12.sp,
                    color = CosmicWhite.copy(alpha = 0.84f),
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!profile.isPrebuilt && profile.audioPath != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LiquidGlassSurfaceElevated)
                            .border(1.dp, LiquidGlassBorder, RoundedCornerShape(10.dp))
                            .clickable { onPlaySample() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPlayingSample) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play Original Sample",
                                tint = CosmicCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPlayingSample) "Playing..." else "Original Sample",
                                fontSize = 11.sp,
                                color = CosmicCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LiquidActiveButtonBrush)
                        .clickable { onUseClick() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = DeepSpaceBackground, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Use in Studio",
                            fontSize = 11.sp,
                            color = DeepSpaceBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveRecordingWaveform(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 8.dp)
    ) {
        val count = 28
        val barWidth = size.width / (count * 1.6f)
        val space = barWidth * 0.6f
        val centerY = size.height / 2f

        for (i in 0 until count) {
            val amp = amplitudes.getOrNull(amplitudes.size - count + i) ?: (500 + (i % 5) * 400)
            val normalizedAmp = (amp / 28000f).coerceIn(0.12f, 1f)
            val barHeight = size.height * normalizedAmp

            val x = i * (barWidth + space) + space / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CosmicCyan, CosmicViolet)
                ),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

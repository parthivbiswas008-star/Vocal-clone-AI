package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.VoiceCatalog
import com.example.data.database.VoiceProfileEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel
import com.example.util.AudioFileSaver

private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsScreen(
    viewModel: VoiceViewModel,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val selectedVoiceId by viewModel.selectedVoiceId.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()
    val generationSuccess by viewModel.generationSuccess.collectAsStateWithLifecycle()
    val generatedAudios by viewModel.generatedAudios.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    // Global audio playback & speed state
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playPosition by viewModel.playPosition.collectAsStateWithLifecycle()
    val playDuration by viewModel.playDuration.collectAsStateWithLifecycle()
    val playingPath by viewModel.playingPath.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()

    var textToSpeak by remember { mutableStateOf("") }
    var speechTitle by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val selectedProfile: VoiceProfileEntity? by remember(voiceProfiles, selectedVoiceId) {
        derivedStateOf {
            voiceProfiles.firstOrNull { it.id == selectedVoiceId }
                ?: voiceProfiles.firstOrNull { !it.isPrebuilt }
                ?: voiceProfiles.firstOrNull()
        }
    }

    LaunchedEffect(generationError) {
        generationError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearGenerationState()
        }
    }

    LaunchedEffect(generationSuccess) {
        if (generationSuccess) {
            Toast.makeText(context, "Voice generated successfully!", Toast.LENGTH_SHORT).show()
            viewModel.clearGenerationState()
        }
    }

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
                        colors = listOf(Color(0x2200F5D4), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.08f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1C9D4EDD), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.6f),
                        radius = size.width * 0.7f
                    )
                )
            }
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // App Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, LiquidBorderBrush, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "AI Voice Clone Maker Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "AI Voice Clone Maker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite
                    )
                    Text(
                        text = "Multilingual TTS & Voice Cloning Studio",
                        fontSize = 11.sp,
                        color = CosmicMutedText
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiquidGlassSurfaceElevated)
                    .border(0.8.dp, CosmicCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CosmicCyan)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "HD AUDIO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Voice Selector Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LiquidGlassSurface)
                .border(
                    1.dp,
                    if (selectedProfile?.isPrebuilt == false) LiquidBorderBrush else SolidColor(LiquidGlassBorder),
                    RoundedCornerShape(20.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedProfile?.isPrebuilt == false) Icons.Default.RecordVoiceOver else Icons.Default.SpatialAudio,
                            contentDescription = null,
                            tint = if (selectedProfile?.isPrebuilt == false) CosmicCyan else CosmicViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedProfile?.isPrebuilt == false) "Active Cloned Voice" else "Active Studio Character",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedProfile?.isPrebuilt == false) CosmicCyan else CosmicMutedText
                        )
                    }

                    Text(
                        text = "+ Clone Voice",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicCyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToProfiles() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LiquidGlassSurfaceElevated)
                            .border(1.dp, LiquidGlassBorder, RoundedCornerShape(14.dp))
                            .clickable { expandedDropdown = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("voice_selector_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedProfile?.name ?: "Select a Voice",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicWhite
                                    )
                                    if (selectedProfile != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (!selectedProfile!!.isPrebuilt) CosmicCyan.copy(alpha = 0.2f)
                                                    else CosmicViolet.copy(alpha = 0.25f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (!selectedProfile!!.isPrebuilt) "CLONED" else selectedProfile!!.language.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!selectedProfile!!.isPrebuilt) CosmicCyan else CosmicWhite
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = selectedProfile?.let { p ->
                                        val accent = p.detectedAccent?.takeIf { it.isNotBlank() } ?: p.accent
                                        "${p.gender} • $accent • ${p.pitchTone}"
                                    } ?: "Tap to choose a voice",
                                    fontSize = 11.sp,
                                    color = CosmicCyan
                                )
                                if (!selectedProfile?.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = selectedProfile!!.description,
                                        fontSize = 11.sp,
                                        color = CosmicMutedText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Choose Voice",
                                tint = CosmicCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 380.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DeepSpaceLuminous)
                            .border(1.dp, LiquidBorderBrush, RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create New Voice Clone", color = CosmicCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                expandedDropdown = false
                                onNavigateToProfiles()
                            }
                        )

                        HorizontalDivider(color = LiquidGlassBorder, thickness = 0.8.dp)

                        voiceProfiles.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.name,
                                                color = CosmicWhite,
                                                fontWeight = if (profile.id == selectedProfile?.id) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (!profile.isPrebuilt) CosmicCyan.copy(alpha = 0.2f)
                                                        else CosmicViolet.copy(alpha = 0.25f)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = if (!profile.isPrebuilt) "MY CLONE" else profile.language,
                                                    fontSize = 9.sp,
                                                    color = if (!profile.isPrebuilt) CosmicCyan else CosmicWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${profile.gender} • ${profile.detectedAccent ?: profile.accent}",
                                            color = CosmicCyan.copy(alpha = 0.85f),
                                            fontSize = 10.sp
                                        )
                                        if (profile.description.isNotBlank()) {
                                            Text(
                                                text = profile.description,
                                                color = CosmicMutedText,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectVoiceProfile(profile.id)
                                    // Match default target language if user chooses a language-specific character
                                    if (profile.isPrebuilt && VoiceCatalog.supportedLanguages.any { it.displayName == profile.language }) {
                                        selectedLanguage = profile.language
                                    }
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Output Language Selector
        Text(
            text = "Output Language",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CosmicCyan
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(VoiceCatalog.supportedLanguages, key = { it.code }) { lang ->
                val isSelected = selectedLanguage == lang.displayName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) CosmicCyan else LiquidGlassSurface)
                        .border(
                            1.dp,
                            if (isSelected) CosmicCyan else LiquidGlassBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedLanguage = lang.displayName }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("lang_chip_${lang.displayName.lowercase()}")
                ) {
                    Text(
                        text = lang.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) DeepSpaceBackground else CosmicWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Consistent Playback Speed Control (Hardware-synced globally across the app)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Playback Speed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicCyan
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SPEED_OPTIONS.forEach { speed ->
                    val isSelected = kotlin.math.abs(playbackSpeed - speed) < 0.01f
                    val label = if (speed == 1.0f) "1x" else if (speed == 2.0f) "2x" else "${speed}x"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CosmicViolet else LiquidGlassSurface)
                            .border(
                                1.dp,
                                if (isSelected) CosmicCyan else LiquidGlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setPlaybackSpeed(speed) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("speed_btn_${label}")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSelected) CosmicWhite else CosmicMutedText,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Optional Title
        OutlinedTextField(
            value = speechTitle,
            onValueChange = { speechTitle = it },
            label = { Text("Clip Title (Optional)", color = CosmicMutedText, fontSize = 12.sp) },
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
                .testTag("clip_title_input"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Script / Text Input Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(LiquidGlassSurface)
                .border(1.dp, LiquidBorderBrush, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Script to Speak ($selectedLanguage)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CosmicCyan
                    )

                    Row {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { textToSpeak = it }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste Text", tint = CosmicMutedText, modifier = Modifier.size(17.dp))
                        }

                        if (textToSpeak.isNotEmpty()) {
                            IconButton(
                                onClick = { textToSpeak = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Text", tint = CosmicMutedText, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }

                // Quick sample pills for selected language
                val samples = VoiceCatalog.ttsQuickSamples[selectedLanguage]
                    ?: VoiceCatalog.ttsQuickSamples["English"]!!
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(samples) { sample ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(LiquidGlassSurfaceElevated)
                                .border(0.6.dp, LiquidGlassBorder, RoundedCornerShape(10.dp))
                                .clickable { textToSpeak = sample }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = sample.take(26) + if (sample.length > 26) "..." else "",
                                fontSize = 10.sp,
                                color = CosmicMutedText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                BasicTextField(
                    value = textToSpeak,
                    onValueChange = { textToSpeak = it },
                    cursorBrush = SolidColor(CosmicCyan),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = CosmicWhite,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 105.dp, max = 180.dp)
                        .testTag("text_input_field"),
                    decorationBox = { innerTextField ->
                        if (textToSpeak.isEmpty()) {
                            Text(
                                text = "Type or paste any script here. The selected voice will speak it naturally with consistent speed...",
                                color = CosmicMutedText.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${textToSpeak.length} characters",
                        fontSize = 11.sp,
                        color = CosmicMutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Generate Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isGenerating) SolidColor(LiquidGlassSurfaceElevated) else LiquidActiveButtonBrush)
                .clickable(enabled = !isGenerating) {
                    val profile = selectedProfile
                    if (profile == null) {
                        Toast.makeText(context, "Please select a voice profile first.", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }
                    if (textToSpeak.isBlank()) {
                        Toast.makeText(context, "Please enter text to generate speech.", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }
                    viewModel.generateVoice(
                        title = speechTitle,
                        text = textToSpeak,
                        voiceProfile = profile,
                        language = selectedLanguage
                    )
                }
                .testTag("generate_voice_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = CosmicCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synthesizing High-Fidelity Audio...",
                        color = CosmicCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = DeepSpaceBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedProfile?.isPrebuilt == false) "Generate in Cloned Voice" else "Generate Speech",
                        color = DeepSpaceBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 7. Latest Generated Audio Quick Player (with Download & Share)
        val latestAudio = generatedAudios.firstOrNull()
        if (latestAudio != null) {
            val isLatestPlaying = isPlaying && playingPath == latestAudio.audioPath
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(LiquidGlassSurface)
                    .border(1.dp, LiquidBorderBrush, RoundedCornerShape(20.dp))
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
                                    .background(LiquidActiveButtonBrush)
                                    .clickable { viewModel.playAudio(latestAudio.audioPath) }
                                    .testTag("latest_play_button")
                            ) {
                                Icon(
                                    imageVector = if (isLatestPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isLatestPlaying) "Pause" else "Play",
                                    tint = DeepSpaceBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latestAudio.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${latestAudio.voiceProfileName} • ${latestAudio.language} • ${playbackSpeed}x",
                                    fontSize = 11.sp,
                                    color = CosmicCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.downloadAudio(latestAudio) },
                                modifier = Modifier.testTag("latest_download_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download Audio", tint = CosmicCyan, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = {
                                    AudioFileSaver.shareAudioFile(context, latestAudio.audioPath, latestAudio.title)
                                        .onFailure { err ->
                                            Toast.makeText(context, "Share failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                modifier = Modifier.testTag("latest_share_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Audio", tint = CosmicWhite, modifier = Modifier.size(19.dp))
                            }
                        }
                    }

                    if (playingPath == latestAudio.audioPath && playDuration > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Slider(
                            value = playPosition.toFloat().coerceIn(0f, playDuration.toFloat()),
                            onValueChange = { viewModel.seekAudio(it.toInt()) },
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
                                text = formatAudioTime(playPosition),
                                fontSize = 10.sp,
                                color = CosmicMutedText
                            )
                            Text(
                                text = formatAudioTime(playDuration),
                                fontSize = 10.sp,
                                color = CosmicMutedText
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun formatAudioTime(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
}

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import com.example.R
import com.example.data.database.VoiceProfileEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsScreen(
    viewModel: VoiceViewModel,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val voiceProfiles by viewModel.voiceProfiles.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationError by viewModel.generationError.collectAsState()
    val generationSuccess by viewModel.generationSuccess.collectAsState()
    val generatedAudios by viewModel.generatedAudios.collectAsState()

    var textToSpeak by remember { mutableStateOf("") }
    var speechTitle by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Bangla 🇧🇩") }
    var selectedPace by remember { mutableStateOf("1.0x") }
    var selectedProfile by remember { mutableStateOf<VoiceProfileEntity?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Audio Playback states
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playPosition by viewModel.playPosition.collectAsState()
    val playDuration by viewModel.playDuration.collectAsState()
    val playingPath by viewModel.playingPath.collectAsState()

    val languages = listOf("Bangla 🇧🇩", "English 🇺🇸", "Hindi 🇮🇳", "Arabic 🇸🇦", "Spanish 🇪🇸", "French 🇫🇷")
    val paces = listOf("0.8x (ধীর)", "1.0x (স্বাভাবিক)", "1.2x (দ্রুত)")

    val quickSampleTexts = mapOf(
        "Bangla 🇧🇩" to listOf(
            "“জীবন সুন্দর, যদি দেখার চোখ থাকে। সুরের মূর্ছনায় ভেসে যাক প্রতিটি সকাল।”",
            "“আজকের এআই ভয়েস প্রযুক্তিতে আমার নিজের কণ্ঠেই সব টেক্সট শুনতে পাচ্ছি!”",
            "“পডকাস্টের নতুন পর্বে সবাইকে স্বাগতম। আজকের বিষয় ভবিষ্যতের এআই প্রযুক্তি।”"
        ),
        "English 🇺🇸" to listOf(
            "“Welcome to the future of voice synthesis powered by artificial intelligence.”",
            "“The journey of a thousand miles begins with a single step. Keep believing!”",
            "“This audio was generated using my personal AI cloned voice model.”"
        ),
        "Hindi 🇮🇳" to listOf(
            "“नमस्ते दोस्तों! इस नए ऑडियो एपिसोड में आप सभी का हार्दिक स्वागत है।”",
            "“कड़ी मेहनत और सकारात्मक सोच से हर मुश्किल आसान हो जाती है।”"
        )
    )

    // Set default selected profile
    LaunchedEffect(voiceProfiles) {
        if (selectedProfile == null && voiceProfiles.isNotEmpty()) {
            selectedProfile = voiceProfiles.firstOrNull { !it.isPrebuilt } ?: voiceProfiles.firstOrNull()
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
            Toast.makeText(context, "ভয়েস সফলভাবে জেনারেট হয়েছে!", Toast.LENGTH_SHORT).show()
            viewModel.clearGenerationState()
            generatedAudios.firstOrNull()?.let {
                viewModel.playAudio(it.audioPath)
            }
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
                        center = Offset(size.width * 0.9f, size.height * 0.1f),
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

        // Brand App Logo Header with Liquid Glass Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, LiquidBorderBrush, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "AI Voice Clone Maker",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite
                    )
                    Text(
                        text = "টেক্সট টু স্পিচ ও ভয়েস ক্লোনিং",
                        fontSize = 11.sp,
                        color = CosmicMutedText
                    )
                }
            }

            // Liquid Glass Feature Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiquidGlassSurfaceElevated)
                    .border(0.8.dp, CosmicCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CosmicCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIQUID GLASS AI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Voice Profile Selector Card (Liquid Glass)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LiquidGlassSurface)
                .border(1.dp, if (selectedProfile?.isPrebuilt == false) LiquidBorderBrush else SolidColor(LiquidGlassBorder), RoundedCornerShape(20.dp))
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
                            text = if (selectedProfile?.isPrebuilt == false) "আপনার ক্লোন করা ভয়েস" else "সিলেক্টেড ভয়েস প্রোফাইল",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedProfile?.isPrebuilt == false) CosmicCyan else CosmicMutedText
                        )
                    }

                    Text(
                        text = "পরিবর্তন করতে চাপুন",
                        fontSize = 11.sp,
                        color = CosmicMutedText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown Trigger Box
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LiquidGlassSurfaceElevated)
                            .border(1.dp, LiquidGlassBorder, RoundedCornerShape(14.dp))
                            .clickable { expandedDropdown = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("voice_selector_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedProfile?.name ?: "একটি ভয়েস নির্বাচন করুন",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (selectedProfile?.isPrebuilt == false) {
                                        "✨ আপনার নিজস্ব কণ্ঠ • ${selectedProfile?.recordingLanguage} • ${selectedProfile?.pitchTone}"
                                    } else {
                                        "${selectedProfile?.gender ?: ""} • ${selectedProfile?.accent ?: ""}"
                                    },
                                    fontSize = 11.sp,
                                    color = if (selectedProfile?.isPrebuilt == false) CosmicCyan else CosmicMutedText
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = CosmicCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DeepSpaceLuminous)
                            .border(1.dp, LiquidBorderBrush, RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("+ নতুন ভয়েস ক্লোন করুন", color = CosmicCyan, fontWeight = FontWeight.Bold)
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
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.name,
                                                color = CosmicWhite,
                                                fontWeight = if (profile == selectedProfile) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            if (!profile.isPrebuilt) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(CosmicCyan.copy(alpha = 0.2f))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("CLONE", fontSize = 8.sp, color = CosmicCyan, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${profile.gender} • ${profile.pitchTone}",
                                            color = CosmicMutedText,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedProfile = profile
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Language Selection
        Text(
            text = "স্পিচ এর ভাষা (Speaking Language)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CosmicCyan
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(languages) { lang ->
                val isSelected = selectedLanguage == lang
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) CosmicCyan else LiquidGlassSurface)
                        .border(
                            1.dp,
                            if (isSelected) CosmicCyan else LiquidGlassBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedLanguage = lang }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = lang,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DeepSpaceBackground else CosmicWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Speaking Pace / Speed
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "উচ্চারণের গতি (Pace / Speed)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CosmicCyan
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                paces.forEach { pace ->
                    val cleanPace = pace.substringBefore(" ")
                    val isSel = selectedPace == cleanPace
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) CosmicViolet else LiquidGlassSurface)
                            .border(1.dp, if (isSel) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedPace = cleanPace }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pace,
                            fontSize = 10.sp,
                            color = if (isSel) CosmicWhite else CosmicMutedText,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Title Input (Optional)
        OutlinedTextField(
            value = speechTitle,
            onValueChange = { speechTitle = it },
            label = { Text("অডিওর শিরোনাম (Optional Title)", color = CosmicMutedText, fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = CosmicWhite,
                unfocusedTextColor = CosmicWhite,
                focusedBorderColor = CosmicCyan,
                unfocusedBorderColor = LiquidGlassBorder,
                focusedContainerColor = LiquidGlassSurface,
                unfocusedContainerColor = LiquidGlassSurface
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Text Input Container (Liquid Glass Card)
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
                        text = "টেক্সট লিখুন বা পেস্ট করুন:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CosmicCyan
                    )

                    Row {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let {
                                    textToSpeak = it
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CosmicMutedText, modifier = Modifier.size(16.dp))
                        }

                        if (textToSpeak.isNotEmpty()) {
                            IconButton(
                                onClick = { textToSpeak = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = CosmicMutedText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Quick Prompt Chips
                val sampleList = quickSampleTexts[selectedLanguage] ?: quickSampleTexts["Bangla 🇧🇩"]!!
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(sampleList) { sample ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(LiquidGlassSurfaceElevated)
                                .border(0.6.dp, LiquidGlassBorder, RoundedCornerShape(10.dp))
                                .clickable { textToSpeak = sample.trim('“', '”') }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sample.take(18) + "...",
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CosmicWhite, fontSize = 14.sp, lineHeight = 20.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 160.dp)
                        .testTag("text_input_field"),
                    decorationBox = { innerTextField ->
                        if (textToSpeak.isEmpty()) {
                            Text(
                                text = "এখানে আপনার যে কোনো লেখা লিখুন, এআই আপনার সিলেক্ট করা ভয়েসে তা উচ্চারণ করবে...",
                                color = CosmicMutedText.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${textToSpeak.length} অক্ষর",
                        fontSize = 11.sp,
                        color = CosmicMutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Generate Voice Button (Liquid Active Gradient Button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isGenerating) SolidColor(LiquidGlassSurfaceElevated) else LiquidActiveButtonBrush)
                .clickable(enabled = !isGenerating) {
                    val profile = selectedProfile
                    if (profile == null) {
                        Toast.makeText(context, "দয়া করে একটি ভয়েস প্রোফাইল নির্বাচন করুন!", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }
                    if (textToSpeak.isBlank()) {
                        Toast.makeText(context, "দয়া করে কিছু টেক্সট লিখুন!", Toast.LENGTH_SHORT).show()
                        return@clickable
                    }
                    viewModel.generateVoice(
                        title = speechTitle,
                        text = textToSpeak,
                        voiceProfile = profile,
                        language = selectedLanguage,
                        paceSpeed = selectedPace
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
                        text = "ভয়েস সংশ্লেষণ হচ্ছে... (Synthesizing)",
                        color = CosmicCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = null,
                        tint = DeepSpaceBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedProfile?.isPrebuilt == false) "✨ আমার ভয়েসে তৈরি করুন" else "ভয়েস তৈরি করুন (Generate Speech)",
                        color = DeepSpaceBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 7. Recent Generated Audio Player (Liquid Glass Card)
        val latestAudio = generatedAudios.firstOrNull()
        if (latestAudio != null) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CosmicCyan.copy(alpha = 0.2f))
                                    .border(1.dp, CosmicCyan, CircleShape)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = latestAudio.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${latestAudio.voiceProfileName} • ${latestAudio.language}",
                                    fontSize = 10.sp,
                                    color = CosmicCyan
                                )
                            }
                        }

                        // Play/Pause Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LiquidActiveButtonBrush)
                                .clickable { viewModel.playAudio(latestAudio.audioPath) }
                        ) {
                            Icon(
                                imageVector = if (isPlaying && playingPath == latestAudio.audioPath) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = DeepSpaceBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Progress slider if playing
                    if (playingPath == latestAudio.audioPath && playDuration > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = playPosition.toFloat(),
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

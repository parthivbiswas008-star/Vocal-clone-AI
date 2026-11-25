package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.database.VoiceProfileEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel

data class ScriptOption(val title: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceProfiles by viewModel.voiceProfiles.collectAsState()

    // Recording states
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val recordingAmplitudes by viewModel.recordingAmplitudes.collectAsState()
    val tempRecordFile by viewModel.tempRecordFile.collectAsState()

    // Audio preview player states
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playingPath by viewModel.playingPath.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newVoiceName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }
    var selectedRecordingLanguage by remember { mutableStateOf("Bangla 🇧🇩") }
    var selectedPitchTone by remember { mutableStateOf("Natural Warm") }
    var selectedAccent by remember { mutableStateOf("খাঁটি দেশীয় অ্যাকসেন্ট (Authentic Native)") }
    var selectedScriptIndex by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf(1) } // 1: Info, 2: Record & Preview

    val languages = listOf("Bangla 🇧🇩", "English 🇺🇸", "Hindi 🇮🇳", "Arabic 🇸🇦", "Spanish 🇪🇸", "French 🇫🇷")
    val genders = listOf("Male", "Female", "Other")
    val pitchTones = listOf("Natural Warm", "Deep Bass", "Bright Crisp", "Soft Clear")
    val accents = listOf(
        "খাঁটি দেশীয় অ্যাকসেন্ট (Authentic Native)",
        "পেশাদার ধারাভাষ্য (Studio Pro)",
        "মিষ্টি ও সুরভিত (Sweet Melodic)",
        "নাটকীয় ও গম্ভীর (Dramatic Bold)"
    )

    // Language-specific sample reading scripts
    val sampleScripts = mapOf(
        "Bangla 🇧🇩" to listOf(
            ScriptOption(
                "স্বাভাবিক কথোপকথন",
                "হ্যালো! আমি আমার নিজের স্বাভাবিক কণ্ঠে কথা বলছি। এই অডিও রেকর্ডিং দিয়ে আমার নিজস্ব ডিজিটাল ভয়েস ক্লোন তৈরি হচ্ছে, যাতে কৃত্রিম বুদ্ধিমত্তা আমার কন্ঠ ও অ্যাকসেন্ট সঠিকভাবে শিখে নিতে পারে।"
            ),
            ScriptOption(
                "কাব্যিক ও স্পষ্ট পাঠ",
                "আমি বাংলায় গান গাই, আমি বাংলায় কথা কই। বাংলার মাটি, বাংলার জল, বাংলার প্রকৃতি আমার হৃদয়জুড়ে। প্রতিটি শব্দের উচ্চারণে ফুটে উঠুক আমার কন্ঠের স্নিগ্ধতা।"
            ),
            ScriptOption(
                "পেশাদার ধারাভাষ্য",
                "বিজ্ঞান ও প্রযুক্তির অভাবনীয় অগ্রগতির ফলে এআই ভয়েস ক্লোনিং আজ মানুষের ভাব প্রকাশকে আরও সহজ ও গতিশীল করে তুলেছে।"
            )
        ),
        "English 🇺🇸" to listOf(
            ScriptOption(
                "Natural Conversation",
                "Hello! I am recording my authentic voice sample so that the artificial intelligence model can capture my personal accent, natural pacing, and vocal tone for speech generation."
            ),
            ScriptOption(
                "Phonetic Pangram",
                "The quick brown fox jumps gracefully over the lazy dog near the sparkling riverbank on a warm and sunny afternoon."
            ),
            ScriptOption(
                "Studio Narration",
                "Technology connects people across languages and cultures. Personalized voice cloning brings stories, audiobooks, and content to life with genuine human emotion."
            )
        ),
        "Hindi 🇮🇳" to listOf(
            ScriptOption(
                "प्राकृतिक बातचीत",
                "नमस्ते! मैं अपनी स्वाभाविक आवाज़ में यह वाक्य पढ़ रहा हूँ ताकि एआई मेरे अनोखे लहजे, पिच और बोलने के अंदाज़ को पूरी तरह सीख सके।"
            ),
            ScriptOption(
                "सुंदर विचार",
                "जीवन में हर नया दिन कुछ नया सीखने और आगे बढ़ने का एक सुनहरा अवसर लेकर आता है। आत्मविश्वास और स्पष्ट आवाज़ ही हमारी पहचान है।"
            )
        ),
        "Arabic 🇸🇦" to listOf(
            ScriptOption(
                "محادثة طبيعية",
                "مرحباً! أقوم بتسجيل نبرة صوتي الطبيعية لتمكين الذكاء الاصطناعي من استنساخ صوتي بلهجتي الأصلية وأسلوبي الخاص بكل دقة ووضوح."
            )
        ),
        "Spanish 🇪🇸" to listOf(
            ScriptOption(
                "Conversación Natural",
                "¡Hola! Estoy grabando mi voz auténtica para que la inteligencia artificial reproduzca mi entonación, tono y acento de manera natural y expresiva."
            )
        ),
        "French 🇫🇷" to listOf(
            ScriptOption(
                "Conversation Naturelle",
                "Bonjour! J'enregistre ma voix naturelle afin que l'intelligence artificielle puisse reproduire fidèlement mon accent, mon rythme et mon timbre chaleureux."
            )
        )
    )

    val currentScripts = sampleScripts[selectedRecordingLanguage] ?: sampleScripts["Bangla 🇧🇩"]!!

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            step = 2
        } else {
            Toast.makeText(context, "ভয়েস রেকর্ড করতে মাইক্রোফোনের অনুমতি আবশ্যক!", Toast.LENGTH_LONG).show()
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
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Header with Liquid Glass Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CosmicCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ভয়েস স্টুডিও (Voice Studio)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite
                    )
                }
                Text(
                    text = "আপনার ক্লোন করা এবং ডিফল্ট ভয়েসসমূহ",
                    fontSize = 12.sp,
                    color = CosmicMutedText,
                    modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(LiquidActiveButtonBrush)
                    .clickable {
                        newVoiceName = ""
                        selectedGender = "Male"
                        selectedRecordingLanguage = "Bangla 🇧🇩"
                        selectedPitchTone = "Natural Warm"
                        selectedAccent = "খাঁটি দেশীয় অ্যাকসেন্ট (Authentic Native)"
                        selectedScriptIndex = 0
                        step = 1
                        showCreateDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("add_voice_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = DeepSpaceBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ভয়েস ক্লোন", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepSpaceBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (voiceProfiles.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = CosmicCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("কোনো ভয়েস প্রোফাইল পাওয়া যায়নি", color = CosmicMutedText, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(voiceProfiles) { profile ->
                    LiquidGlassVoiceCard(
                        profile = profile,
                        onDelete = { viewModel.deleteVoiceProfile(profile.id) },
                        onPlaySample = {
                            profile.audioPath?.let { viewModel.playAudio(it) }
                        },
                        isPlaying = isPlaying && playingPath == profile.audioPath
                    )
                }
            }
        }
    }

    // --- Record Voice & Setup Dialog (Liquid Glass Modal) ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.cancelRecording()
                showCreateDialog = false
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
                    Text(
                        text = if (step == 1) "✨ ভয়েস ক্লোনিং সেটআপ" else "🎙️ ভয়েস স্যাম্পেল রেকর্ড",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite
                    )
                    IconButton(
                        onClick = {
                            viewModel.cancelRecording()
                            showCreateDialog = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CosmicMutedText)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (step == 1) {
                        // STEP 1: Info & Details
                        Text("১. ভয়েস এর নাম (Voice Name)", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = newVoiceName,
                            onValueChange = { newVoiceName = it },
                            placeholder = { Text("যেমন: আমার আসল কণ্ঠস্বর, আব্বুর কণ্ঠ...", color = CosmicMutedText, fontSize = 13.sp) },
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
                                .padding(vertical = 6.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recording Language Selection
                        Text("২. রেকর্ডিং এর ভাষা (Recording Language)", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("স্যাম্পেল টেক্সট এই ভাষায় দেওয়া হবে এবং আপনার অ্যাকসেন্ট ক্লোন করা হবে:", color = CosmicMutedText, fontSize = 11.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            items(languages) { lang ->
                                val isSel = selectedRecordingLanguage == lang
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSel) CosmicCyan else LiquidGlassSurface)
                                        .border(1.dp, if (isSel) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedRecordingLanguage = lang
                                            selectedScriptIndex = 0
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = lang,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) DeepSpaceBackground else CosmicWhite
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Gender Selection
                        Text("৩. জেন্ডার (Gender)", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            genders.forEach { gender ->
                                val isSel = selectedGender == gender
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) CosmicViolet else LiquidGlassSurface)
                                        .border(1.dp, if (isSel) CosmicViolet else LiquidGlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedGender = gender }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when(gender) {
                                            "Male" -> "পুরুষ (Male)"
                                            "Female" -> "মহিলা (Female)"
                                            else -> "অন্যান্য"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = CosmicWhite
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pitch / Tone Selection
                        Text("৪. কণ্ঠস্বরের ধরণ ও পিচ (Pitch / Tone)", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            items(pitchTones) { tone ->
                                val isSel = selectedPitchTone == tone
                                val label = when(tone) {
                                    "Natural Warm" -> "স্বাভাবিক ও উষ্ণ"
                                    "Deep Bass" -> "গম্ভীর ও ভারী বেস"
                                    "Bright Crisp" -> "উজ্জ্বল ও স্পষ্ট"
                                    "Soft Clear" -> "কোমল ও শান্ত"
                                    else -> tone
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSel) CosmicPurple else LiquidGlassSurface)
                                        .border(1.dp, if (isSel) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(14.dp))
                                        .clickable { selectedPitchTone = tone }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = CosmicWhite
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LiquidActiveButtonBrush)
                                .clickable {
                                    if (newVoiceName.isBlank()) {
                                        Toast.makeText(context, "দয়া করে ভয়েস এর একটি নাম দিন!", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        step = 2
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("পরবর্তী ধাপ: বাক্য পড়ুন ও রেকর্ড করুন", fontWeight = FontWeight.Bold, color = DeepSpaceBackground, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DeepSpaceBackground, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        // STEP 2: Record Voice with language-specific sample text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "নির্বাচিত ভাষা: $selectedRecordingLanguage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicCyan
                            )
                            TextButton(onClick = { step = 1 }) {
                                Text("ভাষা পরিবর্তন", color = CosmicViolet, fontSize = 11.sp)
                            }
                        }

                        // Script Selector Tabs
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            items(currentScripts.indices.toList()) { index ->
                                val script = currentScripts[index]
                                val isSel = selectedScriptIndex == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) LiquidGlassSurfaceElevated else LiquidGlassSurface)
                                        .border(1.dp, if (isSel) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedScriptIndex = index }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = script.title,
                                        fontSize = 11.sp,
                                        color = if (isSel) CosmicCyan else CosmicMutedText,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Selected Sample Script Reading Card
                        val activeScript = currentScripts.getOrNull(selectedScriptIndex) ?: currentScripts.first()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(LiquidGlassSurfaceElevated)
                                .border(1.dp, LiquidBorderBrush, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("নিচের বাক্যটি স্পষ্ট উচ্চারণে পড়ুন:", fontSize = 12.sp, color = CosmicCyan, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "“${activeScript.text}”",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CosmicWhite,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Waveform / Recording Visualizer
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
                            } else if (tempRecordFile != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("স্যাম্পেল রেকর্ড সম্পন্ন হয়েছে!", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("মাইক্রোফোন বাটনে ট্যাপ করে পড়া শুরু করুন", color = CosmicMutedText, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Recording timer
                        Text(
                            text = if (isRecording) String.format("● রেকর্ডিং হচ্ছে: 00:%02d / 00:20", recordingDurationSec)
                            else if (tempRecordFile != null) "রেকর্ড করা ভয়েস শুনুন অথবা সেভ করুন"
                            else "ন্যূনতম ৫-১০ সেকেন্ড পড়ুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) SignalRed else CosmicMutedText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Recording & Preview Controls
                        if (!isRecording && tempRecordFile == null) {
                            // Start Record Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(LiquidRecordButtonBrush)
                                        .border(2.dp, Color(0xFFFF6688), CircleShape)
                                        .clickable { viewModel.startRecording() }
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                        } else if (isRecording) {
                            // Stop Record Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(LiquidGlassSurfaceElevated)
                                        .border(2.dp, SignalRed, CircleShape)
                                        .clickable { viewModel.stopRecordingOnly() }
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = SignalRed, modifier = Modifier.size(32.dp))
                                }
                            }
                        } else {
                            // Preview & Save Controls
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Listen to sample button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LiquidGlassSurfaceElevated)
                                        .border(1.dp, CosmicCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { viewModel.playPreviewRecording() }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPlaying && playingPath == tempRecordFile?.absolutePath) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play Preview",
                                            tint = CosmicCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isPlaying && playingPath == tempRecordFile?.absolutePath) "প্লে হচ্ছে..." else "আপনার রেকর্ড করা ভয়েস শুনুন",
                                            color = CosmicCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Re-record
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LiquidGlassSurface)
                                            .border(1.dp, LiquidGlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.cancelRecording() }
                                        .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = CosmicWhite, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("আবার রেকর্ড", color = CosmicWhite, fontSize = 12.sp)
                                        }
                                    }

                                    // Save Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LiquidActiveButtonBrush)
                                            .clickable {
                                                viewModel.stopRecordingAndSave(
                                                    name = newVoiceName,
                                                    gender = selectedGender,
                                                    accent = selectedAccent,
                                                    recordingLanguage = selectedRecordingLanguage,
                                                    pitchTone = selectedPitchTone,
                                                    paceSpeed = "1.0x"
                                                ) {
                                                    showCreateDialog = false
                                                    Toast.makeText(context, "ভয়েস ক্লোন সফলভাবে তৈরি হয়েছে!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = DeepSpaceBackground, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ক্লোন সেভ করুন", color = DeepSpaceBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun LiquidGlassVoiceCard(
    profile: VoiceProfileEntity,
    onDelete: () -> Unit,
    onPlaySample: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LiquidGlassSurface)
            .border(1.dp, if (!profile.isPrebuilt) LiquidBorderBrush else Brush.linearGradient(listOf(LiquidGlassBorder, Color.Transparent)), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with glowing ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (!profile.isPrebuilt) CosmicCyan.copy(alpha = 0.2f) else CosmicViolet.copy(alpha = 0.2f))
                        .border(1.5.dp, if (!profile.isPrebuilt) CosmicCyan else CosmicViolet, CircleShape)
                ) {
                    Icon(
                        imageVector = if (!profile.isPrebuilt) Icons.Default.RecordVoiceOver else Icons.Default.SpatialAudio,
                        contentDescription = null,
                        tint = if (!profile.isPrebuilt) CosmicCyan else CosmicViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!profile.isPrebuilt) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SignalRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicViolet.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("STUDIO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmicViolet)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = profile.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Cloned Voice highlight tag
            if (!profile.isPrebuilt) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicCyan.copy(alpha = 0.15f))
                        .border(0.8.dp, CosmicCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("✨ আপনার নিজস্ব ক্লোন", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CosmicCyan)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "${profile.gender} • ${profile.pitchTone}",
                fontSize = 11.sp,
                color = CosmicMutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sample audio play button if custom audio sample exists
            if (!profile.isPrebuilt && profile.audioPath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LiquidGlassSurfaceElevated)
                        .clickable { onPlaySample() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CosmicCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "প্লে হচ্ছে..." else "কন্ঠস্বর শুনুন",
                            fontSize = 11.sp,
                            color = CosmicCyan,
                            fontWeight = FontWeight.SemiBold
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
            .height(44.dp)
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

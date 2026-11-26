package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
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
import com.example.ui.components.AudioWaveformPlayerBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.ApiKeyDialogTab
import com.example.ui.viewmodel.VoiceViewModel
import com.example.util.AudioFileSaver
import java.util.Locale

private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsScreen(
    viewModel: VoiceViewModel,
    onNavigateToProfiles: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val selectedVoiceId by viewModel.selectedVoiceId.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedEmotionStyle by viewModel.selectedEmotionStyle.collectAsStateWithLifecycle()
    val naturalBreathsEnabled by viewModel.naturalBreathsEnabled.collectAsStateWithLifecycle()
    val ultraHumanMode by viewModel.ultraHumanMode.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val hasCartesiaApiKey by viewModel.hasCartesiaApiKey.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
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

    val glowPrimary = CosmicCyan.copy(alpha = 0.14f)
    val glowSecondary = CosmicViolet.copy(alpha = 0.12f)
    val hasAnyApiKey = hasApiKey || hasCartesiaApiKey

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary, Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.08f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary, Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.6f),
                        radius = size.width * 0.7f
                    )
                )
            }
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // App Brand Header + Interactive API Keys Pill + Settings Icon
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

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "AI Voice Clone Maker",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasCartesiaApiKey) "Cartesia 1:1 + Studio TTS" else "Multilingual TTS & Voice Cloning",
                        fontSize = 11.sp,
                        color = CosmicMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (hasAnyApiKey) LiquidGlassSurfaceElevated else SignalRed.copy(alpha = 0.2f))
                        .border(
                            1.dp,
                            if (hasAnyApiKey) CosmicCyan.copy(alpha = 0.65f) else SignalRed.copy(alpha = 0.8f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            viewModel.openApiKeyDialog(
                                if (!hasCartesiaApiKey) ApiKeyDialogTab.CARTESIA else ApiKeyDialogTab.GEMINI
                            )
                        }
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .testTag("api_key_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Configure API Keys",
                            tint = if (hasAnyApiKey) CosmicCyan else SignalRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val keyLabel = when {
                            hasCartesiaApiKey && hasApiKey -> "2 Keys"
                            hasCartesiaApiKey -> "1:1 Key"
                            hasApiKey -> "API Key"
                            else -> "Set Keys"
                        }
                        Text(
                            text = keyLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasAnyApiKey) CosmicCyan else CosmicWhite
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LiquidGlassSurfaceElevated)
                        .border(1.dp, LiquidGlassBorder, CircleShape)
                        .clickable { onOpenSettings() }
                        .testTag("header_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = CosmicCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Inline helper banner when no API keys are configured
        if (!hasAnyApiKey) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SignalRed.copy(alpha = 0.14f))
                    .border(1.dp, SignalRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.openApiKeyDialog(ApiKeyDialogTab.CARTESIA) }
                    .padding(12.dp)
                    .testTag("missing_api_key_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = SignalRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "API Key Required (Cartesia AI / Gemini)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite
                            )
                            Text(
                                text = "Tap here to add Cartesia AI (1:1 exact clone) or Gemini API key",
                                fontSize = 10.sp,
                                color = CosmicMutedText
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open API Key Setup", tint = CosmicCyan, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                            text = if (selectedProfile?.isPrebuilt == false) "Active Cloned Voice" else "Active Voice Model (${voiceProfiles.size} Available)",
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
                                        val langFlag = VoiceCatalog.flagForLanguage(selectedProfile!!.language)
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
                                                text = when {
                                                    !selectedProfile!!.isPrebuilt && !selectedProfile!!.cartesiaVoiceId.isNullOrBlank() ->
                                                        "$langFlag 1:1 CARTESIA"
                                                    !selectedProfile!!.isPrebuilt ->
                                                        "$langFlag CLONED"
                                                    else ->
                                                        "$langFlag ${selectedProfile!!.language.uppercase()}"
                                                },
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
                                        val calInfo = when {
                                            !p.isPrebuilt && !p.cartesiaVoiceId.isNullOrBlank() ->
                                                " • Sonic-3 True 1:1 Clone"
                                            !p.isPrebuilt -> {
                                                val pitchPct = kotlin.math.round((p.pitchCalibration - 1.0f) * 100f).toInt()
                                                val pitchTag = when {
                                                    pitchPct > 0 -> "+${pitchPct}% pitch"
                                                    pitchPct < 0 -> "${pitchPct}% pitch"
                                                    else -> "0% pitch"
                                                }
                                                " • ${p.eqProfile.replaceFirstChar { it.uppercase() }} EQ ($pitchTag)"
                                            }
                                            else -> ""
                                        }
                                        "${p.gender} • $accent • ${p.pitchTone}$calInfo"
                                    } ?: "Tap to choose a voice",
                                    fontSize = 11.sp,
                                    color = CosmicCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                            .heightIn(max = 400.dp)
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
                            val langFlag = VoiceCatalog.flagForLanguage(profile.language)
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
                                                    text = when {
                                                        !profile.isPrebuilt && !profile.cartesiaVoiceId.isNullOrBlank() -> "$langFlag 1:1 CLONE"
                                                        !profile.isPrebuilt -> "$langFlag MY CLONE"
                                                        else -> "$langFlag ${profile.language}"
                                                    },
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
                                    viewModel.selectVoiceProfile(profile.id, syncLanguage = profile.isPrebuilt)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Helpful 1:1 Cartesia upgrade hint for cloned voices
                if (selectedProfile != null && !selectedProfile!!.isPrebuilt && selectedProfile!!.cartesiaVoiceId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!hasCartesiaApiKey) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicCyan.copy(alpha = 0.1f))
                                .border(0.8.dp, CosmicCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.openApiKeyDialog(ApiKeyDialogTab.CARTESIA) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("tts_cartesia_hint_pill")
                        ) {
                            Text(
                                text = "Add Cartesia AI key for 100% identical 1:1 voice clone",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CosmicCyan,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
                        }
                    } else if (appSettings.preferCartesiaCloning) {
                        Text(
                            text = "✓ Cartesia AI key ready — will auto-upgrade this clone to 1:1 exact voice on generate",
                            fontSize = 10.sp,
                            color = CosmicCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Output Language Selector with Country Flags
        Text(
            text = "Output Language (${VoiceCatalog.supportedLanguages.size} Languages)",
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
                        .clickable { viewModel.selectLanguage(lang.displayName) }
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                        .testTag("lang_chip_${lang.displayName.lowercase()}")
                ) {
                    Text(
                        text = lang.labelWithFlag,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) DeepSpaceBackground else CosmicWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2B. Human Emotion & Delivery Style Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mood, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Human Delivery & Emotion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicCyan
                )
            }
            Text(
                text = "Lifelike Prosody",
                fontSize = 10.sp,
                color = CosmicMutedText
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(VoiceCatalog.emotionStyles, key = { it.id }) { style ->
                val isSelected = selectedEmotionStyle == style.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) CosmicViolet else LiquidGlassSurface)
                        .border(
                            1.dp,
                            if (isSelected) CosmicCyan else LiquidGlassBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.selectEmotionStyle(style.id) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("emotion_chip_${style.id.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = style.labelWithIcon,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CosmicWhite else CosmicMutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2C. Ultra-Human Pro & Natural Breaths Realism Toggles
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (ultraHumanMode) CosmicCyan.copy(alpha = 0.16f) else LiquidGlassSurface)
                    .border(
                        1.dp,
                        if (ultraHumanMode) CosmicCyan.copy(alpha = 0.75f) else LiquidGlassBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { viewModel.toggleUltraHumanMode() }
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                    .testTag("toggle_ultra_human_pro")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ultra-Human Pro Engine",
                        tint = if (ultraHumanMode) CosmicCyan else CosmicMutedText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (ultraHumanMode) "Ultra-Human Pro: ON" else "Fast Engine Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ultraHumanMode) CosmicCyan else CosmicWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (ultraHumanMode) "Studio 3.1 / Pro TTS + EQ" else "Standard fast flash TTS",
                            fontSize = 9.sp,
                            color = CosmicMutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (naturalBreathsEnabled) CosmicViolet.copy(alpha = 0.22f) else LiquidGlassSurface)
                    .border(
                        1.dp,
                        if (naturalBreathsEnabled) CosmicViolet else LiquidGlassBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { viewModel.toggleNaturalBreaths() }
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                    .testTag("toggle_natural_breaths")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = "Natural Breaths and Pauses",
                        tint = if (naturalBreathsEnabled) CosmicCyan else CosmicMutedText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (naturalBreathsEnabled) "Natural Breaths: ON" else "Clean Breaths: OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (naturalBreathsEnabled) CosmicWhite else CosmicMutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (naturalBreathsEnabled) "Organic pauses & airflow" else "Tight studio phrasing",
                            fontSize = 9.sp,
                            color = CosmicMutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
        val activeLangFlag = VoiceCatalog.flagForLanguage(selectedLanguage)
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
                        text = "Script to Speak ($activeLangFlag $selectedLanguage)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CosmicCyan
                    )

                    Row {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                if (!clip.isNullOrBlank()) {
                                    textToSpeak = clip
                                }
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
                        text = if (ultraHumanMode) "Synthesizing Lifelike Studio Audio..." else "Synthesizing Pure Studio Audio...",
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
            val latestLangFlag = VoiceCatalog.flagForLanguage(latestAudio.language)
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
                                    .clickable {
                                        viewModel.playAudio(
                                            path = latestAudio.audioPath,
                                            useCustomSpeed = true,
                                            pitchCalibration = latestAudio.pitchCalibration
                                        )
                                    }
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
                                    text = "${latestAudio.voiceProfileName} • $latestLangFlag ${latestAudio.language} • ${latestAudio.emotionStyle} • ${playbackSpeed}x",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    val isLatestActive = playingPath == latestAudio.audioPath
                    val latestDurationMs = if (isLatestActive && playDuration > 0) {
                        playDuration
                    } else {
                        latestAudio.durationMs.toInt()
                    }
                    if (appSettings.showWaveforms) {
                        AudioWaveformPlayerBar(
                            audioPath = latestAudio.audioPath,
                            isActiveTrack = isLatestActive,
                            isPlaying = isLatestPlaying,
                            currentPositionMs = if (isLatestActive) playPosition else 0,
                            durationMs = latestDurationMs,
                            onSeekFraction = { fraction ->
                                viewModel.seekAudioFraction(
                                    path = latestAudio.audioPath,
                                    fraction = fraction,
                                    useCustomSpeed = true,
                                    pitchCalibration = latestAudio.pitchCalibration
                                )
                            },
                            barCount = 54,
                            waveformHeight = 52.dp,
                            showTimeLabels = true,
                            speedLabel = "${playbackSpeed}x",
                            testTagName = "latest_audio_waveform"
                        )
                    } else {
                        val safeDur = latestDurationMs.coerceAtLeast(1)
                        val progress = if (isLatestActive) {
                            (playPosition.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = progress,
                                onValueChange = { fraction ->
                                    viewModel.seekAudioFraction(
                                        path = latestAudio.audioPath,
                                        fraction = fraction,
                                        useCustomSpeed = true,
                                        pitchCalibration = latestAudio.pitchCalibration
                                    )
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = CosmicCyan,
                                    activeTrackColor = CosmicCyan,
                                    inactiveTrackColor = LiquidGlassBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formatAudioTime(if (isLatestActive) playPosition else 0),
                                    fontSize = 10.sp,
                                    color = CosmicMutedText
                                )
                                Text(
                                    text = formatAudioTime(latestDurationMs),
                                    fontSize = 10.sp,
                                    color = CosmicMutedText
                                )
                            }
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
    return String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
}

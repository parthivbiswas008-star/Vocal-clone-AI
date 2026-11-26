package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ApiKeyDialogTab
import com.example.ui.viewmodel.VoiceViewModel
import com.example.util.AccentColorPreset
import com.example.util.AppThemePreset
import java.util.Locale

private val SETTINGS_SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@Composable
fun SettingsScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val hasGeminiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val maskedGeminiKey by viewModel.maskedApiKey.collectAsStateWithLifecycle()
    val hasCartesiaKey by viewModel.hasCartesiaApiKey.collectAsStateWithLifecycle()
    val maskedCartesiaKey by viewModel.maskedCartesiaKey.collectAsStateWithLifecycle()
    val voiceProfiles by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val generatedAudios by viewModel.generatedAudios.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUserMessage()
        }
    }

    val clonedCount = remember(voiceProfiles) { voiceProfiles.count { !it.isPrebuilt } }
    val cartesiaClonedCount = remember(voiceProfiles) {
        voiceProfiles.count { !it.isPrebuilt && !it.cartesiaVoiceId.isNullOrBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CosmicCyan.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CosmicViolet.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.72f),
                        radius = size.width * 0.65f
                    )
                )
            }
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CosmicCyan.copy(alpha = 0.16f))
                    .border(1.dp, CosmicCyan, CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Studio Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicWhite
                )
                Text(
                    text = "Themes, appearance, Cartesia 1:1 cloning & audio engine",
                    fontSize = 11.sp,
                    color = CosmicMutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 1: AI VOICE ENGINES & API KEYS ---
        SettingsSectionHeader(
            icon = Icons.Default.Key,
            title = "AI Voice Engines & API Keys"
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsGlassCard {
            // Cartesia AI Key Row (Featured for 1:1 True Cloning)
            ApiKeyProviderRow(
                title = "Cartesia AI (1:1 Exact Voice Clone)",
                subtitle = if (hasCartesiaKey) "Active: $maskedCartesiaKey • Sonic-3" else "Recommended — clones your voice 100% identically",
                isActive = hasCartesiaKey,
                badgeText = if (hasCartesiaKey) "1:1 ACTIVE" else "SET KEY",
                onClick = { viewModel.openApiKeyDialog(ApiKeyDialogTab.CARTESIA) },
                testTag = "settings_cartesia_key_row"
            )

            HorizontalDivider(
                color = LiquidGlassBorder,
                thickness = 0.7.dp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // Gemini AI Key Row
            ApiKeyProviderRow(
                title = "Google Gemini AI (Voices & Accent AI)",
                subtitle = if (hasGeminiKey) "Active: $maskedGeminiKey • 37+ voices" else "Required for built-in studio voices & Accent DNA",
                isActive = hasGeminiKey,
                badgeText = if (hasGeminiKey) "ACTIVE" else "SET KEY",
                onClick = { viewModel.openApiKeyDialog(ApiKeyDialogTab.GEMINI) },
                testTag = "settings_gemini_key_row"
            )

            HorizontalDivider(
                color = LiquidGlassBorder,
                thickness = 0.7.dp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            SettingsToggleRow(
                icon = Icons.Default.RecordVoiceOver,
                title = "Prefer Cartesia 1:1 Cloning Engine",
                subtitle = "Automatically clone & speak using Cartesia Sonic-3 whenever key is configured",
                checked = settings.preferCartesiaCloning,
                onCheckedChange = { viewModel.setPreferCartesiaCloning(it) },
                testTag = "settings_toggle_prefer_cartesia"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 2: THEME & APPEARANCE ---
        SettingsSectionHeader(
            icon = Icons.Default.Palette,
            title = "Theme & Appearance"
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsGlassCard {
            Text(
                text = "App Theme",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicWhite
            )
            Text(
                text = "Tap any theme to switch the entire app instantly",
                fontSize = 10.sp,
                color = CosmicMutedText
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppThemePreset.entries, key = { it.id }) { preset ->
                    val isSelected = settings.themePreset == preset
                    val previewPalette = remember(preset, settings.accentPreset) {
                        ThemeState.resolvePalette(preset, settings.accentPreset)
                    }
                    Column(
                        modifier = Modifier
                            .width(126.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(previewPalette.background)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CosmicCyan else previewPalette.glassBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.setThemePreset(preset) }
                            .padding(10.dp)
                            .testTag("theme_card_${preset.id}")
                    ) {
                        // Mini live preview bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(previewPalette.accentPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(previewPalette.accentSecondary)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected Theme",
                                    tint = previewPalette.accentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = preset.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = previewPalette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.subtitle,
                            fontSize = 9.sp,
                            color = previewPalette.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Accent Color",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicWhite
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                AccentColorPreset.entries.forEach { accent ->
                    val isSelected = settings.accentPreset == accent
                    val swatchColor = remember(accent, settings.themePreset) {
                        ThemeState.resolvePalette(settings.themePreset, accent).accentPrimary
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setAccentPreset(accent) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("accent_chip_${accent.id}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) CosmicWhite else swatchColor.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected Accent",
                                    tint = DeepSpaceBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = accent.displayName,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CosmicCyan else CosmicMutedText
                        )
                    }
                }
            }

            HorizontalDivider(
                color = LiquidGlassBorder,
                thickness = 0.7.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            SettingsToggleRow(
                icon = Icons.Default.GraphicEq,
                title = "Show Visual Audio Waveforms",
                subtitle = "Display interactive amplitude waveforms on generated & recorded clips",
                checked = settings.showWaveforms,
                onCheckedChange = { viewModel.setShowWaveforms(it) },
                testTag = "settings_toggle_waveforms"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleRow(
                icon = Icons.Default.ViewAgenda,
                title = "Compact History Cards",
                subtitle = "Show more voice clips on screen at once in Audio History",
                checked = settings.compactCards,
                onCheckedChange = { viewModel.setCompactCards(it) },
                testTag = "settings_toggle_compact"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 3: AUDIO & STUDIO QUALITY ---
        SettingsSectionHeader(
            icon = Icons.Default.HighQuality,
            title = "Audio & Studio Quality"
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsGlassCard {
            SettingsToggleRow(
                icon = Icons.Default.Equalizer,
                title = "Studio Audio Mastering",
                subtitle = "Apply close-mic warmth EQ, DC cleanup & -1.5 dBFS normalization",
                checked = settings.studioMasteringEnabled,
                onCheckedChange = { viewModel.setStudioMasteringEnabled(it) },
                testTag = "settings_toggle_mastering"
            )

            HorizontalDivider(color = LiquidGlassBorder, thickness = 0.7.dp, modifier = Modifier.padding(vertical = 8.dp))

            SettingsToggleRow(
                icon = Icons.Default.PlayCircleOutline,
                title = "Auto-Play After Generation",
                subtitle = "Start playing newly synthesized speech immediately",
                checked = settings.autoPlayGenerated,
                onCheckedChange = { viewModel.setAutoPlayGenerated(it) },
                testTag = "settings_toggle_autoplay"
            )

            HorizontalDivider(color = LiquidGlassBorder, thickness = 0.7.dp, modifier = Modifier.padding(vertical = 8.dp))

            SettingsToggleRow(
                icon = Icons.Default.Mic,
                title = "48 kHz Studio Mic Recording",
                subtitle = "Capture ultra-crisp 48 kHz voice samples for closest cloning fidelity",
                checked = settings.highResRecording,
                onCheckedChange = { viewModel.setHighResRecording(it) },
                testTag = "settings_toggle_highres"
            )

            HorizontalDivider(color = LiquidGlassBorder, thickness = 0.7.dp, modifier = Modifier.padding(vertical = 8.dp))

            SettingsToggleRow(
                icon = Icons.Default.AutoAwesome,
                title = "Ultra-Human Pro Synthesis",
                subtitle = "Prioritize highest-realism neural TTS models by default",
                checked = settings.ultraHumanDefault,
                onCheckedChange = { viewModel.setUltraHumanDefault(it) },
                testTag = "settings_toggle_ultrahuman"
            )

            HorizontalDivider(color = LiquidGlassBorder, thickness = 0.7.dp, modifier = Modifier.padding(vertical = 8.dp))

            SettingsToggleRow(
                icon = Icons.Default.Air,
                title = "Natural Human Breaths",
                subtitle = "Include organic breath pauses and human speech rhythm by default",
                checked = settings.naturalBreathsDefault,
                onCheckedChange = { viewModel.setNaturalBreathsDefault(it) },
                testTag = "settings_toggle_breaths"
            )

            HorizontalDivider(color = LiquidGlassBorder, thickness = 0.7.dp, modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = "Default Playback Speed",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SETTINGS_SPEED_OPTIONS.forEach { spd ->
                    val isSel = kotlin.math.abs(settings.defaultPlaybackSpeed - spd) < 0.01f
                    val label = if (spd == 1.0f) "1x" else if (spd == 2.0f) "2x" else "${spd}x"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) CosmicViolet else LiquidGlassSurfaceElevated)
                            .border(1.dp, if (isSel) CosmicCyan else LiquidGlassBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setDefaultPlaybackSpeed(spd) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else CosmicMutedText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 4: STORAGE & DATA ---
        SettingsSectionHeader(
            icon = Icons.Default.Storage,
            title = "Storage & Maintenance"
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsGlassCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatPill(
                    label = "My Clones",
                    value = "$clonedCount ($cartesiaClonedCount 1:1)",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Audio Clips",
                    value = "${generatedAudios.size}",
                    modifier = Modifier.weight(0.8f)
                )
                StatPill(
                    label = "Temp Cache",
                    value = formatBytes(cacheSizeBytes),
                    modifier = Modifier.weight(0.95f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LiquidGlassSurfaceElevated)
                        .border(1.dp, CosmicCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.clearTempAudioCache() }
                        .testTag("settings_clear_cache_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Cache", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CosmicCyan)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LiquidGlassSurfaceElevated)
                        .border(1.dp, LiquidGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.resetAppearanceToDefaults() }
                        .testTag("settings_reset_theme_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = CosmicWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Theme", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CosmicWhite)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App Footer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(
                text = "AI Voice Clone Maker • Studio Edition",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CosmicMutedText
            )
            Text(
                text = "Cartesia Sonic-3 1:1 Cloning • Gemini Pro/3.1 Studio TTS",
                fontSize = 10.sp,
                color = CosmicMutedText.copy(alpha = 0.75f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CosmicCyan
        )
    }
}

@Composable
private fun SettingsGlassCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LiquidGlassSurface)
            .border(1.dp, LiquidGlassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun ApiKeyProviderRow(
    title: String,
    subtitle: String,
    isActive: Boolean,
    badgeText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isActive) CosmicCyan.copy(alpha = 0.16f) else SignalRed.copy(alpha = 0.15f))
                    .border(1.dp, if (isActive) CosmicCyan else SignalRed, CircleShape)
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Verified else Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = if (isActive) CosmicCyan else SignalRed,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = CosmicMutedText,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) CosmicCyan.copy(alpha = 0.18f) else CosmicViolet)
                .border(1.dp, if (isActive) CosmicCyan else CosmicViolet, RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) CosmicCyan else Color.White
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = CosmicMutedText,
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeepSpaceBackground,
                checkedTrackColor = CosmicCyan,
                uncheckedThumbColor = CosmicMutedText,
                uncheckedTrackColor = LiquidGlassSurfaceElevated
            )
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LiquidGlassSurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CosmicCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = CosmicMutedText
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024L -> "${bytes / 1024L} KB"
        else -> "$bytes B"
    }
}

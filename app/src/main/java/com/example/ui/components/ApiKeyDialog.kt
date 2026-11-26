package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.ApiKeyDialogTab

@Composable
fun ApiKeySetupDialog(
    selectedTab: ApiKeyDialogTab,
    onSelectTab: (ApiKeyDialogTab) -> Unit,
    // Gemini key props
    hasActiveGeminiKey: Boolean,
    isUsingCustomGeminiKey: Boolean,
    hasBuiltInGeminiKey: Boolean,
    maskedGeminiKeyPreview: String,
    // Cartesia key props
    hasActiveCartesiaKey: Boolean,
    isUsingCustomCartesiaKey: Boolean,
    hasBuiltInCartesiaKey: Boolean,
    maskedCartesiaKeyPreview: String,
    // Shared verification state
    isVerifying: Boolean,
    errorMessage: String?,
    onVerifyAndSaveGemini: (String) -> Unit,
    onClearCustomGeminiKey: () -> Unit,
    onVerifyAndSaveCartesia: (String) -> Unit,
    onClearCustomCartesiaKey: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var geminiInput by remember { mutableStateOf("") }
    var cartesiaInput by remember { mutableStateOf("") }
    var showKeyText by remember { mutableStateOf(false) }

    val isCartesia = selectedTab == ApiKeyDialogTab.CARTESIA
    val currentInput = if (isCartesia) cartesiaInput else geminiInput
    val hasActiveKey = if (isCartesia) hasActiveCartesiaKey else hasActiveGeminiKey
    val isUsingCustomKey = if (isCartesia) isUsingCustomCartesiaKey else isUsingCustomGeminiKey
    val hasBuiltInKey = if (isCartesia) hasBuiltInCartesiaKey else hasBuiltInGeminiKey
    val maskedKeyPreview = if (isCartesia) maskedCartesiaKeyPreview else maskedGeminiKeyPreview

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.93f)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, LiquidBorderBrush, RoundedCornerShape(24.dp)),
        containerColor = DeepSpaceLuminous,
        confirmButton = {},
        title = {
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
                            .background(CosmicCyan.copy(alpha = 0.16f))
                            .border(1.dp, CosmicCyan, CircleShape)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Engine API Keys",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicWhite
                        )
                        Text(
                            text = "Cartesia 1:1 Cloning + Gemini Studio",
                            fontSize = 11.sp,
                            color = CosmicMutedText
                        )
                    }
                }
                IconButton(
                    onClick = { if (!isVerifying) onDismiss() }
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
                // Provider Switcher Tabs: Cartesia AI vs Gemini AI
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProviderTabButton(
                        title = "Cartesia AI",
                        subtitle = if (hasActiveCartesiaKey) "1:1 Clone Ready" else "Exact 1:1 Clone",
                        selected = isCartesia,
                        isConfigured = hasActiveCartesiaKey,
                        onClick = {
                            showKeyText = false
                            onSelectTab(ApiKeyDialogTab.CARTESIA)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_api_cartesia")
                    )
                    ProviderTabButton(
                        title = "Gemini AI",
                        subtitle = if (hasActiveGeminiKey) "Active" else "Voices & Accent",
                        selected = !isCartesia,
                        isConfigured = hasActiveGeminiKey,
                        onClick = {
                            showKeyText = false
                            onSelectTab(ApiKeyDialogTab.GEMINI)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_api_gemini")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LiquidGlassSurface)
                        .border(
                            1.dp,
                            if (hasActiveKey) CosmicCyan.copy(alpha = 0.55f) else SignalRed.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasActiveKey) Icons.Default.Verified else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (hasActiveKey) CosmicCyan else SignalRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isUsingCustomKey -> if (isCartesia) "Cartesia Key Active (1:1 Cloning ON)" else "Custom Gemini Key Active"
                                    hasBuiltInKey -> if (isCartesia) "Built-in Cartesia Key Active" else "Built-in Gemini Secret Active"
                                    else -> if (isCartesia) "Cartesia AI Key Not Set" else "Gemini API Key Not Set"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite
                            )
                            Text(
                                text = if (hasActiveKey) {
                                    "Key: $maskedKeyPreview"
                                } else if (isCartesia) {
                                    "Add Cartesia key for 100% identical neural voice cloning (Sonic-3)"
                                } else {
                                    "Paste your free Google AI Studio Gemini key below"
                                },
                                fontSize = 11.sp,
                                color = CosmicMutedText,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick link to provider console
                val consoleUrl = if (isCartesia) "https://play.cartesia.ai/keys" else "https://aistudio.google.com/apikey"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicViolet.copy(alpha = 0.2f))
                        .border(1.dp, CosmicViolet.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(consoleUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Open $consoleUrl to get your API key.", Toast.LENGTH_LONG).show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCartesia) "Get a Cartesia AI API Key" else "Get a Free Gemini API Key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite
                            )
                            Text(
                                text = if (isCartesia) "Opens play.cartesia.ai/keys (True 1:1 Voice Clone)" else "Opens aistudio.google.com/apikey in browser",
                                fontSize = 10.sp,
                                color = CosmicCyan
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open API Key Console",
                            tint = CosmicCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isCartesia) "Paste Cartesia AI API Key" else "Paste Gemini API Key",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = currentInput,
                    onValueChange = {
                        if (isCartesia) cartesiaInput = it else geminiInput = it
                    },
                    placeholder = {
                        Text(
                            text = if (isCartesia) "sk_car_..." else "AIzaSy...",
                            color = CosmicMutedText,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                    if (!clipText.isNullOrBlank()) {
                                        if (isCartesia) cartesiaInput = clipText.trim() else geminiInput = clipText.trim()
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste API Key", tint = CosmicCyan, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showKeyText = !showKeyText }) {
                                Icon(
                                    imageVector = if (showKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKeyText) "Hide Key" else "Show Key",
                                    tint = CosmicMutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmicWhite,
                        unfocusedTextColor = CosmicWhite,
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = LiquidGlassBorder,
                        focusedContainerColor = LiquidGlassSurface,
                        unfocusedContainerColor = LiquidGlassSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(if (isCartesia) "cartesia_api_key_input" else "api_key_input")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SignalRed.copy(alpha = 0.14f))
                            .border(1.dp, SignalRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            fontSize = 11.sp,
                            color = CosmicWhite,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isVerifying) SolidColor(LiquidGlassSurfaceElevated) else LiquidActiveButtonBrush)
                        .clickable(enabled = !isVerifying) {
                            if (isCartesia) {
                                onVerifyAndSaveCartesia(cartesiaInput)
                            } else {
                                onVerifyAndSaveGemini(geminiInput)
                            }
                        }
                        .testTag("verify_save_api_key_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVerifying) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = CosmicCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCartesia) "Verifying Cartesia Key..." else "Verifying Gemini Key...",
                                color = CosmicCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DeepSpaceBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCartesia) "Verify & Save Cartesia Key" else "Verify & Save Gemini Key",
                                color = DeepSpaceBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (isUsingCustomKey) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            if (isCartesia) onClearCustomCartesiaKey() else onClearCustomGeminiKey()
                        },
                        enabled = !isVerifying,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (hasBuiltInKey) "Remove Custom Key (Use Built-in)" else "Remove Saved Key",
                            color = SignalRed,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCartesia) {
                        "Stored privately on this device. Sent only to api.cartesia.ai to create exact 1:1 voice clones."
                    } else {
                        "Stored privately on this device. Sent only to Google Gemini API for studio voices & accent analysis."
                    },
                    fontSize = 10.sp,
                    color = CosmicMutedText,
                    lineHeight = 14.sp
                )
            }
        }
    )
}

@Composable
private fun ProviderTabButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    isConfigured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) CosmicCyan.copy(alpha = 0.16f) else LiquidGlassSurface)
            .border(
                width = if (selected) 1.3.dp else 0.9.dp,
                color = if (selected) CosmicCyan else LiquidGlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isConfigured) CosmicCyan else SignalRed)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) CosmicCyan else CosmicWhite
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 9.sp,
            color = CosmicMutedText
        )
    }
}

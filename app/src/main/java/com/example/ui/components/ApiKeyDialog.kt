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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun ApiKeySetupDialog(
    hasActiveKey: Boolean,
    isUsingCustomKey: Boolean,
    hasBuiltInKey: Boolean,
    maskedKeyPreview: String,
    isVerifying: Boolean,
    errorMessage: String?,
    onVerifyAndSave: (String) -> Unit,
    onClearCustomKey: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf("") }
    var showKeyText by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
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
                            text = "Gemini API Key",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicWhite
                        )
                        Text(
                            text = "Required for Voice Cloning & Speech AI",
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LiquidGlassSurface)
                        .border(
                            1.dp,
                            if (hasActiveKey) CosmicCyan.copy(alpha = 0.5f) else SignalRed.copy(alpha = 0.5f),
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
                                    isUsingCustomKey -> "Custom Key Active"
                                    hasBuiltInKey -> "Built-in Secret Key Active"
                                    else -> "No Active API Key"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite
                            )
                            Text(
                                text = if (hasActiveKey) "Key: $maskedKeyPreview" else "Paste your free Google AI Studio key below",
                                fontSize = 11.sp,
                                color = CosmicMutedText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick link to Google AI Studio API key page
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicViolet.copy(alpha = 0.2f))
                        .border(1.dp, CosmicViolet.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Visit aistudio.google.com/apikey to get a free key.", Toast.LENGTH_LONG).show()
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
                                text = "Get a Free Gemini API Key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicWhite
                            )
                            Text(
                                text = "Opens aistudio.google.com/apikey in browser",
                                fontSize = 10.sp,
                                color = CosmicCyan
                            )
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open AI Studio", tint = CosmicCyan, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Paste Gemini API Key",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("AIzaSy...", color = CosmicMutedText, fontSize = 13.sp) },
                    singleLine = true,
                    visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                    if (!clipText.isNullOrBlank()) {
                                        apiKeyInput = clipText.trim()
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
                        .testTag("api_key_input")
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
                            onVerifyAndSave(apiKeyInput)
                        }
                        .testTag("verify_save_api_key_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVerifying) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = CosmicCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Key...", color = CosmicCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DeepSpaceBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Save Key", color = DeepSpaceBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                if (isUsingCustomKey) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onClearCustomKey() },
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
                    text = "Stored privately on this device only. Never uploaded anywhere except Google Gemini API.",
                    fontSize = 10.sp,
                    color = CosmicMutedText,
                    lineHeight = 14.sp
                )
            }
        }
    )
}

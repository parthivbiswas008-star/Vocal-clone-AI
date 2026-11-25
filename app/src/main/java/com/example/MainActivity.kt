package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ApiKeySetupDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TtsScreen
import com.example.ui.screens.VoicesScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    TTS("Studio", Icons.Default.GraphicEq, "tab_tts"),
    VOICES("Voices", Icons.Default.RecordVoiceOver, "tab_voices"),
    HISTORY("History", Icons.Default.History, "tab_history")
}

class MainActivity : ComponentActivity() {
    private val viewModel: VoiceViewModel by viewModels {
        VoiceViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(AppTab.TTS) }

                val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsStateWithLifecycle()
                val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
                val isUsingCustomApiKey by viewModel.isUsingCustomApiKey.collectAsStateWithLifecycle()
                val hasBuiltInApiKey by viewModel.hasBuiltInApiKey.collectAsStateWithLifecycle()
                val maskedApiKey by viewModel.maskedApiKey.collectAsStateWithLifecycle()
                val isVerifyingApiKey by viewModel.isVerifyingApiKey.collectAsStateWithLifecycle()
                val apiKeyDialogError by viewModel.apiKeyDialogError.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DeepSpaceBackground,
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                                .border(
                                    1.dp,
                                    LiquidBorderBrush,
                                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                                )
                        ) {
                            NavigationBar(
                                containerColor = DeepSpaceLuminous,
                                contentColor = CosmicWhite,
                                modifier = Modifier.testTag("bottom_nav")
                            ) {
                                AppTab.entries.forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (isSelected) CosmicCyan else CosmicMutedText
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CosmicCyan else CosmicMutedText
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = CosmicCyan.copy(alpha = 0.16f)
                                        ),
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.TTS -> TtsScreen(
                                viewModel = viewModel,
                                onNavigateToProfiles = { currentTab = AppTab.VOICES }
                            )
                            AppTab.VOICES -> VoicesScreen(
                                viewModel = viewModel,
                                onUseVoiceInStudio = { profileId ->
                                    viewModel.selectVoiceProfile(profileId, syncLanguage = true)
                                    currentTab = AppTab.TTS
                                }
                            )
                            AppTab.HISTORY -> HistoryScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }

                if (showApiKeyDialog) {
                    ApiKeySetupDialog(
                        hasActiveKey = hasApiKey,
                        isUsingCustomKey = isUsingCustomApiKey,
                        hasBuiltInKey = hasBuiltInApiKey,
                        maskedKeyPreview = maskedApiKey,
                        isVerifying = isVerifyingApiKey,
                        errorMessage = apiKeyDialogError,
                        onVerifyAndSave = { key -> viewModel.verifyAndSaveApiKey(key) },
                        onClearCustomKey = { viewModel.clearCustomApiKey() },
                        onDismiss = { viewModel.dismissApiKeyDialog() }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

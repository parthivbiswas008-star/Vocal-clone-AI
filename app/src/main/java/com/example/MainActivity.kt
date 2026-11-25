package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TtsScreen
import com.example.ui.screens.VoicesScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceViewModel

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    TTS("ভয়েস মেকার", Icons.Default.Audiotrack, "tab_tts"),
    VOICES("ভয়েস ক্লোন", Icons.Default.RecordVoiceOver, "tab_voices"),
    HISTORY("ইতিহাস", Icons.Default.History, "tab_history")
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

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .border(1.dp, LiquidBorderBrush, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        ) {
                            NavigationBar(
                                containerColor = DeepSpaceLuminous,
                                contentColor = CosmicWhite,
                                modifier = Modifier.testTag("bottom_nav")
                            ) {
                                AppTab.values().forEach { tab ->
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
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) CosmicCyan else CosmicMutedText
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = CosmicCyan.copy(alpha = 0.15f)
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
                                viewModel = viewModel
                            )
                            AppTab.HISTORY -> HistoryScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

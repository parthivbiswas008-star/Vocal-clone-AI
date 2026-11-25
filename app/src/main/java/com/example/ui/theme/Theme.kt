package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicColorScheme = darkColorScheme(
    primary = CosmicViolet,
    secondary = CosmicIndigo,
    tertiary = CosmicCyan,
    background = DeepSpaceBackground,
    surface = SpaceCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = DeepSpaceBackground,
    onBackground = CosmicWhite,
    onSurface = CosmicWhite,
    surfaceVariant = SpaceCardElevated,
    onSurfaceVariant = CosmicWhite,
    error = SignalRed
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicViolet,
    secondary = CosmicIndigo,
    tertiary = CosmicCyan,
    background = Color(0xFFF7F5FB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF16132A),
    onSurface = Color(0xFF16132A),
    surfaceVariant = Color(0xFFECE9F3)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for that premium studio feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

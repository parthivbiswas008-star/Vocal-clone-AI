package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.example.util.AccentColorPreset
import com.example.util.AppThemePreset

@Composable
fun MyApplicationTheme(
    themePreset: AppThemePreset = AppThemePreset.COSMIC_GLASS,
    accentPreset: AccentColorPreset = AccentColorPreset.CYAN,
    content: @Composable () -> Unit
) {
    val palette = ThemeState.resolvePalette(themePreset, accentPreset)
    if (ThemeState.current != palette) {
        ThemeState.current = palette
    }
    SideEffect {
        ThemeState.current = palette
    }

    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.accentPrimary,
            secondary = palette.accentSecondary,
            tertiary = palette.accentPurple,
            background = palette.background,
            surface = palette.luminousSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.glassSurfaceElevated,
            onSurfaceVariant = palette.textMuted,
            error = palette.signalRed
        )
    } else {
        darkColorScheme(
            primary = palette.accentSecondary,
            secondary = palette.accentDeep,
            tertiary = palette.accentPrimary,
            background = palette.background,
            surface = palette.card,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = palette.background,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.cardElevated,
            onSurfaceVariant = palette.textPrimary,
            error = palette.signalRed
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

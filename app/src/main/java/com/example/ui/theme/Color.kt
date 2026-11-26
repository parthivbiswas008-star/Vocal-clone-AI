package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.util.AccentColorPreset
import com.example.util.AppThemePreset

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

data class AppPalette(
    val isLight: Boolean,
    val background: Color,
    val luminousSurface: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassSurfaceLight: Color,
    val glassBorder: Color,
    val glassBorderGlow: Color,
    val card: Color,
    val cardElevated: Color,
    val accentPrimary: Color,
    val accentPrimaryGlow: Color,
    val accentSecondary: Color,
    val accentDeep: Color,
    val accentPurple: Color,
    val magenta: Color,
    val gold: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val signalRed: Color
) {
    val liquidGlassBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                accentPrimary.copy(alpha = if (isLight) 0.14f else 0.22f),
                accentSecondary.copy(alpha = if (isLight) 0.08f else 0.12f),
                luminousSurface.copy(alpha = if (isLight) 0.85f else 0.25f)
            )
        )

    val liquidBorderBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                accentPrimary.copy(alpha = if (isLight) 0.55f else 0.58f),
                accentSecondary.copy(alpha = if (isLight) 0.35f else 0.35f),
                textPrimary.copy(alpha = if (isLight) 0.10f else 0.14f),
                accentPrimary.copy(alpha = 0.12f)
            )
        )

    val liquidActiveButtonBrush: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(accentPrimary, accentPurple)
        )

    val liquidRecordButtonBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(Color(0xFFFF3366), Color(0xFF99002E))
        )
}

object ThemeState {
    var current: AppPalette by mutableStateOf(
        resolvePalette(AppThemePreset.COSMIC_GLASS, AccentColorPreset.CYAN)
    )

    fun resolvePalette(theme: AppThemePreset, accent: AccentColorPreset): AppPalette {
        val isLight = theme.isLight

        val (primary, primaryGlow, secondary, deep, purple) = when (accent) {
            AccentColorPreset.CYAN -> if (isLight) {
                Quint(Color(0xFF00838F), Color(0xFF00ACC1), Color(0xFF6D28D9), Color(0xFF4C1D95), Color(0xFF5B21B6))
            } else {
                Quint(Color(0xFF00F5D4), Color(0xFF70FFF0), Color(0xFF9D4EDD), Color(0xFF5A189A), Color(0xFF7928CA))
            }
            AccentColorPreset.VIOLET -> if (isLight) {
                Quint(Color(0xFF6D28D9), Color(0xFF8B5CF6), Color(0xFF2563EB), Color(0xFF1E3A8A), Color(0xFF4F46E5))
            } else {
                Quint(Color(0xFFC084FC), Color(0xFFE9D5FF), Color(0xFF6366F1), Color(0xFF3730A3), Color(0xFF7C3AED))
            }
            AccentColorPreset.CORAL -> if (isLight) {
                Quint(Color(0xFFE11D48), Color(0xFFF43F5E), Color(0xFF9333EA), Color(0xFF581C87), Color(0xFFBE185D))
            } else {
                Quint(Color(0xFFFF4D8D), Color(0xFFFF85B3), Color(0xFFA855F7), Color(0xFF6B21A8), Color(0xFFDB2777))
            }
            AccentColorPreset.GOLD -> if (isLight) {
                Quint(Color(0xFFB45309), Color(0xFFD97706), Color(0xFF7C3AED), Color(0xFF4C1D95), Color(0xFF92400E))
            } else {
                Quint(Color(0xFFFFC933), Color(0xFFFFE58F), Color(0xFFF97316), Color(0xFF9A3412), Color(0xFFEA580C))
            }
            AccentColorPreset.EMERALD -> if (isLight) {
                Quint(Color(0xFF047857), Color(0xFF059669), Color(0xFF0284C7), Color(0xFF075985), Color(0xFF0F766E))
            } else {
                Quint(Color(0xFF34D399), Color(0xFF6EE7B7), Color(0xFF06B6D4), Color(0xFF155E75), Color(0xFF0D9488))
            }
        }

        return when (theme) {
            AppThemePreset.COSMIC_GLASS -> AppPalette(
                isLight = false,
                background = Color(0xFF080B14),
                luminousSurface = Color(0xFF0F172A),
                glassSurface = Color(0x281E293B),
                glassSurfaceElevated = Color(0x40334155),
                glassSurfaceLight = Color(0x18FFFFFF),
                glassBorder = Color(0x3638BDF8),
                glassBorderGlow = primary.copy(alpha = 0.55f),
                card = Color(0xFF13182E),
                cardElevated = Color(0xFF1E2442),
                accentPrimary = primary,
                accentPrimaryGlow = primaryGlow,
                accentSecondary = secondary,
                accentDeep = deep,
                accentPurple = purple,
                magenta = Color(0xFFFF206E),
                gold = Color(0xFFFFBE0B),
                textPrimary = Color(0xFFF8FAFC),
                textMuted = Color(0xFF94A3B8),
                signalRed = Color(0xFFFF3366)
            )
            AppThemePreset.MIDNIGHT_AMOLED -> AppPalette(
                isLight = false,
                background = Color(0xFF000000),
                luminousSurface = Color(0xFF0B0D12),
                glassSurface = Color(0xFF11141C),
                glassSurfaceElevated = Color(0xFF191E29),
                glassSurfaceLight = Color(0x14FFFFFF),
                glassBorder = primary.copy(alpha = 0.24f),
                glassBorderGlow = primary.copy(alpha = 0.6f),
                card = Color(0xFF0E1117),
                cardElevated = Color(0xFF181D27),
                accentPrimary = primary,
                accentPrimaryGlow = primaryGlow,
                accentSecondary = secondary,
                accentDeep = deep,
                accentPurple = purple,
                magenta = Color(0xFFFF206E),
                gold = Color(0xFFFFBE0B),
                textPrimary = Color(0xFFF8FAFC),
                textMuted = Color(0xFF8B95A5),
                signalRed = Color(0xFFFF3B6B)
            )
            AppThemePreset.AURORA_ROYAL -> AppPalette(
                isLight = false,
                background = Color(0xFF0D071B),
                luminousSurface = Color(0xFF180E2E),
                glassSurface = Color(0x382B194D),
                glassSurfaceElevated = Color(0x523E256E),
                glassSurfaceLight = Color(0x1CFFFFFF),
                glassBorder = Color(0x45A855F7),
                glassBorderGlow = primary.copy(alpha = 0.6f),
                card = Color(0xFF1B1033),
                cardElevated = Color(0xFF28174B),
                accentPrimary = primary,
                accentPrimaryGlow = primaryGlow,
                accentSecondary = secondary,
                accentDeep = deep,
                accentPurple = purple,
                magenta = Color(0xFFFF2A85),
                gold = Color(0xFFFFCA3A),
                textPrimary = Color(0xFFFDF4FF),
                textMuted = Color(0xFFBBA8D8),
                signalRed = Color(0xFFFF477E)
            )
            AppThemePreset.EMERALD_STUDIO -> AppPalette(
                isLight = false,
                background = Color(0xFF06110E),
                luminousSurface = Color(0xFF0C1E1A),
                glassSurface = Color(0x3813302A),
                glassSurfaceElevated = Color(0x521C453C),
                glassSurfaceLight = Color(0x18FFFFFF),
                glassBorder = Color(0x4010B981),
                glassBorderGlow = primary.copy(alpha = 0.6f),
                card = Color(0xFF0F241F),
                cardElevated = Color(0xFF17352E),
                accentPrimary = primary,
                accentPrimaryGlow = primaryGlow,
                accentSecondary = secondary,
                accentDeep = deep,
                accentPurple = purple,
                magenta = Color(0xFFFF3366),
                gold = Color(0xFFFFCA3A),
                textPrimary = Color(0xFFECFDF5),
                textMuted = Color(0xFF8FB8AC),
                signalRed = Color(0xFFFF4D6D)
            )
            AppThemePreset.DAYLIGHT_FROST -> AppPalette(
                isLight = true,
                background = Color(0xFFF3F6FC),
                luminousSurface = Color(0xFFFFFFFF),
                glassSurface = Color(0xFFE7EEF9),
                glassSurfaceElevated = Color(0xFFDCE6F5),
                glassSurfaceLight = Color(0xAAFFFFFF),
                glassBorder = Color(0x400F172A),
                glassBorderGlow = primary.copy(alpha = 0.55f),
                card = Color(0xFFFFFFFF),
                cardElevated = Color(0xFFEAF0FA),
                accentPrimary = primary,
                accentPrimaryGlow = primaryGlow,
                accentSecondary = secondary,
                accentDeep = deep,
                accentPurple = purple,
                magenta = Color(0xFFD81B60),
                gold = Color(0xFFD97706),
                textPrimary = Color(0xFF0F172A),
                textMuted = Color(0xFF475569),
                signalRed = Color(0xFFDC2626)
            )
        }
    }

    private data class Quint(
        val a: Color,
        val b: Color,
        val c: Color,
        val d: Color,
        val e: Color
    )
}

// Reactive Compose-aware semantic palette accessors
val DeepSpaceBackground: Color get() = ThemeState.current.background
val DeepSpaceLuminous: Color get() = ThemeState.current.luminousSurface

val LiquidGlassSurface: Color get() = ThemeState.current.glassSurface
val LiquidGlassSurfaceElevated: Color get() = ThemeState.current.glassSurfaceElevated
val LiquidGlassSurfaceLight: Color get() = ThemeState.current.glassSurfaceLight
val LiquidGlassBorder: Color get() = ThemeState.current.glassBorder
val LiquidGlassBorderGlow: Color get() = ThemeState.current.glassBorderGlow

val SpaceCard: Color get() = ThemeState.current.card
val SpaceCardElevated: Color get() = ThemeState.current.cardElevated

val CosmicCyan: Color get() = ThemeState.current.accentPrimary
val CosmicCyanGlow: Color get() = ThemeState.current.accentPrimaryGlow
val CosmicViolet: Color get() = ThemeState.current.accentSecondary
val CosmicIndigo: Color get() = ThemeState.current.accentDeep
val CosmicPurple: Color get() = ThemeState.current.accentPurple
val CosmicMagenta: Color get() = ThemeState.current.magenta
val CosmicGold: Color get() = ThemeState.current.gold
val CosmicWhite: Color get() = ThemeState.current.textPrimary
val CosmicMutedText: Color get() = ThemeState.current.textMuted
val SignalRed: Color get() = ThemeState.current.signalRed

val LiquidGlassBrush: Brush get() = ThemeState.current.liquidGlassBrush
val LiquidBorderBrush: Brush get() = ThemeState.current.liquidBorderBrush
val LiquidActiveButtonBrush: Brush get() = ThemeState.current.liquidActiveButtonBrush
val LiquidRecordButtonBrush: Brush get() = ThemeState.current.liquidRecordButtonBrush

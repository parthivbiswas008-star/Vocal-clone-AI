package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Liquid Glass Cosmic Palette
val DeepSpaceBackground = Color(0xFF080B14)
val DeepSpaceLuminous = Color(0xFF0F172A)

// Frosted Liquid Glass Surfaces
val LiquidGlassSurface = Color(0x221E293B)
val LiquidGlassSurfaceElevated = Color(0x35334155)
val LiquidGlassSurfaceLight = Color(0x18FFFFFF)
val LiquidGlassBorder = Color(0x3038BDF8)
val LiquidGlassBorderGlow = Color(0x8000F5D4)

// Legacy card compatibility
val SpaceCard = Color(0xFF13182E)
val SpaceCardElevated = Color(0xFF1E2442)

// Vibrant Liquid Cosmic Accents
val CosmicCyan = Color(0xFF00F5D4)
val CosmicCyanGlow = Color(0xFF70FFF0)
val CosmicViolet = Color(0xFF9D4EDD)
val CosmicIndigo = Color(0xFF5A189A)
val CosmicPurple = Color(0xFF7928CA)
val CosmicMagenta = Color(0xFFFF206E)
val CosmicGold = Color(0xFFFFBE0B)
val CosmicWhite = Color(0xFFF8FAFC)
val CosmicMutedText = Color(0xFF94A3B8)
val SignalRed = Color(0xFFFF3366)

// Liquid Glass Gradient Brushes
val LiquidGlassBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x3038BDF8),
        Color(0x159D4EDD),
        Color(0x250F172A)
    )
)

val LiquidBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x9000F5D4),
        Color(0x509D4EDD),
        Color(0x20FFFFFF),
        Color(0x1000F5D4)
    )
)

val LiquidActiveButtonBrush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF00F5D4),
        Color(0xFF7928CA)
    )
)

val LiquidRecordButtonBrush = Brush.radialGradient(
    colors = listOf(
        Color(0xFFFF3366),
        Color(0xFF99002E)
    )
)

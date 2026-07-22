package com.sacompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── SA Futuristic Color Palette ───────────────────────────────────────────────
val CyberBlue = Color(0xFF00D4FF)
val CyberBlueDeep = Color(0xFF0090C0)
val NeonGreen = Color(0xFF00FF88)
val NeonGreenDim = Color(0xFF00AA55)
val PlasmaViolet = Color(0xFF9B59FF)
val PlasmaVioletDim = Color(0xFF6B3DB0)
val HUDOrange = Color(0xFFFF6B35)
val DarkBase = Color(0xFF030812)
val DarkSurface = Color(0xFF0A1628)
val DarkCard = Color(0xFF0F1F3D)
val GlassWhite = Color(0x1AFFFFFF)
val GlassBorder = Color(0x33FFFFFF)
val TextPrimary = Color(0xFFE0F4FF)
val TextSecondary = Color(0xFF7FC3E8)

private val SADarkColorScheme = darkColorScheme(
    primary = CyberBlue,
    onPrimary = DarkBase,
    primaryContainer = DarkCard,
    onPrimaryContainer = CyberBlue,
    secondary = NeonGreen,
    onSecondary = DarkBase,
    secondaryContainer = Color(0xFF0D2A1A),
    onSecondaryContainer = NeonGreen,
    tertiary = PlasmaViolet,
    onTertiary = DarkBase,
    background = DarkBase,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = Color(0xFFFF4444),
    onError = Color.White
)

@Composable
fun SACompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SADarkColorScheme,
        typography = SATypography,
        content = content
    )
}

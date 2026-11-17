package dev.currency.converter.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Neutral palette — edit these values to re-skin the whole app.
//
// The converter is a dark-first design: a near-black canvas, charcoal cards and
// silver accents. A neutral light scheme is kept so previews and the optional
// light mode never fall back to tinted Material defaults.
// ---------------------------------------------------------------------------
val Ink = Color(0xFF0B0B0C)
val Charcoal = Color(0xFF17171A)
val Graphite = Color(0xFF222226)
val Hairline = Color(0xFF26262B)
val OffWhite = Color(0xFFF4F4F6)
val MutedGrey = Color(0xFF9CA3AF)
val Silver = Color(0xFFE4E4E7)

val DarkColors = darkColorScheme(
    primary = Silver,
    onPrimary = Ink,
    primaryContainer = Graphite,
    onPrimaryContainer = OffWhite,
    inversePrimary = Color(0xFF3F3F46),
    secondary = Color(0xFFD4D4D8),
    onSecondary = Ink,
    secondaryContainer = Graphite,
    onSecondaryContainer = Silver,
    tertiary = Color(0xFFA1A1AA),
    onTertiary = Ink,
    tertiaryContainer = Graphite,
    onTertiaryContainer = OffWhite,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF2A0A0A),
    errorContainer = Color(0xFF3F1515),
    onErrorContainer = Color(0xFFFECACA),
    background = Ink,
    onBackground = OffWhite,
    surface = Ink,
    onSurface = OffWhite,
    surfaceVariant = Color(0xFF1D1D21),
    onSurfaceVariant = MutedGrey,
    surfaceTint = Silver,
    inverseSurface = OffWhite,
    inverseOnSurface = Charcoal,
    outline = Hairline,
    outlineVariant = Hairline,
    scrim = Color(0xFF000000),
    // Tonal surfaces: cards use Low, pills and operator buttons use Highest.
    surfaceBright = Color(0xFF2A2A2F),
    surfaceDim = Ink,
    surfaceContainerLowest = Color(0xFF08080A),
    surfaceContainerLow = Charcoal,
    surfaceContainer = Color(0xFF1B1B1F),
    surfaceContainerHigh = Color(0xFF202024),
    surfaceContainerHighest = Graphite,
)

val LightColors = lightColorScheme(
    primary = Color(0xFF27272A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Silver,
    onPrimaryContainer = Color(0xFF18181B),
    inversePrimary = Color(0xFFA1A1AA),
    secondary = Color(0xFF52525B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1F1F3),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = Color(0xFF71717A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9E9EC),
    onTertiaryContainer = Color(0xFF18181B),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF111113),
    surface = Color(0xFFF7F7F8),
    onSurface = Color(0xFF111113),
    surfaceVariant = Color(0xFFEDEDEF),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceTint = Color(0xFF27272A),
    inverseSurface = Color(0xFF27272A),
    inverseOnSurface = Color(0xFFF4F4F6),
    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFE4E4E7),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE9E9EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFAFAFB),
    surfaceContainerHigh = Color(0xFFF4F4F6),
    surfaceContainerHighest = Color(0xFFF1F1F3),
)

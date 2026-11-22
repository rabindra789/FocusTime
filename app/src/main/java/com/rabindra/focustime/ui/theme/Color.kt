package com.rabindra.focustime.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Warm, low-contrast surfaces with a single muted sand accent.
 * Tokens are named by role so screens never hold literal colours.
 */
@Immutable
data class FocusPalette(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val alert: Color,
    val divider: Color,
    val track: Color,
    val onAccent: Color,
    /** Muted natural greens for the Forest face; never used for chrome. */
    val foliage: Color,
    val foliageMuted: Color,
    val isDark: Boolean,
)

val DarkPalette = FocusPalette(
    background = Color(0xFF171815),
    surface = Color(0xFF20211D),
    surfaceRaised = Color(0xFF26271F),
    textPrimary = Color(0xFFE8E6DC),
    textSecondary = Color(0xFF99998F),
    textMuted = Color(0xFF6E6E64),
    accent = Color(0xFFB7A18A),
    alert = Color(0xFFA66F69),
    divider = Color(0xFF33342F),
    track = Color(0xFF2A2B26),
    onAccent = Color(0xFF171815),
    foliage = Color(0xFF7D8A68),
    foliageMuted = Color(0xFF4B5340),
    isDark = true,
)

/**
 * Pure black surfaces for AMOLED panels. Black pixels are switched off on OLED, so the timer
 * costs less power; LCD screens still light their backlight and save nothing.
 */
val LowPowerPalette = DarkPalette.copy(
    background = Color.Black,
    surface = Color(0xFF070706),
    surfaceRaised = Color(0xFF0C0C0B),
    divider = Color(0xFF242420),
    track = Color(0xFF1A1A17),
)

val LightPalette = FocusPalette(
    background = Color(0xFFF2F0E8),
    surface = Color(0xFFEAE7DD),
    surfaceRaised = Color(0xFFE1DED3),
    textPrimary = Color(0xFF1E1F1B),
    textSecondary = Color(0xFF63635A),
    textMuted = Color(0xFF8A8A80),
    accent = Color(0xFF82694F),
    alert = Color(0xFF8F5B55),
    divider = Color(0xFFD5D2C6),
    track = Color(0xFFDDD9CD),
    onAccent = Color(0xFFF7F5EE),
    foliage = Color(0xFF6C7A58),
    foliageMuted = Color(0xFF9DAA8C),
    isDark = false,
)

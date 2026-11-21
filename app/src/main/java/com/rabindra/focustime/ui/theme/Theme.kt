package com.rabindra.focustime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalFocusPalette = staticCompositionLocalOf { DarkPalette }

private val FocusTypography = Typography(
    displayLarge = FocusType.timer,
    displayMedium = FocusType.timerCompact,
    headlineMedium = FocusType.display,
    titleLarge = FocusType.title,
    bodyLarge = FocusType.body,
    bodyMedium = FocusType.label,
    labelLarge = FocusType.action,
    labelMedium = FocusType.caption,
)

@Composable
fun FocusTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.textSecondary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceRaised,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.divider,
            error = palette.alert,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.textSecondary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceRaised,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.divider,
            error = palette.alert,
        )
    }
    CompositionLocalProvider(LocalFocusPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = FocusTypography, content = content)
    }
}

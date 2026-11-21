package com.rabindra.focustime.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.data.AppSettings
import com.rabindra.focustime.ui.components.Hairline
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.components.SettingsRow
import com.rabindra.focustime.ui.components.StepperRow
import com.rabindra.focustime.ui.components.ToggleRow
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import androidx.compose.material3.Text

@Composable
fun SettingsScreen(
    settings: AppSettings,
    dndAccess: Boolean,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetStatistics: () -> Unit,
    onRefreshDndAccess: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current

    LaunchedEffect(Unit) { onRefreshDndAccess() }

    Column(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Settings", style = FocusType.title, color = palette.textPrimary)
            QuietAction(
                label = "Done",
                onClick = onBack,
                color = palette.accent,
                horizontalPadding = 0.dp,
            )
        }

        Spacer(Modifier.height(FocusSpacing.lg))

        SectionLabel("Durations")

        StepperRow(
            label = "Focus duration",
            value = "${settings.focusMinutes} min",
            decreaseLabel = "Decrease focus duration",
            increaseLabel = "Increase focus duration",
            onDecrease = {
                onUpdate { it.copy(focusMinutes = (it.focusMinutes - 1).coerceAtLeast(1)) }
            },
            onIncrease = {
                onUpdate { it.copy(focusMinutes = (it.focusMinutes + 1).coerceAtMost(180)) }
            },
            canDecrease = settings.focusMinutes > 1,
            canIncrease = settings.focusMinutes < 180,
        )
        StepperRow(
            label = "Short break",
            value = "${settings.shortBreakMinutes} min",
            decreaseLabel = "Decrease short break",
            increaseLabel = "Increase short break",
            onDecrease = {
                onUpdate { it.copy(shortBreakMinutes = (it.shortBreakMinutes - 1).coerceAtLeast(1)) }
            },
            onIncrease = {
                onUpdate { it.copy(shortBreakMinutes = (it.shortBreakMinutes + 1).coerceAtMost(60)) }
            },
            canDecrease = settings.shortBreakMinutes > 1,
            canIncrease = settings.shortBreakMinutes < 60,
        )
        StepperRow(
            label = "Long break",
            value = "${settings.longBreakMinutes} min",
            decreaseLabel = "Decrease long break",
            increaseLabel = "Increase long break",
            onDecrease = {
                onUpdate { it.copy(longBreakMinutes = (it.longBreakMinutes - 1).coerceAtLeast(1)) }
            },
            onIncrease = {
                onUpdate { it.copy(longBreakMinutes = (it.longBreakMinutes + 1).coerceAtMost(120)) }
            },
            canDecrease = settings.longBreakMinutes > 1,
            canIncrease = settings.longBreakMinutes < 120,
        )

        Spacer(Modifier.height(FocusSpacing.lg))
        SectionLabel("Flow")

        ToggleRow(
            label = "Start breaks automatically",
            description = "Move into a break when focus ends.",
            checked = settings.autoStartBreaks,
            onCheckedChange = { on -> onUpdate { it.copy(autoStartBreaks = on) } },
        )
        ToggleRow(
            label = "Start focus automatically",
            description = "Return to focus when a break ends.",
            checked = settings.autoStartFocus,
            onCheckedChange = { on -> onUpdate { it.copy(autoStartFocus = on) } },
        )

        Spacer(Modifier.height(FocusSpacing.lg))
        SectionLabel("Silence")

        ToggleRow(
            label = "Automatic Do Not Disturb",
            description = "Quiet notifications while focusing.",
            checked = settings.autoDnd,
            onCheckedChange = { on -> onUpdate { it.copy(autoDnd = on) } },
        )
        SettingsRow(
            label = "Do Not Disturb access",
            value = if (dndAccess) "ALLOWED" else "GRANT",
            onClick = onOpenDndSettings,
        )

        Spacer(Modifier.height(FocusSpacing.lg))
        SectionLabel("Display")

        ToggleRow(
            label = "24-hour clock",
            description = "Use 13:00 instead of 1:00 PM.",
            checked = settings.use24Hour,
            onCheckedChange = { on -> onUpdate { it.copy(use24Hour = on) } },
        )
        ToggleRow(
            label = "Keep screen awake",
            description = "Hold the display on during focus.",
            checked = settings.keepScreenOn,
            onCheckedChange = { on -> onUpdate { it.copy(keepScreenOn = on) } },
        )
        ToggleRow(
            label = "Low power mode",
            description = "True black background. Saves power on AMOLED screens only; LCD backlights stay on.",
            checked = settings.lowPowerMode,
            onCheckedChange = { on -> onUpdate { it.copy(lowPowerMode = on) } },
        )
        SettingsRow(
            label = "Landscape watch face",
            value = settings.watchFace.label,
            onClick = { onUpdate { it.copy(watchFace = it.watchFace.next()) } },
        )
        Text(
            text = "Swipe sideways in landscape to change, or tap the row.",
            style = FocusType.caption,
            color = palette.textMuted,
        )

        Spacer(Modifier.height(FocusSpacing.lg))
        SectionLabel("Feedback")

        ToggleRow(
            label = "Sound",
            description = "A soft chime when a session changes.",
            checked = settings.soundEnabled,
            onCheckedChange = { on -> onUpdate { it.copy(soundEnabled = on) } },
        )
        ToggleRow(
            label = "Vibration",
            description = "A quiet buzz when a session changes.",
            checked = settings.vibrateEnabled,
            onCheckedChange = { on -> onUpdate { it.copy(vibrateEnabled = on) } },
        )

        Spacer(Modifier.height(FocusSpacing.lg))
        SectionLabel("About")

        SettingsRow(
            label = "Replay tutorial",
            onClick = { onUpdate { it.copy(tutorialCompleted = false) } },
        )
        SettingsRow(
            label = "Reset statistics",
            onClick = onResetStatistics,
        )
        SettingsRow(
            label = "FocusTime",
            value = "A quiet desk clock",
        )

        Spacer(Modifier.height(FocusSpacing.huge))
        Hairline()
    }
}

/** Small uppercase section marker; quieter than a heading. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = FocusType.caption,
        color = LocalFocusPalette.current.textMuted,
    )
}

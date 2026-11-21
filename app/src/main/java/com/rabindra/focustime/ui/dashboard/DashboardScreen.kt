package com.rabindra.focustime.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.data.AppSettings
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.components.ThinProgress
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.TimeFormatter

/**
 * Quiet portrait timer.
 *
 * Identity is small, the countdown is the only loud element, and everything else
 * stays out of the way. Secondary controls rest behind an upward swipe; a long
 * press asks before resetting.
 */
@Composable
fun DashboardScreen(
    state: TimerState,
    remainingMillis: Long,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onSettings: () -> Unit,
    onStatistics: () -> Unit = {},
    keepScreenOn: Boolean = false,
    showTutorial: Boolean = false,
    onTutorialDone: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current

    // Hold the display on only while this quiet screen is visible.
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    if (showTutorial) {
        TutorialOverlay(onDone = onTutorialDone)
        return
    }

    var controlsVisible by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var dragAccumulator by remember { mutableStateOf(0f) }

    Column(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccumulator < -40f) controlsVisible = true
                        if (dragAccumulator > 40f) controlsVisible = false
                        dragAccumulator = 0f
                    },
                    onDragCancel = { dragAccumulator = 0f },
                )
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(FocusSpacing.huge))

        Text(
            text = "FocusTime",
            style = FocusType.caption,
            color = palette.textSecondary,
        )

        Spacer(Modifier.height(FocusSpacing.lg))

        Text(
            text = TimeFormatter.countdown(remainingMillis),
            style = FocusType.timer,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .testTag("timer")
                .semantics { contentDescription = "Timer" }
                .pointerInput(state.status, remainingMillis) {
                    detectTapGestures(
                        onTap = { onStartPause() },
                        onLongPress = { confirmReset = true },
                    )
                },
        )

        Spacer(Modifier.height(FocusSpacing.md))

        Text(
            text = contextLabel(state),
            style = FocusType.caption,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(FocusSpacing.xl))

        ThinProgress(
            progress = state.progressAt(remainingMillis),
            color = palette.accent,
            modifier = Modifier
                .width(180.dp)
                .height(2.dp),
        )

        Spacer(Modifier.height(FocusSpacing.xl))

        Text(
            text = TimeFormatter.sessionLabel(state.sessionNumber),
            style = FocusType.caption,
            color = palette.textMuted,
        )

        Spacer(Modifier.height(FocusSpacing.xl))

        if (controlsVisible) {
            SecondaryControls(
                state = state,
                onStartPause = onStartPause,
                onSkip = onSkip,
                onReset = { confirmReset = true },
                onSettings = onSettings,
                onStatistics = onStatistics,
            )
        } else {
            QuietAction(
                label = "More",
                onClick = { controlsVisible = true },
            )
        }

        Spacer(Modifier.height(FocusSpacing.huge))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset session?", color = palette.textPrimary) },
            text = {
                Text(
                    "The current countdown will start over.",
                    color = palette.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) {
                    Text("Reset", color = palette.alert)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text("Keep going", color = palette.textSecondary)
                }
            },
            containerColor = palette.surface,
        )
    }
}

@Composable
private fun SecondaryControls(
    state: TimerState,
    onStartPause: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
    onStatistics: () -> Unit,
) {
    val palette = LocalFocusPalette.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietAction(
                label = if (state.status == TimerStatus.RUNNING) "Pause" else "Start",
                onClick = onStartPause,
                color = palette.accent,
            )
            Spacer(Modifier.width(FocusSpacing.sm))
            QuietAction(label = "Skip", onClick = onSkip)
            Spacer(Modifier.width(FocusSpacing.sm))
            QuietAction(label = "Reset", onClick = onReset)
        }
        Spacer(Modifier.height(FocusSpacing.xs))
        Row(horizontalArrangement = Arrangement.Center) {
            QuietAction(label = "Settings", onClick = onSettings)
            Spacer(Modifier.width(FocusSpacing.sm))
            QuietAction(label = "Stats", onClick = onStatistics)
        }
    }
}

/** Tiny contextual label: what you are doing, and whether it is moving. */
private fun contextLabel(state: TimerState): String {
    val name = when (state.sessionType) {
        SessionType.FOCUS -> "Focus"
        SessionType.SHORT_BREAK -> "Short break"
        SessionType.LONG_BREAK -> "Long break"
    }
    val suffix = when (state.status) {
        TimerStatus.RUNNING -> "in progress"
        TimerStatus.PAUSED -> "paused"
        TimerStatus.IDLE -> "ready"
    }
    return "$name · $suffix"
}

/** Three quiet lines, then the timer. Never shown again once dismissed. */
@Composable
fun TutorialOverlay(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current

    Box(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(FocusSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg),
        ) {
            Text(text = "Tap to begin.", style = FocusType.title, color = palette.textPrimary)
            Text(
                text = "Turn your phone sideways.",
                style = FocusType.title,
                color = palette.textPrimary,
            )
            Text(
                text = "Stay focused. We'll handle interruptions.",
                style = FocusType.body,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(FocusSpacing.sm))
            QuietAction(
                label = "Get started",
                onClick = onDone,
                color = palette.accent,
                horizontalPadding = 0.dp,
            )
        }
    }
}

package com.rabindra.focustime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.theme.FocusRadius
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.TimeFormatter

/** "Focus", "Short break" or "Long break". */
fun sessionName(state: TimerState): String = when (state.sessionType) {
    SessionType.FOCUS -> "Focus"
    SessionType.SHORT_BREAK -> "Short break"
    SessionType.LONG_BREAK -> "Long break"
}

/** "Focus · in progress", used wherever the clock shows its quiet context line. */
fun sessionDetail(state: TimerState): String {
    val suffix = when (state.status) {
        TimerStatus.RUNNING -> "in progress"
        TimerStatus.PAUSED -> "paused"
        TimerStatus.IDLE -> "ready"
    }
    return "${sessionName(state)} · $suffix"
}

/**
 * Long-press sheet: the few actions that are not worth a permanent button. Nothing destructive
 * happens here; skip and reset only ask the caller to show its confirmation dialog.
 */
@Composable
fun SessionQuickActions(
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxSize()
            .background(palette.background.copy(alpha = 0.96f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onDismiss,
            )
            .testTag("quick-actions"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(FocusSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
        ) {
            Text(
                text = "Session actions",
                style = FocusType.caption,
                color = palette.textMuted,
            )
            Spacer(Modifier.height(FocusSpacing.sm))
            Row(horizontalArrangement = Arrangement.Center) {
                QuietAction(label = "Skip", onClick = onSkip)
                Spacer(Modifier.width(FocusSpacing.sm))
                QuietAction(label = "Reset", onClick = onReset, color = palette.alert)
            }
            Spacer(Modifier.height(FocusSpacing.xs))
            QuietAction(label = "Close", onClick = onDismiss, color = palette.textSecondary)
        }
    }
}

/** Compact read-out revealed by a downward swipe. Never holds controls. */
@Composable
fun SessionInfoPanel(
    state: TimerState,
    remainingMillis: Long,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    Column(
        modifier
            .testTag("session-panel")
            .background(palette.surface, RoundedCornerShape(FocusRadius.md))
            .padding(horizontal = FocusSpacing.xl, vertical = FocusSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = TimeFormatter.countdown(remainingMillis),
            style = FocusType.timerCompact,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(FocusSpacing.xs))
        Text(
            text = sessionDetail(state),
            style = FocusType.caption,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(FocusSpacing.sm))
        Text(
            text = TimeFormatter.sessionLabel(state.sessionNumber),
            style = FocusType.caption,
            color = palette.textMuted,
        )
        Spacer(Modifier.height(FocusSpacing.md))
        ThinProgress(
            progress = state.progressAt(remainingMillis),
            color = palette.accent,
            modifier = Modifier
                .fillMaxWidth()
                .width(180.dp),
        )
    }
}

@Composable
fun ResetSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset session?", color = palette.textPrimary) },
        text = {
            Text("The current countdown will start over.", color = palette.textSecondary)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm-reset"),
            ) {
                Text("Reset", color = palette.alert)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep going", color = palette.textSecondary)
            }
        },
        containerColor = palette.surface,
    )
}

@Composable
fun SkipSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skip session?", color = palette.textPrimary) },
        text = {
            Text(
                "Move on to the next session without finishing this one.",
                color = palette.textSecondary,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm-skip"),
            ) {
                Text("Skip", color = palette.alert)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep going", color = palette.textSecondary)
            }
        },
        containerColor = palette.surface,
    )
}

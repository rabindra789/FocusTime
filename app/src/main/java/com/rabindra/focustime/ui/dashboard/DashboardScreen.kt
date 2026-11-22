package com.rabindra.focustime.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.components.ResetSessionDialog
import com.rabindra.focustime.ui.components.SessionInfoPanel
import com.rabindra.focustime.ui.components.SessionQuickActions
import com.rabindra.focustime.ui.components.SkipSessionDialog
import com.rabindra.focustime.ui.components.ThinProgress
import com.rabindra.focustime.ui.components.sessionDetail
import com.rabindra.focustime.ui.gesture.SwipeDirection
import com.rabindra.focustime.ui.gesture.clockGestures
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.TimeFormatter
import com.rabindra.focustime.watchface.WatchFace
import kotlinx.coroutines.delay

/**
 * Quiet portrait timer.
 *
 * Identity is small and the countdown is the only loud element. The same gestures as the
 * landscape clock apply: tap reveals or hides the controls, double tap pauses or resumes,
 * a long press opens quick actions, swiping up asks before skipping and swiping down shows the
 * session panel. Sideways swipes choose the landscape face that comes next.
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
    modifier: Modifier = Modifier,
    watchFace: WatchFace = WatchFace.DESK_CLOCK,
    onWatchFaceChange: (WatchFace) -> Unit = {},
) {
    val palette = LocalFocusPalette.current

    // Hold the display on only while this quiet screen is visible.
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var controlsVisible by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmSkip by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var faceHint by remember { mutableStateOf<WatchFace?>(null) }

    // Controls and the glance panel withdraw on their own.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(showInfo) {
        if (showInfo) {
            delay(6_000)
            showInfo = false
        }
    }
    LaunchedEffect(faceHint) {
        if (faceHint != null) {
            delay(2_200)
            faceHint = null
        }
    }

    fun changeFace(next: WatchFace) {
        if (next == watchFace) return
        faceHint = next
        onWatchFaceChange(next)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clockGestures(
                    onTap = {
                        showInfo = false
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = {
                        showInfo = false
                        controlsVisible = false
                        onStartPause()
                    },
                    onLongPress = {
                        controlsVisible = false
                        showInfo = false
                        showActions = true
                    },
                    onSwipe = { direction ->
                        when (direction) {
                            SwipeDirection.LEFT -> changeFace(watchFace.next())
                            SwipeDirection.RIGHT -> changeFace(watchFace.previous())
                            SwipeDirection.UP -> {
                                controlsVisible = false
                                showInfo = false
                                confirmSkip = true
                            }
                            SwipeDirection.DOWN -> {
                                controlsVisible = false
                                showInfo = true
                            }
                        }
                    },
                )
                .semantics {
                    contentDescription = "Timer"
                    onClick(label = "Show or hide controls") {
                        controlsVisible = !controlsVisible
                        true
                    }
                    customActions = listOf(
                        CustomAccessibilityAction(
                            if (state.status == TimerStatus.RUNNING) "Pause session"
                            else "Resume session",
                        ) {
                            onStartPause()
                            true
                        },
                        CustomAccessibilityAction("Skip session") {
                            confirmSkip = true
                            true
                        },
                        CustomAccessibilityAction("Reset session") {
                            confirmReset = true
                            true
                        },
                    )
                }
                .testTag("clock-gestures"),
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
                    .semantics { contentDescription = "Countdown" },
            )

            Spacer(Modifier.height(FocusSpacing.md))

            Text(
                text = sessionDetail(state),
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
                    onSkip = { confirmSkip = true },
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

        faceHint?.let { hint ->
            Text(
                text = hint.label,
                style = FocusType.caption,
                color = palette.textSecondary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = FocusSpacing.huge)
                    .semantics { contentDescription = "Watch face ${hint.label}" },
            )
        }

        if (showInfo) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SessionInfoPanel(state = state, remainingMillis = remainingMillis)
            }
        }

        if (showActions) {
            SessionQuickActions(
                onSkip = {
                    showActions = false
                    confirmSkip = true
                },
                onReset = {
                    showActions = false
                    confirmReset = true
                },
                onDismiss = { showActions = false },
            )
        }

        if (confirmReset) {
            ResetSessionDialog(
                onConfirm = {
                    confirmReset = false
                    onReset()
                },
                onDismiss = { confirmReset = false },
            )
        }

        if (confirmSkip) {
            SkipSessionDialog(
                onConfirm = {
                    confirmSkip = false
                    onSkip()
                },
                onDismiss = { confirmSkip = false },
            )
        }
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

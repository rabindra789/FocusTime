package com.rabindra.focustime.ui.landscape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.components.ResetSessionDialog
import com.rabindra.focustime.ui.components.SessionInfoPanel
import com.rabindra.focustime.ui.components.SessionQuickActions
import com.rabindra.focustime.ui.components.SkipSessionDialog
import com.rabindra.focustime.ui.components.ThinProgress
import com.rabindra.focustime.ui.components.sessionName
import com.rabindra.focustime.ui.gesture.clockGestures
import com.rabindra.focustime.ui.gesture.SwipeDirection
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.ui.watchface.AnalogFace
import com.rabindra.focustime.ui.watchface.CircularPomodoroFace
import com.rabindra.focustime.ui.watchface.ForestFace
import com.rabindra.focustime.ui.watchface.GrowingPlantFace
import com.rabindra.focustime.ui.watchface.MorseCodeFace
import com.rabindra.focustime.ui.watchface.rememberReducedMotion
import com.rabindra.focustime.util.TimeFormatter
import com.rabindra.focustime.watchface.WatchFace
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * Landscape desk clock.
 *
 * Every face shares one gesture surface:
 * tap shows or hides the controls, double tap pauses or resumes, a sideways swipe changes face,
 * a long press opens quick actions, swiping up asks before skipping and swiping down shows the
 * compact session panel. Controls fade after five quiet seconds.
 */
@Composable
fun LandscapeClockScreen(
    state: TimerState,
    remainingMillis: Long,
    use24Hour: Boolean,
    onStartPause: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    watchFace: WatchFace = WatchFace.DESK_CLOCK,
    onWatchFaceChange: (WatchFace) -> Unit = {},
) {
    val palette = LocalFocusPalette.current
    val reducedMotion = rememberReducedMotion()
    var controlsVisible by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmSkip by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var faceHint by remember { mutableStateOf<WatchFace?>(null) }
    var now by remember { mutableStateOf(LocalTime.now()) }

    // Clock ticks once a second, nothing more.
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000)
        }
    }

    // Controls disappear after five seconds of inactivity.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5_000)
            controlsVisible = false
        }
    }

    // The session panel is a glance, not a screen; it folds away on its own.
    LaunchedEffect(showInfo) {
        if (showInfo) {
            delay(6_000)
            showInfo = false
        }
    }

    // The face name rests on screen just long enough to confirm a swipe.
    LaunchedEffect(faceHint) {
        if (faceHint != null) {
            delay(2_200)
            faceHint = null
        }
    }

    // Keep the screen awake only here.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val clock = TimeFormatter.clock(now, use24Hour)

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
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                        contentDescription = "Clock"
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
            ) {
                when (watchFace) {
                    WatchFace.DESK_CLOCK -> DeskClockFace(state, remainingMillis, clock)
                    WatchFace.ANALOG -> AnalogFace(
                        state = state,
                        remainingMillis = remainingMillis,
                        clock = clock,
                        time = now,
                        reducedMotion = reducedMotion,
                    )

                    WatchFace.CIRCULAR_POMODORO -> CircularPomodoroFace(
                        state = state,
                        remainingMillis = remainingMillis,
                        clock = clock,
                        reducedMotion = reducedMotion,
                    )

                    WatchFace.GROWING_PLANT -> GrowingPlantFace(
                        state = state,
                        remainingMillis = remainingMillis,
                        clock = clock,
                        reducedMotion = reducedMotion,
                    )

                    WatchFace.MORSE_CODE -> MorseCodeFace(
                        state = state,
                        remainingMillis = remainingMillis,
                        clock = clock,
                        time = now,
                        use24Hour = use24Hour,
                    )

                    WatchFace.FOREST -> ForestFace(
                        state = state,
                        remainingMillis = remainingMillis,
                        clock = clock,
                        reducedMotion = reducedMotion,
                    )
                }

                faceHint?.let { hint ->
                    Text(
                        text = hint.label,
                        style = FocusType.caption,
                        color = palette.textSecondary,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = FocusSpacing.md)
                            .semantics { contentDescription = "Watch face ${hint.label}" },
                    )
                }

                if (showInfo) {
                    SessionInfoPanel(
                        state = state,
                        remainingMillis = remainingMillis,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            if (controlsVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Clear of the system's bottom gesture strip in landscape.
                        .padding(bottom = FocusSpacing.lg, top = FocusSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuietAction(
                            label = if (state.status == TimerStatus.RUNNING) "Pause" else "Resume",
                            onClick = onStartPause,
                            color = palette.accent,
                        )
                        Spacer(Modifier.width(FocusSpacing.sm))
                        QuietAction(label = "Skip", onClick = { confirmSkip = true })
                        Spacer(Modifier.width(FocusSpacing.sm))
                        QuietAction(
                            label = "Reset",
                            onClick = {
                                controlsVisible = false
                                confirmReset = true
                            },
                        )
                        Spacer(Modifier.width(FocusSpacing.sm))
                        QuietAction(
                            label = "Face",
                            onClick = { changeFace(watchFace.next()) },
                        )
                    }
                }
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

/** The original split clock: wall clock leads, session countdown sits beside it. */
@Composable
private fun DeskClockFace(
    state: TimerState,
    remainingMillis: Long,
    clock: com.rabindra.focustime.util.ClockText,
) {
    val palette = LocalFocusPalette.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FocusSpacing.huge, vertical = FocusSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = clock.time,
                    style = FocusType.clock,
                    color = palette.textPrimary,
                )
                clock.meridiem?.let {
                    Spacer(Modifier.width(FocusSpacing.xs))
                    Text(
                        text = it,
                        style = FocusType.caption,
                        color = palette.textMuted,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(120.dp)
                .background(palette.divider),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = TimeFormatter.countdown(remainingMillis),
                style = FocusType.clockCountdown,
                color = palette.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FocusSpacing.sm))
            Text(
                text = "${sessionName(state)} · ${state.sessionNumber}",
                style = FocusType.caption,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(FocusSpacing.md))
            ThinProgress(
                progress = state.progressAt(remainingMillis),
                color = palette.accent,
                modifier = Modifier
                    .width(160.dp)
                    .height(2.dp),
            )
        }
    }
}

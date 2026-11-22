package com.rabindra.focustime.ui.landscape

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.components.ThinProgress
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
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
 * Five faces share the same quiet frame: tap for controls, swipe sideways to change face.
 * Controls fade after five quiet seconds and the display is kept awake only on this screen.
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

    // The face and the controls get separate strips, so a button tap can never be mistaken
    // for a tap on the clock (or the other way round).
    Column(
        modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(watchFace) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragged > 60f) changeFace(watchFace.previous())
                            if (dragged < -60f) changeFace(watchFace.next())
                            dragged = 0f
                        },
                        onDragCancel = { dragged = 0f },
                        onHorizontalDrag = { _, amount -> dragged += amount },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                },
        ) {
        when (watchFace) {
            WatchFace.DESK_CLOCK -> DeskClockFace(state, remainingMillis, clock)
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
                    QuietAction(label = "Skip", onClick = onSkip)
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
                text = sessionLabel(state),
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

private fun sessionLabel(state: TimerState): String {
    val name = when (state.sessionType) {
        SessionType.FOCUS -> "Focus"
        SessionType.SHORT_BREAK -> "Short break"
        SessionType.LONG_BREAK -> "Long break"
    }
    return "$name · ${state.sessionNumber}"
}

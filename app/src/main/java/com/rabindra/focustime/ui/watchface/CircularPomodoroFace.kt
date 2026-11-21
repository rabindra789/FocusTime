package com.rabindra.focustime.ui.watchface

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.ClockText
import com.rabindra.focustime.util.TimeFormatter

/**
 * The clock owns the screen; the Pomodoro is a small ring in the corner.
 *
 * The ring is the remaining share of the session, so it empties smoothly while a focus or a
 * break runs. Focus uses the sand accent; breaks step back to a neutral grey.
 */
@Composable
fun CircularPomodoroFace(
    state: TimerState,
    remainingMillis: Long,
    clock: ClockText,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    val isFocus = state.sessionType.isFocus
    val duration = state.durationMillis.coerceAtLeast(1L)
    val target = (remainingMillis.toFloat() / duration).coerceIn(0f, 1f)
    val remainingShare by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reducedMotion) snap() else tween(1_000, easing = LinearEasing),
        label = "pomodoro ring",
    )
    val ringColor = if (isFocus) palette.accent else palette.textSecondary

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FocusSpacing.huge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = clock.time,
                    style = FocusType.clock,
                    color = palette.textPrimary,
                    modifier = Modifier.semantics {
                        contentDescription = "Current time ${clock.time} ${clock.meridiem.orEmpty()}"
                    },
                )
                clock.meridiem?.let {
                    Spacer(Modifier.width(FocusSpacing.xs))
                    Text(text = it, style = FocusType.caption, color = palette.textMuted)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = FocusSpacing.huge, vertical = FocusSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .size(58.dp)
                        .semantics {
                            contentDescription =
                                "${if (isFocus) "Focus" else "Break"} progress, " +
                                "${(remainingShare * 100).toInt()} percent remaining"
                        },
                ) {
                    val stroke = 2.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = palette.track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    // Negative sweep empties the ring clockwise as the session runs down.
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = -360f * remainingShare,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = if (isFocus) "focus" else "break",
                    style = FocusType.caption,
                    color = palette.textMuted,
                )
            }
            Spacer(Modifier.height(FocusSpacing.sm))
            Text(
                text = sessionLine(state, remainingMillis),
                style = FocusType.caption,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun sessionLine(state: TimerState, remainingMillis: Long): String {
    val name = when (state.sessionType) {
        SessionType.FOCUS -> "Focus"
        SessionType.SHORT_BREAK -> "Short break"
        SessionType.LONG_BREAK -> "Long break"
    }
    val suffix = when (state.status) {
        TimerStatus.RUNNING -> ""
        TimerStatus.PAUSED -> " · paused"
        TimerStatus.IDLE -> " · ready"
    }
    return "$name$suffix · ${TimeFormatter.countdown(remainingMillis)}"
}

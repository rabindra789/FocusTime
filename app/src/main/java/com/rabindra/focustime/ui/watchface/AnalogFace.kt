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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.components.sessionName
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.ClockText
import com.rabindra.focustime.util.TimeFormatter
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Angles for the three hands, measured clockwise from twelve o'clock.
 *
 * Hour and minute hands carry their fractions so a minute hand creeps forward with the second
 * hand instead of jumping once a minute. Pure maths, so it can be checked without a clock.
 */
data class AnalogHandAngles(val hour: Float, val minute: Float, val second: Float)

fun analogHandAngles(time: LocalTime): AnalogHandAngles {
    val seconds = time.second.toFloat()
    val minutes = time.minute + seconds / 60f
    val hours = (time.hour % 12) + minutes / 60f
    return AnalogHandAngles(
        hour = hours * 30f,
        minute = minutes * 6f,
        second = seconds * 6f,
    )
}

/**
 * A full-screen analog desk clock.
 *
 * The dial is the hero: warm quartz surface, twelve restrained markers, three quiet hands and a
 * small digital time beneath it. The session lives in the top corner and a hairline ring outside
 * the dial, both drawn from [TimerState]'s own progress so there is no second timer anywhere.
 *
 * The second hand glides between the once-a-second clock ticks; with reduced motion it steps
 * instead. Progress is only ever [TimerState.progressAt] for the remaining time the screen
 * already publishes — the face never advances the timer itself.
 */
@Composable
fun AnalogFace(
    state: TimerState,
    remainingMillis: Long,
    clock: ClockText,
    time: LocalTime,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    val hands = analogHandAngles(time)
    val progress = state.progressAt(remainingMillis)
    val complete = state.status == TimerStatus.RUNNING && remainingMillis <= 0L
    val idle = state.status == TimerStatus.IDLE
    val paused = state.status == TimerStatus.PAUSED
    val isFocus = state.sessionType.isFocus

    val target = if (complete) 1f else progress
    // Both animated values are read in the draw scope rather than here, so a moving hand or ring
    // only invalidates the canvas instead of recomposing the face every frame.
    val ringProgress = animateFloatAsState(
        targetValue = target,
        animationSpec = if (reducedMotion) snap() else tween(1_000, easing = LinearEasing),
        label = "analog progress",
    )

    // Keep a monotonic sweep so the hand never unwinds when the second wraps from 59 to 0.
    var sweep by remember { mutableFloatStateOf(hands.second) }
    var previousSecond by remember { mutableIntStateOf(time.second) }
    LaunchedEffect(time.second) {
        val delta = (time.second - previousSecond + 60) % 60
        if (delta > 0) sweep += delta * 6f
        previousSecond = time.second
    }
    val secondSweep = animateFloatAsState(
        targetValue = sweep,
        animationSpec = if (reducedMotion) snap() else tween(1_000, easing = LinearEasing),
        label = "second hand",
    )

    val ringColor = when {
        paused -> palette.textMuted
        isFocus -> palette.accent
        state.sessionType == SessionType.LONG_BREAK -> palette.foliageMuted
        else -> palette.textSecondary
    }

    val statusSuffix = when {
        complete -> " · complete"
        paused -> " · paused"
        idle -> " · ready"
        else -> ""
    }
    val sessionLine = "${sessionName(state)} · ${TimeFormatter.countdown(remainingMillis)}$statusSuffix"
    val progressPercent = (target * 100f).roundToInt()
    val digital = "${clock.time}${clock.meridiem?.let { " $it" }.orEmpty()}"
    val ringDescription = when {
        idle -> "No active progress"
        complete -> "Session complete"
        else -> {
            val kind = if (isFocus) "Focus" else "Break"
            "$kind progress $progressPercent percent elapsed${if (paused) ", paused" else ""}"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("analog-face")
            .semantics {
                contentDescription = "Analog clock showing $digital. $sessionLine. $ringDescription."
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawAnalogDial(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension * 0.33f,
                hour = hands.hour,
                minute = hands.minute,
                second = secondSweep.value,
                dial = palette.surface,
                bezel = palette.divider,
                marker = palette.textSecondary,
                markerMuted = palette.textMuted,
                hourHand = palette.textPrimary,
                minuteHand = palette.textPrimary,
                secondHand = palette.accent,
                hub = palette.background,
            )

            if (!idle) {
                drawProgressRing(
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 0.33f + 18.dp.toPx(),
                    progress = ringProgress.value,
                    track = palette.track,
                    color = ringColor,
                )
            }
        }

        Text(
            text = sessionLine,
            style = FocusType.caption,
            color = if (paused) palette.textMuted else palette.textSecondary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = FocusSpacing.huge, vertical = FocusSpacing.xl),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = FocusSpacing.lg),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = clock.time,
                style = FocusType.numerals,
                color = palette.textSecondary,
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
}

private fun DrawScope.drawAnalogDial(
    center: Offset,
    radius: Float,
    hour: Float,
    minute: Float,
    second: Float,
    dial: Color,
    bezel: Color,
    marker: Color,
    markerMuted: Color,
    hourHand: Color,
    minuteHand: Color,
    secondHand: Color,
    hub: Color,
) {
    drawCircle(color = dial, radius = radius, center = center)
    drawCircle(
        color = bezel,
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )

    // Twelve markers only; no numerals, no minute ticks.
    for (index in 0 until 12) {
        val angle = Math.toRadians((index * 30f - 90f).toDouble())
        val cardinal = index % 3 == 0
        val outer = radius * 0.91f
        val inner = radius * if (cardinal) 0.83f else 0.87f
        drawLine(
            color = if (cardinal) marker else markerMuted,
            start = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * inner,
            end = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * outer,
            strokeWidth = if (cardinal) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    drawHand(center, hour, radius * 0.50f, 5.dp.toPx(), hourHand, radius * 0.08f)
    drawHand(center, minute, radius * 0.72f, 3.dp.toPx(), minuteHand, radius * 0.10f)
    drawHand(center, second, radius * 0.80f, 1.2.dp.toPx(), secondHand, radius * 0.16f)

    drawCircle(color = hub, radius = 5.dp.toPx(), center = center)
    drawCircle(color = secondHand, radius = 2.dp.toPx(), center = center)
}

private fun DrawScope.drawHand(
    center: Offset,
    angle: Float,
    length: Float,
    width: Float,
    color: Color,
    tail: Float,
) {
    val radians = Math.toRadians((angle - 90f).toDouble())
    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    drawLine(
        color = color,
        start = center - direction * tail,
        end = center + direction * length,
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawProgressRing(
    center: Offset,
    radius: Float,
    progress: Float,
    track: Color,
    color: Color,
) {
    val width = 2.dp.toPx()
    drawCircle(
        color = track,
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
    if (progress <= 0f) return
    val topLeft = Offset(center.x - radius, center.y - radius)
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * progress.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = topLeft,
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = width, cap = StrokeCap.Round),
    )
}

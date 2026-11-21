package com.rabindra.focustime.ui.watchface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.ClockText
import com.rabindra.focustime.util.MorseCode
import com.rabindra.focustime.util.MorseTime
import com.rabindra.focustime.util.TimeFormatter
import java.time.LocalTime

private val MorseUnit = 5.dp
private val MorseMarkHeight = 4.dp

/**
 * The Pomodoro countdown is the subject. The wall clock is spelled out in real Morse below it:
 * dot = 1 unit, dash = 3, one unit inside a digit, three between digits, seven between the
 * hour and the minute. Nothing blinks; the code is simply drawn at its true proportions.
 */
@Composable
fun MorseCodeFace(
    state: TimerState,
    remainingMillis: Long,
    clock: ClockText,
    time: LocalTime,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    val morse = MorseCode.encodeTime(time, use24Hour)
    val spokenTime = buildString {
        append(clock.time)
        clock.meridiem?.let { append(' ').append(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FocusSpacing.huge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = sessionName(state),
            style = FocusType.caption,
            color = palette.textMuted,
        )
        Spacer(Modifier.height(FocusSpacing.sm))
        Text(
            text = TimeFormatter.countdown(remainingMillis),
            style = FocusType.clock,
            color = palette.textPrimary,
            modifier = Modifier.semantics {
                contentDescription = "Timer ${TimeFormatter.countdown(remainingMillis)}"
            },
        )
        Spacer(Modifier.height(FocusSpacing.xl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "LOCAL TIME",
                style = FocusType.caption,
                color = palette.textMuted,
            )
            clock.meridiem?.let {
                Spacer(Modifier.width(FocusSpacing.xs))
                Text(text = it, style = FocusType.caption, color = palette.textMuted)
            }
        }
        Spacer(Modifier.height(FocusSpacing.xs))
        Canvas(
            modifier = Modifier
                .width((morse.totalUnits * 5).dp)
                .height(22.dp)
                .semantics {
                    contentDescription = "Local time $spokenTime in Morse code"
                },
        ) {
            drawMorseTime(
                morse = morse,
                color = palette.textSecondary,
                unit = MorseUnit.toPx(),
                markHeight = MorseMarkHeight.toPx(),
            )
        }
    }
}

private fun sessionName(state: TimerState): String {
    val name = when (state.sessionType) {
        SessionType.FOCUS -> "Focus"
        SessionType.SHORT_BREAK -> "Short break"
        SessionType.LONG_BREAK -> "Long break"
    }
    return when (state.status) {
        TimerStatus.RUNNING -> name
        TimerStatus.PAUSED -> "$name · paused"
        TimerStatus.IDLE -> "$name · ready"
    }
}

private fun DrawScope.drawMorseTime(
    morse: MorseTime,
    color: Color,
    unit: Float,
    markHeight: Float,
) {
    val totalWidth = morse.totalUnits * unit
    var cursor = (size.width - totalWidth) / 2f
    val centerY = size.height / 2f

    fun drawCode(code: String) {
        code.forEachIndexed { index, element ->
            if (index > 0) cursor += unit
            if (element == '-') {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cursor, centerY - markHeight / 2f),
                    size = Size(3 * unit, markHeight),
                    cornerRadius = CornerRadius(markHeight / 2f),
                )
                cursor += 3 * unit
            } else {
                drawCircle(
                    color = color,
                    radius = unit / 2f,
                    center = Offset(cursor + unit / 2f, centerY),
                )
                cursor += unit
            }
        }
    }

    morse.hour.forEachIndexed { index, code ->
        if (index > 0) cursor += MorseCode.DIGIT_GAP * unit
        drawCode(code)
    }
    cursor += MorseCode.WORD_GAP * unit
    morse.minute.forEachIndexed { index, code ->
        if (index > 0) cursor += MorseCode.DIGIT_GAP * unit
        drawCode(code)
    }
}

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.rabindra.focustime.util.TimeFormatter

/**
 * The clock stays the subject; a single stem grows with the focus session and folds back into
 * the ground during a break. Growth is a plain function of session progress, so it never
 * rewards or hurries: it only shows how much of the session has passed.
 */
@Composable
fun GrowingPlantFace(
    state: TimerState,
    remainingMillis: Long,
    clock: ClockText,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    // Focus grows the plant; a break folds it back into the ground.
    val target = plantGrowth(state.sessionType, state.progressAt(remainingMillis))
    val growth by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = if (reducedMotion) snap() else tween(1_200, easing = LinearEasing),
        label = "plant growth",
    )
    val percent = (target * 100).toInt()

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
            Spacer(Modifier.height(FocusSpacing.sm))
            Text(
                text = sessionLine(state, remainingMillis),
                style = FocusType.caption,
                color = palette.textSecondary,
            )
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = FocusSpacing.huge, vertical = FocusSpacing.xl)
                .size(width = 172.dp, height = 152.dp)
                .semantics {
                    contentDescription = "Plant grown $percent percent"
                },
        ) {
            drawPlant(
                growth = growth,
                stemColor = palette.accent,
                leafColor = palette.accent.copy(alpha = 0.82f),
                groundColor = palette.divider,
            )
        }
    }
}

/**
 * Focus growth is the elapsed share; any break rests at the seed, whether it is ready, running or
 * paused, so a finished focus always visibly folds away.
 */
internal fun plantGrowth(sessionType: SessionType, elapsed: Float): Float =
    if (sessionType == SessionType.FOCUS) elapsed.coerceIn(0f, 1f) else 0f

private fun DrawScope.drawPlant(
    growth: Float,
    stemColor: Color,
    leafColor: Color,
    groundColor: Color,
) {
    val baseX = size.width / 2f
    val baseY = size.height - 2.dp.toPx()
    val maxStem = size.height - 20.dp.toPx()
    val stemHeight = maxStem * growth
    val lean = 6.dp.toPx() * growth
    val tip = Offset(baseX + lean, baseY - stemHeight)

    // Ground line, the only thing that hints at a pot.
    drawLine(
        color = groundColor,
        start = Offset(baseX - 26.dp.toPx(), baseY),
        end = Offset(baseX + 26.dp.toPx(), baseY),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )

    val stem = Path().apply {
        moveTo(baseX, baseY)
        quadraticBezierTo(baseX - 3.dp.toPx(), baseY - stemHeight * 0.55f, tip.x, tip.y)
    }
    drawPath(stem, color = stemColor, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))

    // A seed on the ground before anything opens.
    if (growth < 0.06f) {
        drawCircle(
            color = stemColor.copy(alpha = 0.9f),
            radius = 2.6.dp.toPx(),
            center = Offset(baseX, baseY - 3.dp.toPx()),
        )
        return
    }

    // Three leaf pairs open in order as the stem lengthens.
    val levels = listOf(0.34f to -1f, 0.58f to 1f, 0.8f to -1f)
    val thresholds = listOf(0.08f, 0.32f, 0.56f)
    levels.forEachIndexed { index, (level, side) ->
        val open = ((growth - thresholds[index]) / 0.22f).coerceIn(0f, 1f)
        if (open <= 0f) return@forEachIndexed
        val t = level
        val x = baseX + (tip.x - baseX) * t
        val y = baseY - stemHeight * t
        val length = 24.dp.toPx() * open
        val width = 7.dp.toPx() * open
        drawLeaf(
            origin = Offset(x, y),
            length = length,
            halfWidth = width,
            angle = if (side < 0) 200f else -20f,
            color = leafColor,
        )
    }

    // A single bud at the tip once the plant is nearly grown.
    val bud = ((growth - 0.86f) / 0.14f).coerceIn(0f, 1f)
    if (bud > 0f) {
        drawCircle(
            color = leafColor,
            radius = 3.dp.toPx() * bud,
            center = tip,
        )
    }
}

private fun DrawScope.drawLeaf(
    origin: Offset,
    length: Float,
    halfWidth: Float,
    angle: Float,
    color: Color,
) {
    rotate(degrees = angle, pivot = origin) {
        val leaf = Path().apply {
            moveTo(origin.x, origin.y)
            cubicTo(
                origin.x + length * 0.3f, origin.y - halfWidth,
                origin.x + length * 0.7f, origin.y - halfWidth,
                origin.x + length, origin.y,
            )
            cubicTo(
                origin.x + length * 0.7f, origin.y + halfWidth,
                origin.x + length * 0.3f, origin.y + halfWidth,
                origin.x, origin.y,
            )
        }
        drawPath(leaf, color = color)
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

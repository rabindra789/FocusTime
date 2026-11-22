package com.rabindra.focustime.ui.watchface

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.theme.FocusPalette
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.util.ClockText
import com.rabindra.focustime.util.TimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Forest grows one tree into a whole canopy as focus time passes, then lets the greenery clear
 * during a break. Everything is drawn from the session's own progress, so the face has no
 * animation clock of its own: it only eases between the values the timer publishes.
 *
 * The trees are placed with a fixed pseudo-random hash, so the same session always looks the
 * same and recomposition never shuffles the forest.
 */
@Composable
fun ForestFace(
    state: TimerState,
    remainingMillis: Long,
    clock: ClockText,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    val progress = state.progressAt(remainingMillis)
    val target = forestGrowth(state.sessionType, progress)
    val growth by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reducedMotion) snap() else tween(1_200, easing = LinearEasing),
        label = "forest growth",
    )
    val percent = (target * 100).toInt()

    Box(modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Forest grown $percent percent" },
        ) {
            drawForest(growth = growth, palette = palette)
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = FocusSpacing.huge),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = clock.time,
                    style = FocusType.timerCompact,
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
            Spacer(Modifier.height(FocusSpacing.xs))
            Text(
                text = sessionLine(state, remainingMillis),
                style = FocusType.caption,
                color = palette.textSecondary,
            )
        }
    }
}

/**
 * Focus grows the forest with elapsed time. A break clears it with the share of the break still
 * to run, so the canopy is at its fullest the moment focus ends and is bare by the last second.
 */
internal fun forestGrowth(sessionType: SessionType, elapsed: Float): Float =
    if (sessionType == SessionType.FOCUS) {
        elapsed.coerceIn(0f, 1f)
    } else {
        (1f - elapsed.coerceIn(0f, 1f))
    }

private fun DrawScope.drawForest(growth: Float, palette: FocusPalette) {
    val g = growth.coerceIn(0f, 1f)
    val width = size.width
    val height = size.height
    val unit = size.minDimension
    val groundY = height * 0.88f
    val baseX = width * 0.63f

    // Distant canopy: the room fills with green only in the second half of a session. The masses
    // hug the top and bottom edges so they read as a forest edge rather than floating blobs, and
    // the upper-left corner stays clear of the clock block.
    val distant = ((g - 0.40f) / 0.60f).coerceIn(0f, 1f)
    if (distant > 0f) {
        val topEdge = listOf(
            0.03f to -0.08f,
            0.15f to -0.05f,
            0.28f to -0.07f,
            0.40f to -0.02f,
            0.52f to 0.02f,
            0.64f to -0.05f,
            0.76f to 0.02f,
            0.88f to -0.03f,
            0.99f to 0.05f,
        )
        topEdge.forEachIndexed { index, (fx, fy) ->
            drawCluster(
                center = Offset(width * fx, height * fy),
                radius = unit * 0.135f * (0.55f + 0.45f * distant),
                color = palette.foliageMuted.copy(alpha = 0.30f + 0.22f * distant),
                seed = 100 + index * 17,
            )
        }
        val bottomEdge = listOf(
            0.02f to 0.99f,
            0.34f to 1.02f,
            0.66f to 1.00f,
            0.97f to 0.96f,
            0.00f to 0.70f,
            0.04f to 0.86f,
        )
        bottomEdge.forEachIndexed { index, (fx, fy) ->
            drawCluster(
                center = Offset(width * fx, height * fy),
                radius = unit * 0.11f * (0.6f + 0.4f * distant),
                color = palette.foliageMuted.copy(alpha = 0.35f * distant),
                seed = 130 + index * 19,
            )
        }
    }

    // Ground: a hairline horizon with a low scrub line that thickens as the forest fills in.
    drawLine(
        color = palette.divider,
        start = Offset(width * 0.05f, groundY),
        end = Offset(width * 0.95f, groundY),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val shrubs = listOf(0.46f to 0.10f, 0.55f to 0.30f, 0.72f to 0.55f, 0.84f to 0.25f, 0.93f to 0.45f, 0.16f to 0.62f)
    shrubs.forEachIndexed { index, (fx, threshold) ->
        val open = ((g - threshold) / 0.20f).coerceIn(0f, 1f)
        if (open <= 0f) return@forEachIndexed
        drawCluster(
            center = Offset(width * fx, groundY + unit * 0.006f),
            radius = unit * 0.032f * (0.6f + 0.5f * open),
            color = palette.foliage.copy(alpha = 0.55f * open),
            seed = 300 + index * 11,
        )
    }

    // Young trees at the edges appear one by one, so the space fills gradually rather than at once.
    val saplings = listOf(0.30f to 0.16f, 0.40f to 0.34f, 0.86f to 0.30f, 0.94f to 0.62f)
    saplings.forEachIndexed { index, (fx, threshold) ->
        val open = ((g - threshold) / 0.22f).coerceIn(0f, 1f)
        if (open <= 0f) return@forEachIndexed
        val base = Offset(width * fx, groundY)
        val saplingHeight = unit * 0.05f + unit * 0.06f * open
        val tip = Offset(base.x + unit * 0.004f, base.y - saplingHeight)
        drawLine(
            color = palette.accent.copy(alpha = 0.75f * open),
            start = base,
            end = tip,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCluster(
            center = tip,
            radius = unit * 0.024f * open,
            color = palette.foliage.copy(alpha = 0.7f * open),
            seed = 500 + index * 7,
        )
    }

    drawTree(growth = g, base = Offset(baseX, groundY), groundY = groundY, palette = palette)
}

private fun DrawScope.drawTree(growth: Float, base: Offset, groundY: Float, palette: FocusPalette) {
    val unit = size.minDimension
    val height = size.height
    val trunkHeight = height * (0.16f + 0.52f * growth)
    val trunkWidth = unit * 0.026f * (0.35f + 0.65f * growth)
    val lean = height * 0.02f * growth
    val top = Offset(base.x + lean, base.y - trunkHeight)

    if (growth < 0.05f) {
        // A sapling: a short stem and a seed still in the ground.
        drawLine(
            color = palette.accent,
            start = base,
            end = top,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = palette.accent,
            radius = unit * 0.006f,
            center = Offset(base.x, base.y),
        )
        return
    }

    val trunk = Path().apply {
        moveTo(base.x - trunkWidth / 2f, base.y)
        quadraticBezierTo(base.x - trunkWidth * 0.15f, base.y - trunkHeight * 0.55f, top.x - trunkWidth * 0.10f, top.y)
        lineTo(top.x + trunkWidth * 0.10f, top.y)
        quadraticBezierTo(base.x + trunkWidth * 0.20f, base.y - trunkHeight * 0.55f, base.x + trunkWidth / 2f, base.y)
        close()
    }
    drawPath(trunk, color = palette.accent)

    // Branches unfold from the bottom up; each side reaches out a little further as it opens.
    val levels = listOf(0.10f, 0.26f, 0.42f, 0.58f)
    levels.forEachIndexed { level, threshold ->
        val open = ((growth - threshold) / 0.18f).coerceIn(0f, 1f)
        if (open <= 0f) return@forEachIndexed
        val t = 0.36f + 0.15f * level
        val start = Offset(
            base.x + (top.x - base.x) * t,
            base.y + (top.y - base.y) * t,
        )
        val jitter = wobble(seed = 700 + level)
        listOf(-1f, 1f).forEachIndexed { side, direction ->
            val angle = if (direction < 0f) {
                -125f + 7f * level + jitter * 6f
            } else {
                -55f - 5f * level - jitter * 5f
            }
            val length = trunkHeight * (0.30f - 0.035f * level) * open * (0.85f + 0.25f * jitter)
            val tip = start + Offset(
                x = cos(angle.toRadians()) * length,
                y = sin(angle.toRadians()) * length,
            )
            drawLine(
                color = palette.accent,
                start = start,
                end = tip,
                strokeWidth = (unit * 0.005f * (1f - 0.16f * level)).coerceAtLeast(1.2.dp.toPx()),
                cap = StrokeCap.Round,
            )
            drawCluster(
                center = tip,
                radius = unit * 0.038f * (0.55f + 0.55f * open),
                color = if (side == 0) palette.foliage else palette.foliage.copy(alpha = 0.82f),
                seed = 900 + level * 23 + side * 5,
            )
        }
    }

    // The crown closes last, then widens until it holds most of the sky above the tree.
    val crown = ((growth - 0.30f) / 0.70f).coerceIn(0f, 1f)
    if (crown > 0f) {
        val crowns = listOf(
            0f to -0.02f,
            -0.34f to 0.18f,
            0.34f to 0.18f,
            -0.20f to -0.24f,
            0.20f to -0.26f,
            -0.44f to -0.02f,
            0.44f to -0.04f,
            0.04f to 0.30f,
        )
        val crownRadius = unit * (0.035f + 0.115f * crown)
        crowns.forEachIndexed { index, (dx, dy) ->
            val open = ((crown - index * 0.06f) / 0.45f).coerceIn(0f, 1f)
            if (open <= 0f) return@forEachIndexed
            drawCluster(
                center = Offset(top.x + crownRadius * dx * 1.35f, top.y + crownRadius * dy * 1.35f),
                radius = crownRadius * (0.48f + 0.28f * open),
                color = if (index % 2 == 0) palette.foliage else palette.foliage.copy(alpha = 0.86f),
                seed = 1200 + index * 31,
            )
        }
    }

    // A few blades at the trunk keep the base from looking pasted on.
    if (growth > 0.2f) {
        val blade = unit * 0.02f * ((growth - 0.2f) / 0.8f).coerceIn(0f, 1f)
        listOf(-1f, 1f).forEachIndexed { index, direction ->
            drawLine(
                color = palette.foliageMuted.copy(alpha = 0.8f),
                start = Offset(base.x + direction * trunkWidth, groundY),
                end = Offset(base.x + direction * (trunkWidth + blade * 0.5f), groundY - blade),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** A loose leaf mass: overlapping circles placed by a fixed hash, never by a live random. */
private fun DrawScope.drawCluster(center: Offset, radius: Float, color: Color, seed: Int) {
    val leaves = 6
    repeat(leaves) { index ->
        val angle = (index * 360f / leaves + wobble(seed + index) * 44f) * PI.toFloat() / 180f
        val distance = radius * (0.40f + 0.35f * wobble(seed * 3 + index))
        val leafRadius = radius * (0.42f + 0.30f * wobble(seed * 7 + index))
        drawCircle(
            color = color,
            radius = leafRadius,
            center = Offset(
                center.x + cos(angle) * distance,
                center.y + sin(angle) * distance * 0.78f,
            ),
        )
    }
}

/** Deterministic 0..1 hash. Keeps the forest identical across frames and rotations. */
private fun wobble(seed: Int): Float {
    var value = seed * 1103515245 + 12345
    value = value xor (value shr 13)
    value *= 1274126177
    return ((value ushr 8) and 0xFFFF).toFloat() / 0xFFFF.toFloat()
}

private fun Float.toRadians(): Float = this * PI.toFloat() / 180f

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

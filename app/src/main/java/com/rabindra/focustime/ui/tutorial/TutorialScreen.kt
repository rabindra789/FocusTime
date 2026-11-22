package com.rabindra.focustime.ui.tutorial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rabindra.focustime.ui.components.QuietAction
import com.rabindra.focustime.ui.theme.FocusSpacing
import com.rabindra.focustime.ui.theme.FocusType
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.ui.watchface.rememberReducedMotion

private enum class Demo { NONE, TAP, DOUBLE_TAP, SWIPE, LONG_PRESS }

private data class TutorialStep(
    val title: String,
    val description: String,
    val demo: Demo,
)

private val steps = listOf(
    TutorialStep(
        title = "Your time. Your focus.",
        description = "A simple clock for focused work. No distractions.",
        demo = Demo.NONE,
    ),
    TutorialStep(
        title = "Tap anywhere",
        description = "Reveal or hide the clock controls.",
        demo = Demo.TAP,
    ),
    TutorialStep(
        title = "Double tap",
        description = "Pause or resume your session.",
        demo = Demo.DOUBLE_TAP,
    ),
    TutorialStep(
        title = "Swipe left or right",
        description = "Explore different clock faces.",
        demo = Demo.SWIPE,
    ),
    TutorialStep(
        title = "Press and hold",
        description = "Access additional session actions.",
        demo = Demo.LONG_PRESS,
    ),
    TutorialStep(
        title = "You're ready.",
        description = "Start your session and stay focused.",
        demo = Demo.NONE,
    ),
)

/**
 * First-launch gesture guide. Six quiet steps, a visible progress line, and a way out at every
 * point. Completion is persisted by the caller, so the guide never returns on its own.
 */
@Composable
fun TutorialScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFocusPalette.current
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val isLast = stepIndex == steps.lastIndex

    Column(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.xl, vertical = FocusSpacing.lg)
            .testTag("tutorial"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepProgress(
                current = stepIndex,
                total = steps.size,
                modifier = Modifier.weight(1f),
            )
            QuietAction(label = "Skip", onClick = onSkip, color = palette.textSecondary)
        }

        Spacer(Modifier.height(FocusSpacing.xxl))

        Text(
            text = step.title,
            style = FocusType.display,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(FocusSpacing.sm))
        Text(
            text = step.description,
            style = FocusType.body,
            color = palette.textSecondary,
        )

        if (step.demo != Demo.NONE) {
            Spacer(Modifier.height(FocusSpacing.xl))
            GestureDemo(
                demo = step.demo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            )
        }

        Spacer(Modifier.height(FocusSpacing.xxl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (stepIndex > 0) {
                QuietAction(
                    label = "Back",
                    onClick = { stepIndex -= 1 },
                    color = palette.textSecondary,
                )
            } else {
                Spacer(Modifier)
            }
            if (isLast) {
                QuietAction(
                    label = "Start focusing",
                    onClick = onComplete,
                    color = palette.accent,
                    modifier = Modifier.testTag("tutorial-finish"),
                )
            } else {
                QuietAction(
                    label = "Next",
                    onClick = { stepIndex += 1 },
                    color = palette.accent,
                    modifier = Modifier.testTag("tutorial-next"),
                )
            }
        }

        Spacer(Modifier.height(FocusSpacing.lg))
    }
}

/** One hairline segment per step; the current step is the only bright one. */
@Composable
private fun StepProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = modifier
            .semantics { contentDescription = "Step ${current + 1} of $total" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(if (index == current) palette.accent else palette.track),
            )
        }
    }
}

@Composable
private fun GestureDemo(demo: Demo, modifier: Modifier = Modifier) {
    val palette = LocalFocusPalette.current
    val reducedMotion = rememberReducedMotion()
    val progress = if (reducedMotion) {
        0.65f
    } else {
        val transition = rememberInfiniteTransition(label = "gesture demo")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "gesture progress",
        )
        animated
    }

    Canvas(modifier.testTag("gesture-demo")) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val accent = palette.accent
        val muted = palette.textMuted
        when (demo) {
            Demo.TAP -> {
                val radius = size.minDimension * (0.16f - 0.08f * progress)
                drawCircle(
                    color = accent.copy(alpha = 0.9f - 0.7f * progress),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawCircle(color = muted, radius = 2.dp.toPx(), center = center)
            }

            Demo.DOUBLE_TAP -> {
                for (offset in listOf(0f, 0.3f)) {
                    val phase = (progress - offset + 1f) % 1f
                    if (phase < 0.55f) {
                        val local = (phase / 0.55f)
                        drawCircle(
                            color = accent.copy(alpha = 0.9f - 0.7f * local),
                            radius = size.minDimension * (0.16f - 0.08f * local),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }
                drawCircle(color = muted, radius = 2.dp.toPx(), center = center)
            }

            Demo.SWIPE -> {
                val y = center.y
                drawLine(
                    color = palette.divider,
                    start = Offset(size.width * 0.15f, y),
                    end = Offset(size.width * 0.85f, y),
                    strokeWidth = 1.dp.toPx(),
                )
                val x = size.width * (0.2f + 0.6f * progress)
                drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(x, y))
            }

            Demo.LONG_PRESS -> {
                val radius = size.minDimension * 0.14f
                drawCircle(
                    color = palette.track,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(color = muted, radius = 2.dp.toPx(), center = center)
            }

            Demo.NONE -> Unit
        }
    }
}

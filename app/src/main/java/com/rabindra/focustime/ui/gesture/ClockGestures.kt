package com.rabindra.focustime.ui.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * The four gesture directions FocusTime listens for. Up and down are screen-relative: an upward
 * swipe moves the finger towards the top of the display.
 */
enum class SwipeDirection { LEFT, RIGHT, UP, DOWN }

/**
 * Classifies a finished drag. A drag only counts as a swipe when it travels [minDistance] and has
 * a clear dominant axis, so diagonal wobble during a long press cannot change the watch face.
 */
fun swipeDirectionFor(dx: Float, dy: Float, minDistance: Float): SwipeDirection? {
    if (abs(dx) < minDistance && abs(dy) < minDistance) return null
    if (abs(dx) >= abs(dy)) {
        return if (dx > 0f) SwipeDirection.RIGHT else SwipeDirection.LEFT
    }
    return if (dy > 0f) SwipeDirection.DOWN else SwipeDirection.UP
}

private data class ClockGestureHandlers(
    val onTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onLongPress: () -> Unit,
    val onSwipe: (SwipeDirection) -> Unit,
)

/**
 * One gesture layer shared by every clock face. Tap, double tap and long press are resolved by
 * [detectTapGestures], which already guarantees that a double tap swallows the single tap and a
 * long press cancels the tap. A second detector classifies drags; because it consumes once it
 * leaves the touch slop, [detectTapGestures] cancels its pending tap, so swipes never toggle the
 * controls as well.
 */
@Composable
fun Modifier.clockGestures(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    enabled: Boolean = true,
): Modifier {
    val handlers = rememberUpdatedState(
        ClockGestureHandlers(onTap, onDoubleTap, onLongPress, onSwipe),
    )
    return this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onTap = { handlers.value.onTap() },
                onDoubleTap = { handlers.value.onDoubleTap() },
                onLongPress = { handlers.value.onLongPress() },
            )
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val minDistance = 56.dp.toPx()
            var total = Offset.Zero
            detectDragGestures(
                onDragStart = { total = Offset.Zero },
                onDrag = { change, amount ->
                    total += amount
                    change.consume()
                },
                onDragEnd = {
                    swipeDirectionFor(total.x, total.y, minDistance)?.let {
                        handlers.value.onSwipe(it)
                    }
                    total = Offset.Zero
                },
                onDragCancel = { total = Offset.Zero },
            )
        }
}

package com.rabindra.focustime

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.dashboard.DashboardScreen
import com.rabindra.focustime.ui.landscape.LandscapeClockScreen
import com.rabindra.focustime.ui.theme.FocusTimeTheme
import com.rabindra.focustime.watchface.WatchFace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GestureUiTest {

    @get:Rule
    val rule = createComposeRule()

    private fun tapClock() {
        rule.onNodeWithTag("clock-gestures").performTouchInput { down(center); up() }
        // Let the single-tap wait for a possible second tap expire.
        rule.mainClock.advanceTimeBy(400)
        rule.waitForIdle()
    }

    private fun runningState() = TimerState(
        sessionType = SessionType.FOCUS,
        status = TimerStatus.RUNNING,
        durationMillis = 25 * 60_000L,
        remainingMillis = 20 * 60_000L,
        cyclePosition = 1,
        sessionNumber = 2,
    )

    private fun showClock(
        state: TimerState = runningState(),
        onToggle: () -> Unit = {},
        onSkip: () -> Unit = {},
        onReset: () -> Unit = {},
    ): () -> WatchFace {
        val face = mutableStateOf(WatchFace.DESK_CLOCK)
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                LandscapeClockScreen(
                    state = state,
                    remainingMillis = state.remainingMillis,
                    use24Hour = false,
                    onStartPause = onToggle,
                    onSkip = onSkip,
                    onReset = onReset,
                    watchFace = face.value,
                    onWatchFaceChange = { face.value = it },
                )
            }
        }
        return { face.value }
    }

    @Test
    fun singleTapTogglesControls() {
        showClock()
        tapClock()
        rule.onNodeWithText("Pause").assertIsDisplayed()
        rule.onNodeWithText("Skip").assertIsDisplayed()
        tapClock()
        rule.onAllNodesWithText("Pause").assertCountEquals(0)
    }

    @Test
    fun doubleTapTogglesSessionOnceAndSwallowsSingleTap() {
        var toggles = 0
        showClock(onToggle = { toggles++ })
        rule.onNodeWithTag("clock-gestures").performTouchInput { doubleClick() }
        rule.runOnIdle { assertEquals(1, toggles) }
        // The single tap that shares the double tap must not have opened the controls.
        rule.onAllNodesWithText("Pause").assertCountEquals(0)
    }

    @Test
    fun longPressOpensQuickActionsWithoutResetting() {
        var resets = 0
        var skips = 0
        showClock(onReset = { resets++ }, onSkip = { skips++ })
        rule.onNodeWithTag("clock-gestures").performTouchInput { longClick() }
        rule.onNodeWithText("Session actions").assertIsDisplayed()
        rule.onNodeWithText("Skip").assertIsDisplayed()
        rule.onNodeWithText("Reset").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(0, resets)
            assertEquals(0, skips)
        }
    }

    @Test
    fun swipeLeftShowsNextFaceAndSwipesDoNotToggleControls() {
        val face = showClock()
        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeLeft() }
        rule.runOnIdle { assertEquals(WatchFace.ANALOG, face()) }
        rule.onAllNodesWithText("Pause").assertCountEquals(0)
    }

    @Test
    fun swipeRightShowsPreviousFace() {
        val face = showClock()
        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeRight() }
        rule.runOnIdle { assertEquals(WatchFace.FOREST, face()) }
    }

    @Test
    fun swipeUpAsksBeforeSkipping() {
        var skips = 0
        showClock(onSkip = { skips++ })
        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeUp() }
        rule.onNodeWithText("Skip session?").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, skips) }
        rule.onNodeWithTag("confirm-skip").performClick()
        rule.runOnIdle { assertEquals(1, skips) }
    }

    @Test
    fun swipeDownShowsSessionPanel() {
        showClock()
        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeDown() }
        rule.onNodeWithTag("session-panel").assertIsDisplayed()
    }

    @Test
    fun resetRequiresConfirmation() {
        var resets = 0
        showClock(onReset = { resets++ })
        tapClock()
        rule.onNodeWithText("Reset").performClick()
        rule.onNodeWithText("Reset session?").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, resets) }
        rule.onNodeWithTag("confirm-reset").performClick()
        rule.runOnIdle { assertEquals(1, resets) }
    }

    @Test
    fun dashboardTapRevealsControlsAndDoubleTapPauses() {
        var toggles = 0
        val state = runningState()
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                DashboardScreen(
                    state = state,
                    remainingMillis = state.remainingMillis,
                    onStartPause = { toggles++ },
                    onReset = {},
                    onSkip = {},
                    onSettings = {},
                )
            }
        }
        tapClock()
        rule.runOnIdle { assertEquals(0, toggles) }
        rule.onNodeWithText("Pause").assertIsDisplayed()
        rule.onNodeWithTag("clock-gestures").performTouchInput { doubleClick() }
        rule.runOnIdle { assertEquals(1, toggles) }
    }

    @Test
    fun dashboardLongPressOffersSkipAndReset() {
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                DashboardScreen(
                    state = runningState(),
                    remainingMillis = 20 * 60_000L,
                    onStartPause = {},
                    onReset = {},
                    onSkip = {},
                    onSettings = {},
                )
            }
        }
        rule.onNodeWithTag("clock-gestures").performTouchInput { longClick() }
        rule.onNodeWithText("Session actions").assertIsDisplayed()
        rule.onNodeWithText("Skip").assertIsDisplayed()
        rule.onNodeWithText("Reset").assertIsDisplayed()
    }

    @Test
    fun dashboardSwipeSelectsNextFaceWithoutTapping() {
        val selected = mutableStateOf(WatchFace.DESK_CLOCK)
        var toggles = 0
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                DashboardScreen(
                    state = runningState(),
                    remainingMillis = 20 * 60_000L,
                    onStartPause = { toggles++ },
                    onReset = {},
                    onSkip = {},
                    onSettings = {},
                    watchFace = selected.value,
                    onWatchFaceChange = { selected.value = it },
                )
            }
        }
        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeLeft() }
        rule.runOnIdle {
            assertEquals(WatchFace.ANALOG, selected.value)
            assertEquals(0, toggles)
        }
        rule.onNodeWithContentDescription("Watch face Analog").assertIsDisplayed()
    }
}

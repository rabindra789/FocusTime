package com.rabindra.focustime

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.landscape.LandscapeClockScreen
import com.rabindra.focustime.ui.theme.FocusTimeTheme
import com.rabindra.focustime.ui.theme.LocalFocusPalette
import com.rabindra.focustime.ui.theme.LowPowerPalette
import com.rabindra.focustime.ui.watchface.AnalogFace
import com.rabindra.focustime.util.TimeFormatter
import com.rabindra.focustime.watchface.WatchFace
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalogFaceUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val tenPastNine = LocalTime.of(10, 9, 30)

    private fun timer(
        type: SessionType = SessionType.FOCUS,
        status: TimerStatus = TimerStatus.RUNNING,
        remaining: Long = 15 * 60_000L,
        duration: Long = 25 * 60_000L,
    ) = TimerState(
        sessionType = type,
        status = status,
        durationMillis = duration,
        remainingMillis = remaining,
        sessionNumber = 2,
    )

    private fun showAnalog(
        state: TimerState = timer(),
        time: LocalTime = tenPastNine,
        use24Hour: Boolean = false,
        reducedMotion: Boolean = false,
        lowPower: Boolean = false,
    ) {
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                CompositionLocalProvider(
                    LocalFocusPalette provides if (lowPower) LowPowerPalette else LocalFocusPalette.current,
                ) {
                    AnalogFace(
                        state = state,
                        remainingMillis = state.remainingMillis,
                        clock = TimeFormatter.clock(time, use24Hour),
                        time = time,
                        reducedMotion = reducedMotion,
                    )
                }
            }
        }
    }

    private fun assertDescriptionContains(text: String) {
        rule.onNodeWithTag("analog-face").assert(
            SemanticsMatcher("content description contains \"$text\"") { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { it.contains(text) } == true
            },
        )
    }

    @Test
    fun rendersDialDigitalTimeAndSession() {
        showAnalog()
        rule.onNodeWithTag("analog-face").assertIsDisplayed()
        rule.onNodeWithText("10:09").assertIsDisplayed()
        rule.onNodeWithText("AM").assertIsDisplayed()
        rule.onNodeWithText("Focus · 15:00").assertIsDisplayed()
        assertDescriptionContains("Analog clock showing 10:09 AM")
    }

    @Test
    fun usesTheTwentyFourHourSetting() {
        showAnalog(use24Hour = true, time = LocalTime.of(22, 9, 30))
        rule.onNodeWithText("22:09").assertIsDisplayed()
        assertDescriptionContains("Analog clock showing 22:09")
    }

    @Test
    fun focusRingFollowsTheRealTimerProgress() {
        showAnalog()
        // Fifteen of twenty-five minutes elapsed is forty percent.
        assertDescriptionContains("Focus progress 40 percent elapsed")
    }

    @Test
    fun shortBreakIsQuietAndReadable() {
        showAnalog(state = timer(type = SessionType.SHORT_BREAK, remaining = 5 * 60_000L, duration = 5 * 60_000L))
        rule.onNodeWithText("Short break · 05:00").assertIsDisplayed()
        assertDescriptionContains("Break progress 0 percent elapsed")
    }

    @Test
    fun longBreakIsDistinctFromShortBreak() {
        showAnalog(
            state = timer(type = SessionType.LONG_BREAK, remaining = 10 * 60_000L, duration = 15 * 60_000L),
        )
        rule.onNodeWithText("Long break · 10:00").assertIsDisplayed()
        assertDescriptionContains("Break progress 33 percent elapsed")
    }

    @Test
    fun pausedStateFreezesAndSaysSo() {
        showAnalog(state = timer(status = TimerStatus.PAUSED))
        rule.onNodeWithText("Focus · 15:00 · paused").assertIsDisplayed()
        assertDescriptionContains("paused")
    }

    @Test
    fun idleStateHasNoProgressRing() {
        showAnalog(
            state = timer(status = TimerStatus.IDLE, remaining = 25 * 60_000L),
        )
        rule.onNodeWithText("Focus · 25:00 · ready").assertIsDisplayed()
        assertDescriptionContains("No active progress")
    }

    @Test
    fun completedStateIsBriefAndRestrained() {
        showAnalog(state = timer(remaining = 0L))
        rule.onNodeWithText("Focus · 00:00 · complete").assertIsDisplayed()
        assertDescriptionContains("Session complete")
    }

    @Test
    fun reducedMotionAndLowPowerStillRenderTheFace() {
        showAnalog(reducedMotion = true, lowPower = true)
        rule.onNodeWithTag("analog-face").assertIsDisplayed()
        rule.onNodeWithText("10:09").assertIsDisplayed()
        assertDescriptionContains("Focus progress 40 percent elapsed")
    }

    @Test
    fun analogUsesTheSharedGestureLayer() {
        var toggles = 0
        val face = mutableStateOf(WatchFace.ANALOG)
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                LandscapeClockScreen(
                    state = timer(),
                    remainingMillis = 15 * 60_000L,
                    use24Hour = false,
                    onStartPause = { toggles++ },
                    onSkip = {},
                    onReset = {},
                    watchFace = face.value,
                    onWatchFaceChange = { face.value = it },
                )
            }
        }

        rule.onNodeWithTag("clock-gestures").performTouchInput { doubleClick() }
        rule.runOnIdle { assertEquals(1, toggles) }

        // Swiping changes the face, never the timer.
        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeLeft() }
        rule.runOnIdle {
            assertEquals(WatchFace.CIRCULAR_POMODORO, face.value)
            assertEquals(1, toggles)
        }

        rule.onNodeWithTag("clock-gestures").performTouchInput { longClick() }
        rule.onNodeWithText("Session actions").assertIsDisplayed()
    }

    @Test
    fun switchingBackToAnalogKeepsTheTimerState() {
        var toggles = 0
        val face = mutableStateOf(WatchFace.ANALOG)
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                LandscapeClockScreen(
                    state = timer(status = TimerStatus.PAUSED),
                    remainingMillis = 15 * 60_000L,
                    use24Hour = true,
                    onStartPause = { toggles++ },
                    onSkip = {},
                    onReset = {},
                    watchFace = face.value,
                    onWatchFaceChange = { face.value = it },
                )
            }
        }

        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("clock-gestures").performTouchInput { swipeRight() }
        rule.runOnIdle {
            assertEquals(WatchFace.ANALOG, face.value)
            assertEquals(0, toggles)
        }
        rule.onNodeWithTag("analog-face").assertIsDisplayed()
        rule.onNodeWithText("Focus · 15:00 · paused").assertIsDisplayed()
    }
}

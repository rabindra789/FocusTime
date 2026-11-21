package com.rabindra.focustime

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rabindra.focustime.data.AppSettings
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.ui.dashboard.DashboardScreen
import com.rabindra.focustime.ui.settings.SettingsScreen
import com.rabindra.focustime.ui.theme.FocusTimeTheme
import com.rabindra.focustime.watchface.WatchFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val settings = AppSettings()

    private fun idleState() = TimerState(
        sessionType = SessionType.FOCUS,
        status = TimerStatus.IDLE,
        durationMillis = 25 * 60_000L,
        remainingMillis = 25 * 60_000L,
        cyclePosition = 1,
        sessionNumber = 1,
    )

    private fun showDashboard(
        state: TimerState,
        remaining: Long = 25 * 60_000L,
        onToggle: () -> Unit = {},
        onReset: () -> Unit = {},
        onSkip: () -> Unit = {},
    ) {
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                DashboardScreen(
                    state = state,
                    remainingMillis = remaining,
                    onStartPause = onToggle,
                    onReset = onReset,
                    onSkip = onSkip,
                    onSettings = {},
                    showTutorial = false,
                )
            }
        }
    }

    @Test
    fun dashboardShowsCountdownAndSession() {
        showDashboard(idleState())
        rule.onNodeWithText("25:00").assertIsDisplayed()
        rule.onNodeWithText("SESSION 01").assertIsDisplayed()
        rule.onNodeWithText("Focus \u00b7 ready").assertIsDisplayed()
    }

    @Test
    fun tappingTimerTogglesPause() {
        var toggled = false
        showDashboard(idleState(), onToggle = { toggled = true })
        rule.onNodeWithContentDescription("Timer").performClick()
        rule.runOnIdle { assertTrue(toggled) }
    }

    @Test
    fun runningSessionOffersPauseBehindControls() {
        showDashboard(
            idleState().copy(status = TimerStatus.RUNNING),
            remaining = 20 * 60_000L,
        )
        rule.onNodeWithText("20:00").assertIsDisplayed()
        rule.onNodeWithText("More").performClick()
        rule.onNodeWithText("Pause").assertIsDisplayed()
        rule.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun pausedSessionOffersStart() {
        showDashboard(
            idleState().copy(status = TimerStatus.PAUSED),
            remaining = 9 * 60_000L,
        )
        rule.onNodeWithText("09:00").assertIsDisplayed()
        rule.onNodeWithText("More").performClick()
        rule.onNodeWithText("Start").assertIsDisplayed()
        rule.onNodeWithText("Focus \u00b7 paused").assertIsDisplayed()
    }

    @Test
    fun focusDurationStepperUpdatesSettings() {
        var updated: AppSettings? = null
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                SettingsScreen(
                    settings = settings,
                    dndAccess = false,
                    onUpdate = { transform -> updated = transform(settings) },
                    onResetStatistics = {},
                    onRefreshDndAccess = {},
                    onOpenDndSettings = {},
                    onBack = {},
                )
            }
        }
        rule.onAllNodesWithText("\u2212")[0].performClick()
        rule.runOnIdle { assertEquals(24, updated?.focusMinutes) }
    }

    @Test
    fun watchFaceRowCyclesAndLowPowerIsOffered() {
        var updated: AppSettings? = null
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                SettingsScreen(
                    settings = settings,
                    dndAccess = false,
                    onUpdate = { transform -> updated = transform(settings) },
                    onResetStatistics = {},
                    onRefreshDndAccess = {},
                    onOpenDndSettings = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Low power mode").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Landscape watch face").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(WatchFace.CIRCULAR_POMODORO, updated?.watchFace)
        }
    }

    @Test
    fun dndRowsAreVisibleInSettings() {
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                SettingsScreen(
                    settings = settings,
                    dndAccess = false,
                    onUpdate = {},
                    onResetStatistics = {},
                    onRefreshDndAccess = {},
                    onOpenDndSettings = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Automatic Do Not Disturb").assertIsDisplayed()
        rule.onNodeWithText("GRANT").assertIsDisplayed()
    }
}

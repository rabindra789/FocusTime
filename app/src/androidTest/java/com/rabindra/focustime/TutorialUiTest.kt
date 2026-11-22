package com.rabindra.focustime

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rabindra.focustime.data.AppSettings
import com.rabindra.focustime.ui.settings.SettingsScreen
import com.rabindra.focustime.ui.theme.FocusTimeTheme
import com.rabindra.focustime.ui.tutorial.TutorialScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorialUiTest {

    @get:Rule
    val rule = createComposeRule()

    private fun showTutorial(
        onComplete: () -> Unit = {},
        onSkip: () -> Unit = {},
    ) {
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                TutorialScreen(onComplete = onComplete, onSkip = onSkip)
            }
        }
    }

    @Test
    fun firstStepIntroducesTheClock() {
        showTutorial()
        rule.onNodeWithText("Your time. Your focus.").assertIsDisplayed()
        rule.onNodeWithContentDescription("Step 1 of 6").assertIsDisplayed()
        rule.onAllNodesWithText("Back").assertCountEquals(0)
    }

    @Test
    fun nextAndBackWalkTheSteps() {
        showTutorial()
        rule.onNodeWithTag("tutorial-next").performClick()
        rule.onNodeWithText("Tap anywhere").assertIsDisplayed()
        rule.onNodeWithContentDescription("Step 2 of 6").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Your time. Your focus.").assertIsDisplayed()
    }

    @Test
    fun skipLeavesTheGuide() {
        var skipped = false
        showTutorial(onSkip = { skipped = true })
        rule.onNodeWithText("Skip").performClick()
        rule.runOnIdle { assertTrue(skipped) }
    }

    @Test
    fun lastStepStartsFocusing() {
        var completed = false
        showTutorial(onComplete = { completed = true })
        repeat(5) { rule.onNodeWithTag("tutorial-next").performClick() }
        rule.onNodeWithText("You're ready.").assertIsDisplayed()
        rule.onNodeWithText("Start focusing").performClick()
        rule.runOnIdle { assertTrue(completed) }
    }

    @Test
    fun progressSurvivesRecreation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            FocusTimeTheme(darkTheme = true) {
                TutorialScreen(onComplete = {}, onSkip = {})
            }
        }
        rule.onNodeWithTag("tutorial-next").performClick()
        rule.onNodeWithTag("tutorial-next").performClick()
        rule.onNodeWithText("Double tap").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Double tap").assertIsDisplayed()
        rule.onNodeWithContentDescription("Step 3 of 6").assertIsDisplayed()
    }

    @Test
    fun replayRowClearsTutorialCompletion() {
        var updated: AppSettings? = null
        val settings = AppSettings(tutorialCompleted = true)
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
        rule.onNodeWithText("Replay tutorial").performScrollTo().performClick()
        rule.runOnIdle { assertFalse(updated?.tutorialCompleted ?: true) }
    }
}

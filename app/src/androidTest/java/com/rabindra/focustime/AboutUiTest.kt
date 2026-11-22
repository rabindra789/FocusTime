package com.rabindra.focustime

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rabindra.focustime.ui.about.AboutScreen
import com.rabindra.focustime.ui.about.CREATOR_NAME
import com.rabindra.focustime.ui.theme.FocusTimeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutUiTest {

    @get:Rule
    val rule = createComposeRule()

    private fun showAbout() {
        rule.setContent {
            FocusTimeTheme(darkTheme = true) {
                AboutScreen(onBack = {}, versionName = "1.1.0", versionCode = 2)
            }
        }
    }

    @Test
    fun aboutNamesCreatorAndVersion() {
        showAbout()
        rule.onNodeWithText(CREATOR_NAME).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1.1.0 (2)").assertIsDisplayed()
    }

    @Test
    fun licenseCanBeReadInApp() {
        showAbout()
        rule.onNodeWithText("MIT License").performScrollTo().performClick()
        rule.onNodeWithText("Permission is hereby granted", substring = true)
            .assertIsDisplayed()
    }
}

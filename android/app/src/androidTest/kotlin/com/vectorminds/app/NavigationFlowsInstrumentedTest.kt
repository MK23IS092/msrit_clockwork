package com.vectorminds.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Validates bottom-nav flows without requiring a live backend.
 * Empty trend list shows "No trends yet"; with data, cards would appear.
 */
class NavigationFlowsInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNav_toTrends_showsLeaderboardOrEmptyState() {
        composeTestRule.onNodeWithText("Trends", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Trend Leaderboard", substring = true).assertIsDisplayed()
    }

    @Test
    fun bottomNav_toBlueprints_showsTitle() {
        composeTestRule.onNodeWithText("Blueprints", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Product Blueprints", substring = true).assertIsDisplayed()
    }

    @Test
    fun bottomNav_toPipelines_showsTitle() {
        composeTestRule.onNodeWithText("Pipelines", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ML Pipelines", substring = true).assertIsDisplayed()
    }

    @Test
    fun bottomNav_toSettings_showsBackendSection() {
        composeTestRule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Backend Connection", substring = true).assertIsDisplayed()
    }

    @Test
    fun bottomNav_roundTrip_returnsToDashboard() {
        composeTestRule.onNodeWithText("Pipelines", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Dashboard", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("VectorMind").assertIsDisplayed()
    }
}

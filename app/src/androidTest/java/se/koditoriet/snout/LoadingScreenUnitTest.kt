package se.koditoriet.snout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import se.koditoriet.snout.ui.FullScreenLoadingManager
import se.koditoriet.snout.ui.screens.LoadingScreen

class LoadingScreenUnitTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `The loading screen is visible when calling showLoader and is not visible when calling hideLoader`() {
        composeTestRule.setContent {
            LoadingScreen()
        }
        // Invisible per default
        composeTestRule.onNodeWithTag("LoadingScreen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()

        // Set visible
        FullScreenLoadingManager.showLoader()
        composeTestRule.onNodeWithTag("LoadingScreen").assertExists()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertExists()

        // Set invisible
        FullScreenLoadingManager.hideLoader()
        composeTestRule.onNodeWithTag("LoadingScreen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()
    }

}
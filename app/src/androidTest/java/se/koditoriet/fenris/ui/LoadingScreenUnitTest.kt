package se.koditoriet.fenris.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import se.koditoriet.fenris.ui.components.LoadingOverlayImpl

class LoadingScreenUnitTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `The loading screen is visible when calling showLoader and is not visible when calling hideLoader`() = runTest {
        composeTestRule.setContent {
            LoadingOverlayImpl.Render()
        }

        // Invisible per default
        composeTestRule.onNodeWithTag("LoadingScreen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()

        // Set visible
        LoadingOverlayImpl.show()
        composeTestRule.onNodeWithTag("LoadingScreen").assertExists()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertExists()

        // Set done
        LoadingOverlayImpl.done("")
        composeTestRule.onNodeWithTag("LoadingScreen").assertExists()
        composeTestRule.onNodeWithTag("LoadingCheckbox").assertExists()

        // Set failed
        LoadingOverlayImpl.show()
        LoadingOverlayImpl.done("", success = false)
        composeTestRule.onNodeWithTag("LoadingScreen").assertExists()
        composeTestRule.onNodeWithTag("LoadingWarningTriangle").assertExists()

        // Set invisible
        LoadingOverlayImpl.hide()
        composeTestRule.onNodeWithTag("LoadingScreen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()
    }

}
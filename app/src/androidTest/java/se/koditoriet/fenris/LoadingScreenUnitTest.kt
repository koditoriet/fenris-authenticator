package se.koditoriet.fenris

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import se.koditoriet.fenris.ui.components.LoadingOverlay

class LoadingScreenUnitTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `The loading screen is visible when calling showLoader and is not visible when calling hideLoader`() {
        val showLoadingScreen = mutableStateOf(false)

        composeTestRule.setContent {
            LoadingOverlay(showLoadingScreen.value)
        }
        // Invisible per default
        composeTestRule.onNodeWithTag("LoadingScreen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()

        // Set visible
        showLoadingScreen.value = true
        composeTestRule.onNodeWithTag("LoadingScreen").assertExists()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertExists()

        // Set invisible
        showLoadingScreen.value = false
        composeTestRule.onNodeWithTag("LoadingScreen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("LoadingSpinner").assertDoesNotExist()
    }

}
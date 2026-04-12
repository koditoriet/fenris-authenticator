package se.koditoriet.fenris.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.SecondaryButton

class MainButtonUnitTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun SemanticsNodeInteraction.assertIsMultiClickable(counter: MutableState<Int>) {
        val initialValue = counter.value
        assertExists()
        assertIsEnabled()

        performClick()
        assertExists()
        assertIsEnabled()
        assertEquals(initialValue + 1, counter.value)

        performClick()
        assertExists()
        assertIsEnabled()
        assertEquals(initialValue + 2, counter.value)

        performClick()
        assertExists()
        assertIsEnabled()
        assertEquals(initialValue + 3, counter.value)
    }

    private fun SemanticsNodeInteraction.assertIsOneshotClickable(counter: MutableState<Int>) {
        val initialValue = counter.value
        assertExists()
        assertIsEnabled()

        performClick()
        assertExists()
        assertIsNotEnabled()
        assertEquals(initialValue + 1, counter.value)

        performClick()
        assertExists()
        assertIsNotEnabled()
        assertEquals(initialValue + 1, counter.value)
    }

    @Test
    fun `secondary button is not present if secondaryButton is null`() = runTest {
        composeTestRule.setContent {
            Box {
                MainButton("", onClick = { })
            }
        }

        composeTestRule.onNodeWithTag("SecondaryButton").assertDoesNotExist()
    }

    @Test
    fun `disabled buttons are disabled`() = runTest {
        val mainCounter = mutableStateOf(0)
        val secondaryCounter = mutableStateOf(0)
        composeTestRule.setContent {
            Box {
                MainButton(
                    text = "",
                    onClick = { mainCounter.value += 1 },
                    enabled = false,
                    secondaryButton = SecondaryButton(
                        text = "",
                        enabled = false,
                        onClick = { secondaryCounter.value += 1 },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithTag("MainButton").run {
            assertExists()
            assertIsNotEnabled()
            performClick()
            assertEquals(0, mainCounter.value)
        }

        composeTestRule.onNodeWithTag("SecondaryButton").run {
            assertExists()
            assertIsNotEnabled()
            performClick()
            assertEquals(0, secondaryCounter.value)
        }
    }

    @Test
    fun `main button can be clicked multiple times if oneshot is false`() = runTest {
        val counter = mutableStateOf(0)
        composeTestRule.setContent {
            Box {
                MainButton("", onClick = { counter.value += 1 })
            }
        }

        composeTestRule.onNodeWithTag("MainButton").assertIsMultiClickable(counter)
    }

    @Test
    fun `main button can only be clicked once if oneshot is true`() = runTest {
        val counter = mutableStateOf(0)
        composeTestRule.setContent {
            Box {
                MainButton("", oneshot = true, onClick = { counter.value += 1 })
            }
        }

        composeTestRule.onNodeWithTag("MainButton").assertIsOneshotClickable(counter)
    }

    @Test
    fun `secondary button can be clicked multiple times if oneshot is false`() = runTest {
        val counter = mutableStateOf(0)
        composeTestRule.setContent {
            Box {
                MainButton(
                    text = "",
                    onClick = {},
                    secondaryButton = SecondaryButton(
                        text = "",
                        onClick = { counter.value += 1 },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithTag("SecondaryButton").assertIsMultiClickable(counter)
    }

    @Test
    fun `secondary button can only be clicked once if oneshot is true`() = runTest {
        val counter = mutableStateOf(0)
        composeTestRule.setContent {
            Box {
                MainButton(
                    text = "",
                    onClick = {},
                    secondaryButton = SecondaryButton(
                        text = "",
                        oneshot = true,
                        onClick = { counter.value += 1 },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithTag("SecondaryButton").assertIsOneshotClickable(counter)
    }
}

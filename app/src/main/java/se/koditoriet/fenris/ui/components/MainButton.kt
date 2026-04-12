package se.koditoriet.fenris.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import se.koditoriet.fenris.ui.theme.BUTTON_FONT_SIZE
import se.koditoriet.fenris.ui.theme.PADDING_L

val MAIN_BUTTON_HEIGHT = 60.dp
val MAIN_BUTTON_HEIGHT_WITH_SECONDARY = 120.dp

@Composable
fun BoxScope.MainButton(
    text: String,
    enabled: Boolean = true,
    secondaryButton: SecondaryButton? = null,
    oneshot: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) {
        var mainButtonClicked by remember { mutableStateOf(false) }
        val mainButtonEnabled = enabled && !(mainButtonClicked && oneshot)

        Button(
            onClick = {
                if (mainButtonEnabled) {
                    mainButtonClicked = true
                    onClick()
                }
            },
            enabled = mainButtonEnabled,
            elevation = ButtonDefaults.buttonElevation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(PADDING_L)
                .testTag("MainButton"),
        ) {
            Text(
                text = text,
                fontSize = BUTTON_FONT_SIZE,
            )
        }

        secondaryButton?.let {
            var secondaryButtonClicked by remember { mutableStateOf(false) }
            val secondaryButtonEnabled = it.enabled && !(secondaryButtonClicked && it.oneshot)

        TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SecondaryButton"),
                enabled = secondaryButtonEnabled,
                onClick = {
                    if (secondaryButtonEnabled) {
                        secondaryButtonClicked = true
                        it.onClick()
                    }
                },
            ) {
                Text(it.text)
            }
        }
    }
}

class SecondaryButton(
    val text: String,
    val enabled: Boolean = true,
    val oneshot: Boolean = false,
    val onClick: () -> Unit,
)

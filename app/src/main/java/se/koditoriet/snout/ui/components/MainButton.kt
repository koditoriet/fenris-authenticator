package se.koditoriet.snout.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.koditoriet.snout.ui.theme.BUTTON_FONT_SIZE
import se.koditoriet.snout.ui.theme.PADDING_L

val MAIN_BUTTON_HEIGHT = 60.dp
val MAIN_BUTTON_HEIGHT_WITH_SECONDARY = 120.dp

@Composable
fun BoxScope.MainButton(
    text: String,
    secondaryButton: SecondaryButton? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) {
        Button(
            onClick = onClick,
            elevation = ButtonDefaults.buttonElevation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(PADDING_L),
        ) {
            Text(
                text = text,
                fontSize = BUTTON_FONT_SIZE,
            )
        }

        secondaryButton?.let {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = it.onClick,
            ) {
                Text(it.text)
            }
        }
    }
}

class SecondaryButton(
    val text: String,
    val onClick: () -> Unit,
)

package se.koditoriet.fenris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import se.koditoriet.fenris.ui.primaryHint
import se.koditoriet.fenris.ui.theme.PADDING_L

@Composable
fun RequiresCapability(
    capability: String,
    capabilityRequiredMessage: String = "",
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val present = remember { ctx.packageManager.hasSystemFeature(capability) }

    if (present) {
        content()
    } else {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(PADDING_L)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = capabilityRequiredMessage,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primaryHint,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

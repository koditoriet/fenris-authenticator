package se.koditoriet.fenris.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import se.koditoriet.fenris.R

@Composable
fun PasskeyIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    flavor: PasskeyIconFlavor = PasskeyIconFlavor.Standard,
) {
    val resource = when (flavor) {
        PasskeyIconFlavor.Standard -> R.drawable.passkey_standard
        PasskeyIconFlavor.Fenris -> R.drawable.passkey_fenris
    }
    Icon(
        modifier = modifier,
        painter = painterResource(resource),
        contentDescription = null,
        tint = tint,
    )
}

enum class PasskeyIconFlavor {
    Standard,
    Fenris,
}

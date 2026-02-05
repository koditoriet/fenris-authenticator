package se.koditoriet.fenris.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.ui.components.MAIN_BUTTON_HEIGHT_WITH_SECONDARY
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.PasskeyIcon
import se.koditoriet.fenris.ui.components.SecondaryButton
import se.koditoriet.fenris.ui.theme.PADDING_XL
import se.koditoriet.fenris.ui.theme.SPACING_L

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnablePasskeysScreen(
    onDismiss: () -> Unit,
) {
    val screenStrings = appStrings.enablePasskeysScreen
    val credentialManager = LocalContext.current.let {
        remember(it) { CredentialManager.create(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenStrings.heading) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .padding(PADDING_XL)
                    .padding(bottom = MAIN_BUTTON_HEIGHT_WITH_SECONDARY)
                    .fillMaxSize(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PasskeyIcon()
                    Text(
                        text = screenStrings.useForPasskeys,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(SPACING_L))
                Text(
                    text = screenStrings.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            MainButton(
                text = appStrings.generic.openSystemSettings,
                onClick = {
                    val pendingIntent = credentialManager.createSettingsPendingIntent()
                    pendingIntent.send()
                },
                secondaryButton = SecondaryButton(
                    text = appStrings.generic.maybeLater,
                    onClick = onDismiss,
                ),
            )
        }
    }
}

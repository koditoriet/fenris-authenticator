package se.koditoriet.fenris.ui.screens

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.ui.components.MAIN_BUTTON_HEIGHT_WITH_SECONDARY
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.SecondaryButton
import se.koditoriet.fenris.ui.components.dialogs.LocalDialogHost
import se.koditoriet.fenris.ui.components.dialogs.showWarning
import se.koditoriet.fenris.ui.theme.PADDING_XL
import se.koditoriet.fenris.ui.theme.SPACING_L

private const val TAG = "EnablePasskeysScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSettingsNudgeScreen(
    title: String,
    heading: String,
    headingIcon: @Composable () -> Unit = { },
    description: String,
    allowDismiss: Boolean = true,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogHost = LocalDialogHost.current
    val screenStrings = appStrings.generic

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) }
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
                    headingIcon()
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(SPACING_L))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            MainButton(
                text = screenStrings.openSystemSettings,
                onClick = {
                    try {
                        onOpenSettings()
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to open settings", e)
                        dialogHost.showWarning(
                            title = screenStrings.unableToOpenSystemSettings,
                            text = screenStrings.openSystemSettingsManually,
                        )
                    }
                },
                secondaryButton = if (allowDismiss) {
                    SecondaryButton(
                        text = screenStrings.maybeLater,
                        onClick = onDismiss,
                    )
                } else {
                    null
                },
            )
        }
    }
}

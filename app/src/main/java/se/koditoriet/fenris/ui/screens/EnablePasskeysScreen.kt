package se.koditoriet.fenris.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.ui.components.PasskeyIcon

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun EnablePasskeysScreen(
    onDismiss: () -> Unit,
) {
    val screenStrings = appStrings.enablePasskeysScreen
    val credentialManager = LocalContext.current.let {
        remember(it) { CredentialManager.create(it) }
    }

    SystemSettingsNudgeScreen(
        title = screenStrings.heading,
        heading = screenStrings.useForPasskeys,
        description = screenStrings.description,
        headingIcon = { PasskeyIcon() },
        onOpenSettings = { credentialManager.createSettingsPendingIntent().send() },
        onDismiss = onDismiss,
    )
}

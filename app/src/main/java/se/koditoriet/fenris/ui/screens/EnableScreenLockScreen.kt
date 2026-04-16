package se.koditoriet.fenris.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import se.koditoriet.fenris.appStrings

@Composable
fun EnableScreenLockScreen() {
    val screenStrings = appStrings.enableScreenLockScreen
    val ctx = LocalContext.current

    SystemSettingsNudgeScreen(
        title = screenStrings.heading,
        heading = screenStrings.enableScreenLock,
        description = screenStrings.description,
        allowDismiss = false,
        headingIcon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
            )
        },
        onOpenSettings = { ctx.openSecuritySettings() },
        onDismiss = { },
    )
}

private fun Context.openSecuritySettings() {
    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
        putExtra(
            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }

    startActivity(intent)
}

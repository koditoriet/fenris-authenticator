package se.koditoriet.fenris.ui.components

import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import se.koditoriet.fenris.appStrings

@Composable
fun RequiresCamera(content: @Composable () -> Unit) {
    RequiresCapability(
        capability = PackageManager.FEATURE_CAMERA,
        capabilityRequiredMessage = appStrings.qrScanner.capabilityRequired,
    ) {
        RequiresPermission(
            permission = CAMERA,
            permissionsRequiredMessage = appStrings.qrScanner.permissionsRequired,
        ) {
            content()
        }
    }
}

package se.koditoriet.fenris.ui.components.backupseed

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.BadInputInformationDialog
import se.koditoriet.fenris.ui.components.QrScanner

private const val TAG = "SeedQRCodeInput"

@Composable
fun SeedQRCodeInput(
    onCancel: () -> Unit,
    onContinue: (BackupSeed) -> Unit,
) {
    val screenStrings = appStrings.seedInputScreen
    var invalidBackupSeedQR by remember { mutableStateOf(false) }

    BackHandler {
        onCancel()
    }

    QrScanner(
        onAbort = { onCancel() },
        onQrScanned = {
            if (!invalidBackupSeedQR) {
                // Don't interpret QR codes while the "invalid backup seed" dialog is active
                try {
                    onContinue(BackupSeed.fromUri(it.toUri()))
                } catch (e: Exception) {
                    invalidBackupSeedQR = true
                    Log.w(TAG, "Scanned QR code is not a valid backup seed", e)
                }
            }
        }
    )
    if (invalidBackupSeedQR) {
        BadInputInformationDialog(
            title = screenStrings.invalidSeedQRCode,
            text = screenStrings.invalidSeedQRCodeDescription,
            onDismiss = { invalidBackupSeedQR = false }
        )
    }
}

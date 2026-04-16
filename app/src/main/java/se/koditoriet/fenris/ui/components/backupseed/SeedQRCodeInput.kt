package se.koditoriet.fenris.ui.components.backupseed

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.QrScanner
import se.koditoriet.fenris.ui.components.dialogs.LocalDialogHost
import se.koditoriet.fenris.ui.components.dialogs.showBadInput

private const val TAG = "SeedQRCodeInput"

@Composable
fun SeedQRCodeInput(
    onCancel: () -> Unit,
    onContinue: (BackupSeed) -> Unit,
) {
    val screenStrings = appStrings.seedInputScreen
    val dialogHost = LocalDialogHost.current

    BackHandler {
        onCancel()
    }

    QrScanner(
        onAbort = { onCancel() },
        onQrScanned = {
            if (!dialogHost.visible) {
                // Don't interpret QR codes while the "invalid backup seed" dialog is active
                try {
                    onContinue(BackupSeed.fromUri(it.toUri()))
                } catch (e: Exception) {
                    dialogHost.showBadInput(
                        title = screenStrings.invalidSeedQRCode,
                        text = screenStrings.invalidSeedQRCodeDescription,
                    )
                    Log.w(TAG, "Scanned QR code is not a valid backup seed", e)
                }
            }
        }
    )
}

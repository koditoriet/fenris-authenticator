package se.koditoriet.fenris.ui.components.backupseed

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.MAIN_BUTTON_HEIGHT_WITH_SECONDARY
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.SecondaryButton
import se.koditoriet.fenris.ui.theme.BACKUP_SEED_QR_CODE_HEIGHT
import se.koditoriet.fenris.ui.theme.BACKUP_SEED_QR_CODE_WIDTH
import se.koditoriet.fenris.ui.theme.PADDING_M
import se.koditoriet.fenris.ui.theme.SPACING_XL

@Composable
fun BackupSeedDisplay(
    backupSeed: BackupSeed,
    text: String,
    confirmButtonText: String,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit
) {
    val screenStrings = appStrings.seedDisplayScreen
    val ctx = LocalContext.current
    val openPrintDialog = remember { mutableStateOf(false) }

    PrintQrWarningDialog(
        openPrintDialog = openPrintDialog,
        onConfirmation = {
            val printHelper = PrintHelper(ctx)
            printHelper.printBitmap("PrintBackupSeed", backupSeed.toBitmap())
            openPrintDialog.value = false
        }
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(PADDING_M)
                .padding(bottom = MAIN_BUTTON_HEIGHT_WITH_SECONDARY)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SPACING_XL),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SeedPhraseGrid(mnemonic = backupSeed.toMnemonic())
            Text(
                text = screenStrings.keepThemSafe,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        MainButton(
            text = confirmButtonText,
            onClick = onContinue,
            secondaryButton = SecondaryButton(
                text = screenStrings.printAsQr,
                onClick = { openPrintDialog.value = true },
            )
        )
    }
}

@Composable
private fun PrintQrWarningDialog(
    openPrintDialog: MutableState<Boolean>,
    onConfirmation: () -> Unit
) {
    when {
        openPrintDialog.value -> {
            val onDismissRequest = { openPrintDialog.value = false }
            AlertDialog(
                icon = {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                title = {
                    Text(text = appStrings.seedDisplayScreen.printAsQrTitle)
                },
                text = {
                    Text(text = appStrings.seedDisplayScreen.printAsQrWarning)
                },
                onDismissRequest = onDismissRequest,
                confirmButton = {
                    TextButton(onClick = onConfirmation) {
                        Text(appStrings.generic.next)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissRequest) {
                        Text(appStrings.generic.cancel)
                    }
                }
            )
        }
    }
}

private fun BackupSeed.toBitmap(): Bitmap {
    val bitMatrix = QRCodeWriter().encode(
        toUri().toString(),
        BarcodeFormat.QR_CODE,
        BACKUP_SEED_QR_CODE_WIDTH,
        BACKUP_SEED_QR_CODE_HEIGHT
    )

    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = createBitmap(width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }

    return bitmap
}

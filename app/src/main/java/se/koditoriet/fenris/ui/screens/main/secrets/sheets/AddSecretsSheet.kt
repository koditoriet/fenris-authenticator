package se.koditoriet.fenris.ui.screens.main.secrets.sheets

import BottomSheetAction
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.codec.QrCodeReader
import se.koditoriet.fenris.ui.components.sheet.BottomSheetGlobalHeader
import se.koditoriet.fenris.ui.supportedImageMimeTypes
import se.koditoriet.fenris.vault.NewTotpSecret

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSecretsSheet(
    enableFileImport: Boolean,
    onAddSecretByQR: () -> Unit,
    onAddSecret: (NewTotpSecret?) -> Unit,
    onImportFile: () -> Unit,
) {
    val screenStrings = appStrings.secretsScreen
    val ctx = LocalContext.current
    val hasCamera = remember {
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    val importImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            it?.run {
                val bitmap = ctx.contentResolver.openInputStream(it).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                val qrCodeReader = QrCodeReader()
                qrCodeReader.tryScanBitmap(bitmap)?.let { uri ->
                    onAddSecret(NewTotpSecret.fromUri(uri))
                }
            }
        }
    )

    BottomSheetGlobalHeader(
        heading = screenStrings.addSecretSheetHeading,
        details = screenStrings.addSecretSheetDescription,
    )

    // Add via QR scan (camera)
    if (hasCamera) {
        BottomSheetAction(
            icon = Icons.Default.QrCodeScanner,
            text = screenStrings.addSecretSheetScanQrCode,
            onClick = onAddSecretByQR,
        )
    }

    // Add via QR scan (image file)
    BottomSheetAction(
        icon = Icons.Default.Image,
        text = screenStrings.addSecretSheetScanImage,
        onClick = { importImageLauncher.launch(supportedImageMimeTypes) },
    )

    // Add via file import
    if (enableFileImport) {
        BottomSheetAction(
            icon = Icons.Default.FileDownload,
            text = screenStrings.addSecretSheetImportFile,
            onClick = onImportFile, // In the future, we'll want to add support for selection of file format
        )
    }

    // Add manually
    BottomSheetAction(
        icon = Icons.Default.Edit,
        text = screenStrings.addSecretSheetEnterManually,
        onClick = { onAddSecret(null) },
    )
}

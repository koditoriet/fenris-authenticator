package se.koditoriet.fenris.ui.screens.main.secrets.sheets

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.importformat.ImportFormatDecoder
import se.koditoriet.fenris.ui.components.WarningInformationDialog
import se.koditoriet.fenris.ui.components.sheet.BottomSheetAction
import se.koditoriet.fenris.ui.components.sheet.BottomSheetGlobalHeader

private const val TAG = "ImportFromFileSheet"

@Composable
fun ImportFromFileSheet(
    onImportFromFile: (ImportFormatDecoder.DecodedImport) -> Unit,
) {
    val sheetStrings = appStrings.imports
    val ctx = LocalContext.current
    var selectedDecoder by remember { mutableStateOf<ImportFormatDecoder?>(null) }
    var importFailed by remember { mutableStateOf(false) }

    BottomSheetGlobalHeader(
        heading = sheetStrings.importFrom,
        details = sheetStrings.importFromDescription,
    )

    val importFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            it?.run {
                val bytes = ctx.contentResolver.openInputStream(this).use { stream ->
                    stream!!.readBytes()
                }
                selectedDecoder?.run {
                    try {
                        onImportFromFile(decode(bytes))
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to decode import data", e)
                        importFailed = true
                    }
                } ?: Log.e(TAG, "Selected decoder disappeared while opening file")
            }
        }
    )

    LazyColumn {
        ImportFormatDecoder.decoders.forEach {
            item {
                BottomSheetAction(
                    icon = Icons.Default.FileDownload,
                    text = it.formatName,
                    onClick = {
                        Log.d(TAG, "Opening launcher to select file for import using ${it.javaClass.simpleName}")
                        selectedDecoder = it
                        importFile.launch(it.formatMimeTypes.toTypedArray())
                    },
                )
            }
        }
    }

    if (importFailed) {
        WarningInformationDialog(
            title = sheetStrings.importFailed,
            text = sheetStrings.importFailedInvalidFormat(selectedDecoder!!.formatName),
            onDismiss = { importFailed = false },
        )
    }
}

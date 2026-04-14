package se.koditoriet.fenris.ui.screens.main.secrets.sheets

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
    onFileImported: (ImportFormatDecoder.DecodedImport) -> Unit,
) {
    val sheetStrings = appStrings.imports
    val ctx = LocalContext.current
    var selectedDecoderIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var importFailed by rememberSaveable { mutableStateOf(false) }

    BottomSheetGlobalHeader(
        heading = sheetStrings.importFrom,
        details = sheetStrings.importFromDescription,
    )

    val importFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            it?.run {
                Log.d(TAG, "Importing from $it")
                try {
                    // throw an NPE instead of null checking to make sure all error handling goes through the catch
                    val selectedDecoder = ImportFormatDecoder.decoders[selectedDecoderIndex!!]
                    Log.d(TAG, "Using decoder ${selectedDecoder.formatName}")
                    val decodedImport = importFromFile(ctx, selectedDecoder)
                    onFileImported(decodedImport)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import from $it using decoder $selectedDecoderIndex", e)
                    importFailed = true
                }
            }
        }
    )

    LazyColumn {
        ImportFormatDecoder.decoders.forEachIndexed { index, decoder ->
            item {
                BottomSheetAction(
                    icon = Icons.Default.FileDownload,
                    text = decoder.formatName,
                    onClick = {
                        Log.d(TAG, "Opening launcher to select file for import using ${decoder.javaClass.simpleName}")
                        selectedDecoderIndex = index
                        importFile.launch(decoder.formatMimeTypes.toTypedArray())
                    },
                )
            }
        }
    }

    if (importFailed) {
        val selectedDecoderName = selectedDecoderIndex?.let { ImportFormatDecoder.decoders[it].formatName } ?: "???"
        WarningInformationDialog(
            title = sheetStrings.importFailed,
            text = sheetStrings.importFailedInvalidFormat(selectedDecoderName),
            onDismiss = { importFailed = false },
        )
    }
}

private fun Uri.importFromFile(
    ctx: Context,
    selectedDecoder: ImportFormatDecoder,
): ImportFormatDecoder.DecodedImport {
    val bytes = ctx.contentResolver.openInputStream(this)?.use { stream ->
        stream.readBytes()
    } ?: throw IllegalStateException("unable to open input stream")
    return selectedDecoder.decode(bytes)
}

package se.koditoriet.fenris.ui.screens.main.secrets.sheets

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.importformat.ImportFormatDecoder
import se.koditoriet.fenris.ui.components.MAIN_BUTTON_HEIGHT
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.sheet.BottomSheetGlobalHeader
import se.koditoriet.fenris.ui.components.sheet.BottomSheetSwitch
import se.koditoriet.fenris.vault.NewPasskey
import se.koditoriet.fenris.vault.NewTotpSecret

@Composable
fun ConfirmImportSheet(
    decodedImport: ImportFormatDecoder.DecodedImport,
    onConfirmImport: (Set<NewTotpSecret>, Set<NewPasskey>) -> Unit,
    onTouchImportList: (Boolean) -> Unit,
) {
    val sheetStrings = appStrings.imports
    BottomSheetGlobalHeader(
        heading = sheetStrings.confirmImport,
        details = sheetStrings.confirmImportDescription,
    )

    val scope = rememberCoroutineScope()
    val passkeys = remember { mutableStateSetOf(*decodedImport.passkeys.toTypedArray()) }
    val totpSecrets = remember { mutableStateSetOf(*decodedImport.totpSecrets.toTypedArray()) }

    Box {
        LazyColumn(
            modifier = Modifier
                .padding(bottom = MAIN_BUTTON_HEIGHT * 1.5f)
                .pointerInput(Unit) {
                    // Horrible, horrible hack: to ensure that the user doesn't accidentally close the bottom sheet
                    // while scrolling through the list of items to import, we disable swipe dismissal as soon as
                    // the user touches the list. Then, when the user lets go, we wait for a few milliseconds
                    // and then re-enable swipe dismissal.
                    // The delay is necessary because we don't have the opportunity to veto a dismissal until AFTER
                    // the user actually stops touching the screen.
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Final)
                        onTouchImportList(true)

                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Final)
                        } while (event.changes.any { it.pressed })

                        scope.launch {
                            delay(250)
                            onTouchImportList(false)
                        }
                    }
                },
        ) {
            decodedImport.totpSecrets.forEach { secret ->
                item {
                    BottomSheetSwitch(
                        text = formatTotpMetadata(secret.metadata),
                        checked = secret in totpSecrets,
                        verticalPadding = 0.dp,
                        onCheckedChange = {
                            if (it) {
                                totpSecrets.add(secret)
                            } else {
                                totpSecrets.remove(secret)
                            }
                        },
                    )
                }
            }
        }

        MainButton(
            text = sheetStrings.confirmImport,
            enabled = totpSecrets.isNotEmpty() || passkeys.isNotEmpty(),
            onClick = { onConfirmImport(totpSecrets, passkeys) },
        )
    }
}

private fun formatTotpMetadata(metadata: NewTotpSecret.Metadata): String {
    if (metadata.account == null) {
        return metadata.issuer
    }
    if (metadata.issuer == "") {
        return metadata.account
    }
    return "${metadata.issuer} (${metadata.account})"
}

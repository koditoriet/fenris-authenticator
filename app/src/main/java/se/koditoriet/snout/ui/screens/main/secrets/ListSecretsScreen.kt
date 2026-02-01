package se.koditoriet.snout.ui.screens.main.secrets

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import se.koditoriet.snout.SortMode
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.ui.components.BadInputInformationDialog
import se.koditoriet.snout.ui.components.IrrevocableActionConfirmationDialog
import se.koditoriet.snout.ui.components.QrScanner
import se.koditoriet.snout.ui.components.listview.ListViewTopBar
import se.koditoriet.snout.ui.components.listview.ReorderableList
import se.koditoriet.snout.ui.components.sheet.BottomSheet
import se.koditoriet.snout.ui.screens.main.secrets.sheets.AddSecretsSheet
import se.koditoriet.snout.ui.screens.main.secrets.sheets.EditNewSecretSheet
import se.koditoriet.snout.ui.screens.main.secrets.sheets.EditSecretMetadataSheet
import se.koditoriet.snout.ui.screens.main.secrets.sheets.SecretActionsSheet
import se.koditoriet.snout.ui.theme.SPACING_L
import se.koditoriet.snout.vault.NewTotpSecret
import se.koditoriet.snout.vault.TotpSecret
import kotlin.time.Clock

private const val TAG = "ListSecretsScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ListSecretsScreen(
    secrets: List<TotpSecret>,
    sortMode: SortMode,
    enableDeveloperFeatures: Boolean,
    hideSecretsFromAccessibility: Boolean,
    getTotpCodes: suspend (TotpSecret) -> List<String>,
    onLockVault: () -> Unit,
    onSettings: () -> Unit,
    onAddSecret: (NewTotpSecret) -> Unit,
    onImportFile: (Uri) -> Unit,
    onSortModeChange: (SortMode) -> Unit,
    onUpdateSecret: (TotpSecret) -> Unit,
    onDeleteSecret: (TotpSecret) -> Unit,
    onReindexSecrets: () -> Unit,
    clock: Clock = Clock.System,
) {
    val appStrings = LocalContext.current.let { remember(it) { it.appStrings } }
    val screenStrings = remember { appStrings.secretsScreen }
    var confirmDeleteSecret by remember { mutableStateOf<TotpSecret?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sheetViewState by remember { mutableStateOf<SheetViewState>(SheetViewState.Inactive) }
    var qrScannerState by remember { mutableStateOf<QRScannerState>(QRScannerState.Inactive) }
    var filter by remember { mutableStateOf<String?>(null) }
    val listItemEnvironment = ListItemEnvironment.remember()

    val secretListItems = secrets.map { secret ->
        TotpSecretListItem(
            totpSecret = secret,
            hideSecretsFromAccessibility = hideSecretsFromAccessibility,
            clock = clock,
            environment = listItemEnvironment,
            getTotpCodes = getTotpCodes,
            onUpdateSecret = onUpdateSecret,
            onLongClickSecret = { sheetViewState = SheetViewState.SecretActions(it) },
        )
    }

    Scaffold(
        topBar = {
            ListViewTopBar(
                title = appStrings.generic.appName,
                sortMode = sortMode,
                onSortModeChange = onSortModeChange,
                filterEnabled = filter != null,
                onFilterToggle = { filter = if (filter == null) "" else null },
            ) {
                IconButton(onClick = onLockVault) {
                    Icon(Icons.Filled.LockOpen, screenStrings.lockScreen)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, screenStrings.settings)
                }
            }
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(SPACING_L)) {
                FloatingActionButton(onClick = { sheetViewState = SheetViewState.AddSecretActions }) {
                    Icon(Icons.Filled.Add, screenStrings.addSecret)
                }
            }
        }
    ) { padding ->
        val comparator = compareBy<TotpSecretListItem> { it.totpSecret.issuer.lowercase() }
            .thenBy { it.totpSecret.account?.lowercase() }

        ReorderableList(
            padding = padding,
            filter = filter,
            items = secretListItems,
            selectedItem = (sheetViewState as? SheetViewState.SecretActions)?.selectedItem,
            sortMode = sortMode,
            alphabeticItemComparator = comparator,
            filterPlaceholderText = screenStrings.filterPlaceholder,
            onFilterChange = { filter = it },
            onReindexItems = onReindexSecrets,
        )

        confirmDeleteSecret?.let { secret ->
            IrrevocableActionConfirmationDialog(
                text = screenStrings.actionsSheetDeleteWarning,
                buttonText = screenStrings.actionsSheetDelete,
                onCancel = { confirmDeleteSecret = null },
                onConfirm = {
                    confirmDeleteSecret = null
                    sheetViewState = SheetViewState.Inactive
                    onDeleteSecret(secret)
                }
            )
        }

        if (sheetViewState != SheetViewState.Inactive) {
            ListSecretsBottomSheet(
                viewState = sheetViewState,
                sheetState = sheetState,
                padding = padding,
                hideSecretsFromAccessibility = hideSecretsFromAccessibility,
                enableDeveloperFeatures = enableDeveloperFeatures,
                onImportFile = onImportFile,
                onAddSecret = onAddSecret,
                onUpdateSecret = onUpdateSecret,
                onDeleteSecret = { confirmDeleteSecret = it },
                onChangeSheetViewState = { sheetViewState = it },
                onInitiateQRCodeScan = { qrScannerState = QRScannerState.ScanTOTPSecret }
            )
        }
    }
    when (qrScannerState) {
        QRScannerState.Inactive -> { /* NOP! */ }
        QRScannerState.ScanTOTPSecret -> {
            var invalidTotpQRCode by remember { mutableStateOf(false) }
            if (invalidTotpQRCode) {
                BadInputInformationDialog(
                    title = screenStrings.invalidTotpQRCode,
                    text = screenStrings.invalidTotpQRCodeDescription,
                    onDismiss = { invalidTotpQRCode = false }
                )
            }

            BackHandler {
                qrScannerState = QRScannerState.Inactive
            }
            QrScanner(
                onQrScanned = {
                    try {
                        val secret = NewTotpSecret.fromUri(it)
                        qrScannerState = QRScannerState.Inactive
                        sheetViewState = SheetViewState.AddingNewSecret(secret)
                    } catch (e: Exception) {
                        invalidTotpQRCode = true
                        Log.w(TAG, "Scanned non-TOTP secret QR code", e)
                    }
                },
                onAbort = { qrScannerState = QRScannerState.Inactive }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListSecretsBottomSheet(
    viewState: SheetViewState,
    sheetState: SheetState,
    padding: PaddingValues,
    hideSecretsFromAccessibility: Boolean,
    enableDeveloperFeatures: Boolean,
    onImportFile: (Uri) -> Unit,
    onAddSecret: (NewTotpSecret) -> Unit,
    onUpdateSecret: (TotpSecret) -> Unit,
    onDeleteSecret: (TotpSecret) -> Unit,
    onInitiateQRCodeScan: () -> Unit,
    onChangeSheetViewState: (SheetViewState) -> Unit,
) {
    BottomSheet(
        hideSheet = { onChangeSheetViewState(SheetViewState.Inactive) },
        sheetState = sheetState,
        sheetViewState = viewState,
        padding = padding,
    ) { state ->
        when (state) {
            SheetViewState.Inactive -> { /* NOP! */ }
            SheetViewState.AddSecretActions -> {
                AddSecretsSheet(
                    enableFileImport = enableDeveloperFeatures,
                    onAddSecretByQR = { onInitiateQRCodeScan() },
                    onAddSecret = {
                        onChangeSheetViewState(SheetViewState.AddingNewSecret(it))
                    },
                    onImportFile = {
                        onChangeSheetViewState(SheetViewState.Inactive)
                        onImportFile(it)
                    }
                )
            }

            is SheetViewState.SecretActions -> {
                SecretActionsSheet(
                    totpSecret = state.selectedItem.totpSecret,
                    onEditMetadata = {
                        onChangeSheetViewState(
                            SheetViewState.EditSecretMetadata(state.selectedItem.update(it))
                        )
                    },
                    onDeleteSecret = {
                        onDeleteSecret(state.selectedItem.totpSecret)
                    },
                )
            }

            is SheetViewState.EditSecretMetadata -> {
                BackHandler {
                    onChangeSheetViewState(SheetViewState.SecretActions(state.selectedItem))
                }
                EditSecretMetadataSheet(
                    metadata = NewTotpSecret.Metadata(
                        issuer = state.selectedItem.totpSecret.issuer,
                        account = state.selectedItem.totpSecret.account
                    ),
                    onSave = { newMetadata ->
                        val updatedSecret = state.selectedItem.totpSecret.copy(
                            issuer = newMetadata.issuer,
                            account = newMetadata.account
                        )
                        onUpdateSecret(updatedSecret)
                        onChangeSheetViewState(SheetViewState.Inactive)
                    },
                )
            }

            is SheetViewState.AddingNewSecret -> {
                BackHandler {
                    onChangeSheetViewState(SheetViewState.AddSecretActions)
                }
                EditNewSecretSheet(
                    prefilledSecret = state.prefilledSecret,
                    hideSecretsFromAccessibility = hideSecretsFromAccessibility,
                    onSave = { newSecret ->
                        onAddSecret(newSecret)
                        onChangeSheetViewState(SheetViewState.Inactive)
                    },
                )
            }
        }
    }
}

private sealed interface SheetViewState {
    object Inactive : SheetViewState
    object AddSecretActions : SheetViewState
    data class SecretActions(val selectedItem: TotpSecretListItem) : SheetViewState
    data class EditSecretMetadata(val selectedItem: TotpSecretListItem) : SheetViewState
    data class AddingNewSecret(val prefilledSecret: NewTotpSecret?) : SheetViewState
}

private sealed interface QRScannerState {
    object Inactive : QRScannerState
    object ScanTOTPSecret : QRScannerState
}

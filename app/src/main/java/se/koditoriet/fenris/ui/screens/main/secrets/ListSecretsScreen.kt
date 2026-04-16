package se.koditoriet.fenris.ui.screens.main.secrets

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import se.koditoriet.fenris.AppStrings
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.codec.QRCodeData
import se.koditoriet.fenris.crypto.AuthenticatorFactory
import se.koditoriet.fenris.importformat.GoogleAuthenticatorDecoder
import se.koditoriet.fenris.importformat.ImportFormatDecoder
import se.koditoriet.fenris.ui.components.LocalLoadingOverlay
import se.koditoriet.fenris.ui.components.QrScanner
import se.koditoriet.fenris.ui.components.dialogs.LocalDialogHost
import se.koditoriet.fenris.ui.components.dialogs.showBadInput
import se.koditoriet.fenris.ui.components.dialogs.showIrrevocableActionConfirmation
import se.koditoriet.fenris.ui.components.dialogs.showWarning
import se.koditoriet.fenris.ui.components.listview.ListViewTopBar
import se.koditoriet.fenris.ui.components.listview.ReorderableList
import se.koditoriet.fenris.ui.components.sheet.BottomSheet
import se.koditoriet.fenris.ui.openUri
import se.koditoriet.fenris.ui.screens.main.secrets.SheetViewState.AddSecretActions
import se.koditoriet.fenris.ui.screens.main.secrets.SheetViewState.AddingNewSecret
import se.koditoriet.fenris.ui.screens.main.secrets.SheetViewState.ConfirmImport
import se.koditoriet.fenris.ui.screens.main.secrets.SheetViewState.EditSecretMetadata
import se.koditoriet.fenris.ui.screens.main.secrets.SheetViewState.ImportFromFile
import se.koditoriet.fenris.ui.screens.main.secrets.SheetViewState.Inactive
import se.koditoriet.fenris.ui.screens.main.secrets.SheetViewState.SecretActions
import se.koditoriet.fenris.ui.screens.main.secrets.sheets.AddSecretsSheet
import se.koditoriet.fenris.ui.screens.main.secrets.sheets.ConfirmImportSheet
import se.koditoriet.fenris.ui.screens.main.secrets.sheets.EditNewSecretSheet
import se.koditoriet.fenris.ui.screens.main.secrets.sheets.EditSecretMetadataSheet
import se.koditoriet.fenris.ui.screens.main.secrets.sheets.ImportFromFileSheet
import se.koditoriet.fenris.ui.screens.main.secrets.sheets.SecretActionsSheet
import se.koditoriet.fenris.ui.theme.SPACING_L
import se.koditoriet.fenris.vault.NewPasskey
import se.koditoriet.fenris.vault.NewTotpSecret
import se.koditoriet.fenris.vault.NewTotpSecret.Metadata
import se.koditoriet.fenris.vault.TotpSecret
import se.koditoriet.fenris.viewmodel.ListSecretsViewModel
import kotlin.time.Clock

private const val TAG = "ListSecretsScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ListSecretsScreen(
    authFactory: AuthenticatorFactory,
    onSettings: () -> Unit,
    clock: Clock = Clock.System,
) {
    val loadingOverlay = LocalLoadingOverlay.current
    val ctx = LocalContext.current
    val viewModel = viewModel<ListSecretsViewModel>()
    val config by viewModel.config.collectAsState()
    val secrets by viewModel.secrets.collectAsState()
    val screenStrings = remember { viewModel.appStrings.secretsScreen }
    val listItemEnvironment = ListItemEnvironment.remember()
    val dialogHost = LocalDialogHost.current

    var sheetViewState by rememberSerializable { mutableStateOf<SheetViewState>(Inactive) }
    var qrScannerState by rememberSerializable { mutableStateOf(QRScannerState.Inactive) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetSwipeDismissable by rememberSaveable { mutableStateOf(true) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || sheetSwipeDismissable },
    )

    val secretListItems = secrets.map { secret ->
        TotpSecretListItem(
            totpSecret = secret,
            hideSecretsFromAccessibility = config.hideSecretsFromAccessibility,
            clock = clock,
            environment = listItemEnvironment,
            getTotpCodes = { viewModel.getTotpCodes(authFactory, it) },
            onUpdateSecret = viewModel::onUpdateSecret,
            onLongClickSecret = { sheetViewState = SecretActions(it.totpSecret.id) },
        )
    }

    Scaffold(
        topBar = {
            ListViewTopBar(
                title = viewModel.appStrings.generic.appName,
                sortMode = config.totpSecretSortMode,
                onSortModeChange = viewModel::onSortModeChange,
                filterEnabled = filter != null,
                onFilterToggle = { filter = if (filter == null) "" else null },
            ) {
                IconButton(onClick = viewModel::onLockVault) {
                    Icon(Icons.Filled.LockOpen, screenStrings.lockScreen)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, screenStrings.settings)
                }
            }
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(SPACING_L)) {
                FloatingActionButton(onClick = { qrScannerState = QRScannerState.ScanAnySupported }) {
                    Icon(Icons.Filled.QrCodeScanner, screenStrings.scanQRCode)
                }
                FloatingActionButton(onClick = { sheetViewState = AddSecretActions }) {
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
            selectedItemKey = (sheetViewState as? SecretActions)?.selectedItemId?.toString(),
            sortMode = config.totpSecretSortMode,
            alphabeticItemComparator = comparator,
            filterPlaceholderText = screenStrings.filterPlaceholder,
            onFilterChange = { filter = it },
            onReindexItems = viewModel::onReindexSecrets,
        )

        if (sheetViewState != Inactive) {
            val scope = LocalLifecycleOwner.current.lifecycleScope
            ListSecretsBottomSheet(
                secretListItems = secretListItems,
                viewState = sheetViewState,
                sheetState = sheetState,
                hideSecretsFromAccessibility = config.hideSecretsFromAccessibility,
                onAddSecret = viewModel::onAddSecret,
                onAddSecretFailed = {
                    dialogHost.showWarning(
                        title = ctx.appStrings.imports.importFailed,
                        text = ctx.appStrings.imports.numFailedImports(1),
                    )
                },
                onUpdateSecret = viewModel::onUpdateSecret,
                onDeleteSecret = {
                    dialogHost.showIrrevocableActionConfirmation(
                        text = screenStrings.actionsSheetDeleteWarning,
                        buttonText = screenStrings.actionsSheetDelete,
                        onConfirm = {
                            sheetViewState = Inactive
                            viewModel.onDeleteSecret(it)
                        }
                    )
                },
                onChangeSheetViewState = {
                    sheetSwipeDismissable = true
                    sheetViewState = it
                },
                onChangeSheetSwipeDismissable = { sheetSwipeDismissable = it },
                onInitiateQRCodeScan = { qrScannerState = QRScannerState.ScanTOTPSecret },
                onImportCredentials = { secrets, passkeys ->
                    loadingOverlay.show(scope, ctx.appStrings.imports.importingCredentials)
                    viewModel.onImportCredentials(
                        secrets, passkeys,
                        onSuccess = {
                            loadingOverlay.done(
                                scope = scope,
                                ctx.appStrings.generic.ok,
                                ctx.appStrings.imports.importFinished,
                            )
                        },
                        onFailure = {
                            dialogHost.showWarning(
                                title = ctx.appStrings.imports.importFailed,
                                text = ctx.appStrings.imports.numFailedImports(it.size),
                            )
                        },
                    )
                },
            )
        }
    }

    when (qrScannerState) {
        QRScannerState.Inactive -> { /* NOP! */ }

        QRScannerState.ScanTOTPSecret -> {
            TOTPCodeQRScanner(
                screenStrings = screenStrings,
                closeScanner = { qrScannerState = QRScannerState.Inactive },
                onSuccess = { sheetViewState = AddingNewSecret(it) }
            )
        }

        QRScannerState.ScanAnySupported -> {
            AnySupportedQRScanner(
                screenStrings = screenStrings,
                closeScanner = { qrScannerState = QRScannerState.Inactive },
                onSuccess = {
                    when (it) {
                        is QRCodeData.TotpSecret -> { sheetViewState = AddingNewSecret(it.newTotpSecret) }
                        is QRCodeData.FidoRequest -> { ctx.openUri(it.request) }
                        is QRCodeData.GoogleAuthenticatorExport -> {
                            val decodedImport = GoogleAuthenticatorDecoder.decodeProtobufPayload(it.protobufPayload)
                            sheetViewState = ConfirmImport(decodedImport)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListSecretsBottomSheet(
    secretListItems: List<TotpSecretListItem>,
    viewState: SheetViewState,
    sheetState: SheetState,
    hideSecretsFromAccessibility: Boolean,
    onAddSecret: (NewTotpSecret) -> Unit,
    onAddSecretFailed: () -> Unit,
    onUpdateSecret: (TotpSecret) -> Unit,
    onDeleteSecret: (TotpSecret.Id) -> Unit,
    onInitiateQRCodeScan: () -> Unit,
    onImportCredentials: (Set<NewTotpSecret>, Set<NewPasskey>) -> Unit,
    onChangeSheetViewState: (SheetViewState) -> Unit,
    onChangeSheetSwipeDismissable: (Boolean) -> Unit,
) {
    BottomSheet(
        hideSheet = { onChangeSheetViewState(Inactive) },
        sheetState = sheetState,
        sheetViewState = viewState,
    ) { state ->
        when (state) {
            Inactive -> { /* NOP! */ }
            AddSecretActions -> {
                AddSecretsSheet(
                    onAddSecretByQR = { onInitiateQRCodeScan() },
                    onAddSecret = {
                        onChangeSheetViewState(AddingNewSecret(it))
                    },
                    onError = onAddSecretFailed,
                    onImportFile = {
                        onChangeSheetViewState(ImportFromFile)
                    },
                )
            }

            is SecretActions -> {
                val selectedItem = secretListItems.first { it.totpSecret.id == state.selectedItemId }
                SecretActionsSheet(
                    totpSecret = selectedItem.totpSecret,
                    onEditMetadata = {
                        onChangeSheetViewState(
                            EditSecretMetadata(it)
                        )
                    },
                    onDeleteSecret = {
                        onDeleteSecret(it)
                    },
                )
            }

            is EditSecretMetadata -> {
                BackHandler {
                    onChangeSheetViewState(SecretActions(state.selectedItemId))
                }
                val selectedItem = secretListItems.first { it.totpSecret.id == state.selectedItemId }.totpSecret
                EditSecretMetadataSheet(
                    metadata = Metadata(
                        issuer = selectedItem.issuer,
                        account = selectedItem.account
                    ),
                    onSave = { newMetadata ->
                        val updatedSecret = selectedItem.copy(
                            issuer = newMetadata.issuer,
                            account = newMetadata.account
                        )
                        onUpdateSecret(updatedSecret)
                        onChangeSheetViewState(Inactive)
                    },
                )
            }

            is AddingNewSecret -> {
                BackHandler {
                    onChangeSheetViewState(AddSecretActions)
                }
                EditNewSecretSheet(
                    prefilledSecret = state.prefilledSecret,
                    hideSecretsFromAccessibility = hideSecretsFromAccessibility,
                    onSave = { newSecret ->
                        onAddSecret(newSecret)
                        onChangeSheetViewState(Inactive)
                    },
                )
            }

            ImportFromFile -> {
                BackHandler {
                    onChangeSheetViewState(AddSecretActions)
                }
                ImportFromFileSheet(
                    onFileImported = { onChangeSheetViewState(ConfirmImport(it)) }
                )
            }

            is ConfirmImport -> {
                BackHandler {
                    onChangeSheetViewState(ImportFromFile)
                }
                ConfirmImportSheet(
                    decodedImport = state.decodedImport,
                    onTouchImportList = { onChangeSheetSwipeDismissable(!it) },
                    onConfirmImport = { secrets, passkeys ->
                        onChangeSheetViewState(Inactive)
                        onImportCredentials(secrets, passkeys)
                    }
                )
            }
        }
    }
}

@Composable
private fun TOTPCodeQRScanner(
    screenStrings: AppStrings.SecretsScreen,
    closeScanner: () -> Unit,
    onSuccess: (NewTotpSecret) -> Unit,
) {
    val dialogHost = LocalDialogHost.current

    BackHandler { closeScanner() }
    QrScanner(
        onQrScanned = {
            try {
                val secret = NewTotpSecret.fromUri(it)
                closeScanner()
                onSuccess(secret)
            } catch (e: Exception) {
                dialogHost.showBadInput(
                    title = screenStrings.invalidTotpQRCode,
                    text = screenStrings.invalidTotpQRCodeDescription,
                )
                Log.w(TAG, "Scanned non-TOTP secret QR code", e)
            }
        },
        onAbort = closeScanner
    )
}

@Composable
private fun AnySupportedQRScanner(
    screenStrings: AppStrings.SecretsScreen,
    closeScanner: () -> Unit,
    onSuccess: (QRCodeData) -> Unit,
) {
    val dialogHost = LocalDialogHost.current

    BackHandler { closeScanner() }
    QrScanner(
        onQrScanned = {
            try {
                val data = QRCodeData.parse(it)
                onSuccess(data)
                closeScanner()
            } catch (e: Exception) {
                dialogHost.showBadInput(
                    title = screenStrings.unsupportedQRCode,
                    text = screenStrings.unsupportedQRCodeDescription,
                )
                Log.w(TAG, "Scanned unsupported QR code", e)
            }
        },
        onAbort = closeScanner
    )
}

@Serializable
private sealed interface SheetViewState {
    @Serializable object Inactive : SheetViewState
    @Serializable object AddSecretActions : SheetViewState
    @Serializable object ImportFromFile : SheetViewState
    @Serializable data class SecretActions(val selectedItemId: TotpSecret.Id) : SheetViewState
    @Serializable data class EditSecretMetadata(val selectedItemId: TotpSecret.Id) : SheetViewState
    @Serializable data class AddingNewSecret(val prefilledSecret: NewTotpSecret?) : SheetViewState
    @Serializable data class ConfirmImport(val decodedImport: ImportFormatDecoder.DecodedImport) : SheetViewState
}

private enum class QRScannerState {
    Inactive,
    ScanTOTPSecret,
    ScanAnySupported,
}

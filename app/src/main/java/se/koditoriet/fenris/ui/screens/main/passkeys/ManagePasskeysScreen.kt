package se.koditoriet.fenris.ui.screens.main.passkeys

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import se.koditoriet.fenris.Config
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.ui.components.IrrevocableActionConfirmationDialog
import se.koditoriet.fenris.ui.components.listview.ListViewTopBar
import se.koditoriet.fenris.ui.components.listview.ReorderableList
import se.koditoriet.fenris.ui.components.sheet.BottomSheet
import se.koditoriet.fenris.ui.screens.main.passkeys.sheets.EditPasskeyNameSheet
import se.koditoriet.fenris.ui.screens.main.passkeys.sheets.PasskeyActionsSheet
import se.koditoriet.fenris.vault.Passkey
import se.koditoriet.fenris.viewmodel.ManagePasskeysViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePasskeysScreen() {
    val viewModel = viewModel<ManagePasskeysViewModel>()
    val passkeys by viewModel.passkeys.collectAsState(emptyList())
    val config by viewModel.config.collectAsState(Config.default)
    val screenStrings = remember { viewModel.appStrings.managePasskeysScreen }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var sheetViewState by remember { mutableStateOf<SheetViewState?>(null) }
    var confirmDeletePasskey by remember { mutableStateOf<Passkey?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val passkeyListItems = passkeys.map { passkey ->
        PasskeyListItem(
            passkey = passkey,
            onUpdatePasskey = viewModel::onUpdatePasskey,
            appStrings = viewModel.appStrings,
            onLongClickPasskey = { sheetViewState = SheetViewState.Actions(it) },
        )
    }

    Scaffold(
        topBar = {
            ListViewTopBar(
                title = screenStrings.heading,
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appStrings.generic.back,
                        )
                    }
                },
                sortMode = config.passkeySortMode,
                onSortModeChange = viewModel::onSortModeChange,
                filterEnabled = filter != null,
                onFilterToggle = { filter = if (filter == null) "" else null },
            )
        },
    ) { padding ->
        val comparator = compareBy<PasskeyListItem> { it.passkey.displayName.lowercase() }
            .thenBy { it.passkey.rpId.lowercase() }
            .thenBy { it.passkey.userName.lowercase() }

        ReorderableList(
            padding = padding,
            filter = filter,
            items = passkeyListItems,
            selectedItem = (sheetViewState as? SheetViewState.Actions)?.selectedItem,
            sortMode = config.passkeySortMode,
            alphabeticItemComparator = comparator,
            filterPlaceholderText = screenStrings.filterPlaceholder,
            onFilterChange = { filter = it },
            onReindexItems = viewModel::onReindexPasskeys,
        )

        confirmDeletePasskey?.let { passkey ->
            IrrevocableActionConfirmationDialog(
                text = screenStrings.actionsSheetDeleteWarning,
                buttonText = screenStrings.actionsSheetDelete,
                onCancel = { confirmDeletePasskey = null },
                onConfirm = {
                    confirmDeletePasskey = null
                    sheetViewState = null
                    viewModel.onDeletePasskey(passkey.credentialId)
                }
            )
        }

        sheetViewState?.let { viewState ->
            BottomSheet(
                hideSheet = { sheetViewState = null },
                sheetState = sheetState,
                sheetViewState = viewState,
                padding = padding,
            ) { viewState ->
                when (viewState) {
                    is SheetViewState.Actions -> {
                        PasskeyActionsSheet(
                            passkey = viewState.selectedItem.passkey,
                            onEditDisplayName = {
                                sheetViewState = SheetViewState.EditMetadata(viewState.selectedItem)
                            },
                            onDeletePasskey = {
                                confirmDeletePasskey = viewState.selectedItem.passkey
                            },
                        )
                    }
                    is SheetViewState.EditMetadata -> {
                        BackHandler {
                            sheetViewState = SheetViewState.Actions(viewState.selectedItem)
                        }
                        EditPasskeyNameSheet(
                            existingPasskey = viewState.selectedItem.passkey,
                            onSave = {
                                viewModel.onUpdatePasskey(viewState.selectedItem.passkey.copy(displayName = it))
                                sheetViewState = null
                            }
                        )
                    }
                }
            }
        }
    }
}

private sealed interface SheetViewState {
    class Actions(val selectedItem: PasskeyListItem) : SheetViewState
    class EditMetadata(val selectedItem: PasskeyListItem) : SheetViewState
}

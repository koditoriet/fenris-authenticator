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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.ui.components.dialogs.LocalDialogHost
import se.koditoriet.fenris.ui.components.dialogs.showIrrevocableActionConfirmation
import se.koditoriet.fenris.ui.components.listview.ListViewTopBar
import se.koditoriet.fenris.ui.components.listview.ReorderableList
import se.koditoriet.fenris.ui.components.sheet.BottomSheet
import se.koditoriet.fenris.ui.screens.main.passkeys.sheets.EditPasskeyNameSheet
import se.koditoriet.fenris.ui.screens.main.passkeys.sheets.PasskeyActionsSheet
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.viewmodel.ManagePasskeysViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePasskeysScreen() {
    val viewModel = viewModel<ManagePasskeysViewModel>()
    val passkeys by viewModel.passkeys.collectAsState()
    val config by viewModel.config.collectAsState()
    val screenStrings = remember { viewModel.appStrings.managePasskeysScreen }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val dialogHost = LocalDialogHost.current

    var sheetViewState by rememberSerializable { mutableStateOf<SheetViewState>(SheetViewState.None) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val passkeyListItems = passkeys.map { passkey ->
        PasskeyListItem(
            passkey = passkey,
            onUpdatePasskey = viewModel::onUpdatePasskey,
            appStrings = viewModel.appStrings,
            onLongClickPasskey = { sheetViewState = SheetViewState.Actions(it.passkey.credentialId) },
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
            selectedItemKey = (sheetViewState as? SheetViewState.Actions)?.selectedItemId?.string,
            sortMode = config.passkeySortMode,
            alphabeticItemComparator = comparator,
            filterPlaceholderText = screenStrings.filterPlaceholder,
            onFilterChange = { filter = it },
            onReindexItems = viewModel::onReindexPasskeys,
        )

        sheetViewState.takeIf { it != SheetViewState.None }?.let { viewState ->
            BottomSheet(
                hideSheet = { sheetViewState = SheetViewState.None },
                sheetState = sheetState,
                sheetViewState = viewState,
            ) { viewState ->
                when (viewState) {
                    SheetViewState.None -> {
                        /* unreachable */
                    }

                    is SheetViewState.Actions -> {
                        val selectedItem = passkeys.first { it.credentialId == viewState.selectedItemId }
                        PasskeyActionsSheet(
                            passkey = selectedItem,
                            onEditDisplayName = {
                                sheetViewState = SheetViewState.EditMetadata(viewState.selectedItemId)
                            },
                            onDeletePasskey = {
                                dialogHost.showIrrevocableActionConfirmation(
                                    text = screenStrings.actionsSheetDeleteWarning,
                                    buttonText = screenStrings.actionsSheetDelete,
                                    onConfirm = {
                                        sheetViewState = SheetViewState.None
                                        viewModel.onDeletePasskey(viewState.selectedItemId)
                                    }
                                )
                            },
                        )
                    }

                    is SheetViewState.EditMetadata -> {
                        BackHandler {
                            sheetViewState = SheetViewState.Actions(viewState.selectedItemId)
                        }
                        val selectedItem = passkeys.first { it.credentialId == viewState.selectedItemId }
                        EditPasskeyNameSheet(
                            existingPasskey = selectedItem,
                            onSave = {
                                viewModel.onUpdatePasskey(selectedItem.copy(displayName = it))
                                sheetViewState = SheetViewState.None
                            }
                        )
                    }
                }
            }
        }
    }
}

@Serializable
private sealed interface SheetViewState {
    @Serializable object None : SheetViewState
    @Serializable data class Actions(val selectedItemId: CredentialId) : SheetViewState
    @Serializable data class EditMetadata(val selectedItemId: CredentialId) : SheetViewState
}

package se.koditoriet.fenris.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.crypto.wordMap
import se.koditoriet.fenris.ui.components.BadInputInformationDialog
import se.koditoriet.fenris.ui.components.backupseed.BackupSeedDisplay
import se.koditoriet.fenris.ui.components.backupseed.SeedPhraseInput
import se.koditoriet.fenris.ui.components.backupseed.SeedQRCodeInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegenerateBackupSeedScreen(
    validateSeed: suspend (seed: BackupSeed) -> Boolean,
    onRekeyBackups: (oldSeed: BackupSeed, newSeed: BackupSeed) -> Unit,
) {
    val screenStrings = appStrings.regenerateBackupSeedScreen
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    var viewState by remember {
        mutableStateOf<RegenerateBackupSeedViewState>(RegenerateBackupSeedViewState.InputSeedPhrase)
    }
    var showBadSeedDialog by remember { mutableStateOf(false) }

    fun continueIfSeedIsValid(seed: BackupSeed) {
        lifecycleScope.launch {
            if (validateSeed(seed)) {
                viewState = RegenerateBackupSeedViewState.ShowNewSeed(seed)
            } else {
                seed.wipe()
                showBadSeedDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appStrings.generic.back,
                        )
                    }
                },
                title = { Text(screenStrings.heading) },
            )
        },
    ) { padding ->
        viewState.let { frozenViewState ->
            when (frozenViewState) {
                RegenerateBackupSeedViewState.InputSeedPhrase -> {
                    SeedPhraseInput(
                        confirmButtonText = appStrings.generic.next,
                        wordCount = BackupSeed.MNEMONIC_LENGTH_WORDS,
                        seedWords = wordMap.keys,
                        modifier = Modifier.padding(padding),
                        onScanQRClick = { viewState = RegenerateBackupSeedViewState.InputSeedQR },
                        onContinue = { continueIfSeedIsValid(it) },
                    )
                }

                RegenerateBackupSeedViewState.InputSeedQR -> {
                    SeedQRCodeInput(
                        onCancel = { viewState = RegenerateBackupSeedViewState.InputSeedPhrase },
                        onContinue = { continueIfSeedIsValid(it) },
                    )
                }

                is RegenerateBackupSeedViewState.ShowNewSeed -> {
                    val newSeed = remember { BackupSeed.generate() }
                    BackHandler { viewState = RegenerateBackupSeedViewState.InputSeedPhrase }
                    BackupSeedDisplay(
                        backupSeed = newSeed,
                        text = screenStrings.newSeedExplanation,
                        confirmButtonText = screenStrings.confirmNewSeed,
                        modifier = Modifier.padding(padding),
                        onContinue = { onRekeyBackups(frozenViewState.oldSeed, newSeed) },
                    )
                }
            }
        }

        if (showBadSeedDialog) {
            BadInputInformationDialog(
                title = appStrings.regenerateBackupSeedScreen.invalidSeedDialogTitle,
                text = appStrings.regenerateBackupSeedScreen.invalidSeedDialogText,
                onDismiss = { showBadSeedDialog = false },
            )
        }
    }
}

private sealed interface RegenerateBackupSeedViewState {
    object InputSeedPhrase : RegenerateBackupSeedViewState
    object InputSeedQR : RegenerateBackupSeedViewState
    class ShowNewSeed(val oldSeed: BackupSeed) : RegenerateBackupSeedViewState
}

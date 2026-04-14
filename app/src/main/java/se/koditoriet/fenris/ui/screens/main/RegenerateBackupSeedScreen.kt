package se.koditoriet.fenris.ui.screens.main

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.crypto.wordMap
import se.koditoriet.fenris.ui.components.BadInputInformationDialog
import se.koditoriet.fenris.ui.components.SuccessInformationDialog
import se.koditoriet.fenris.ui.components.WarningInformationDialog
import se.koditoriet.fenris.ui.components.backupseed.BackupSeedDisplay
import se.koditoriet.fenris.ui.components.backupseed.SeedPhraseInput
import se.koditoriet.fenris.ui.components.backupseed.SeedQRCodeInput
import se.koditoriet.fenris.viewmodel.RegenerateBackupSeedViewModel

private const val TAG = "RegenerateBackupSeedScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegenerateBackupSeedScreen(
    onClose: () -> Unit,
) {
    val viewModel = viewModel<RegenerateBackupSeedViewModel>()
    val screenStrings = appStrings.regenerateBackupSeedScreen
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope

    var viewState by rememberSerializable {
        mutableStateOf(RegenerateBackupSeedViewState.InputSeedPhrase)
    }
    var showBadSeedDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupReseedCompleteDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupReseedFailedDialog by rememberSaveable { mutableStateOf(false) }

    fun close() {
        viewModel.seedPhraseWords.forEachIndexed { index, _ -> viewModel.seedPhraseWords[index] = "" }
        onClose()
    }

    fun continueIfSeedIsValid(seed: BackupSeed) {
        lifecycleScope.launch(Dispatchers.Main) {
            Log.d(TAG, "Validating old backup seed")
            val success = withContext(Dispatchers.IO) { viewModel.validateSeed(seed) }
            if (success) {
                Log.d(TAG, "Current backup seed is valid, proceeding to generate a new one")
                viewModel.oldSeed = seed
                viewState = RegenerateBackupSeedViewState.ShowNewSeed
            } else {
                Log.w(TAG, "Backup seed is invalid!")
                seed.wipe()
                showBadSeedDialog = true
            }
        }
    }

    BackHandler {
        close()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { close() }) {
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
                        seedPhraseInputState = viewModel.seedPhraseWords,
                        seedWords = wordMap.keys,
                        modifier = Modifier.padding(padding),
                        onScanQRClick = {
                            Log.d(TAG, "Opening QR scanner to scan current backup seed")
                            viewState = RegenerateBackupSeedViewState.InputSeedQR
                        },
                        onContinue = { continueIfSeedIsValid(it) },
                    )
                }

                RegenerateBackupSeedViewState.InputSeedQR -> {
                    SeedQRCodeInput(
                        onCancel = {
                            Log.d(TAG, "Scan backup seed from QR canceled, going back to seed phrase input")
                            viewState = RegenerateBackupSeedViewState.InputSeedPhrase
                        },
                        onContinue = { continueIfSeedIsValid(it) },
                    )
                }

                RegenerateBackupSeedViewState.ShowNewSeed -> {
                    BackHandler {
                        Log.d(TAG, "Back pressed, backing out of new seed display screen")
                        viewState = RegenerateBackupSeedViewState.InputSeedPhrase
                    }
                    BackupSeedDisplay(
                        backupSeed = viewModel.newSeed,
                        text = screenStrings.newSeedExplanation,
                        confirmButtonText = screenStrings.confirmNewSeed,
                        modifier = Modifier.padding(padding),
                        onContinue = {
                            val oldSeed = viewModel.oldSeed
                            check(oldSeed != null) { "old seed was null - impossible!" }
                            lifecycleScope.launch(Dispatchers.Main) {
                                showBackupReseedCompleteDialog = withContext(Dispatchers.IO) {
                                    Log.d(TAG, "Rekeying backups")
                                    viewModel.rekeyBackups(oldSeed, viewModel.newSeed)
                                }
                                Log.d(TAG, "Wiping temporary seeds")
                                oldSeed.wipe()
                                viewModel.newSeed.wipe()
                            }
                        },
                    )
                }
            }
        }

        if (showBackupReseedCompleteDialog) {
            SuccessInformationDialog(
                title = appStrings.regenerateBackupSeedScreen.successDialogTitle,
                text = appStrings.regenerateBackupSeedScreen.successDialogText,
                onDismiss = { close() },
            )
        }
        if (showBackupReseedFailedDialog) {
            WarningInformationDialog(
                title = appStrings.regenerateBackupSeedScreen.failureDialogTitle,
                text = appStrings.regenerateBackupSeedScreen.failureDialogText,
                onDismiss = { showBackupReseedFailedDialog = false },
            )
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

private enum class RegenerateBackupSeedViewState {
    InputSeedPhrase,
    InputSeedQR,
    ShowNewSeed,
}

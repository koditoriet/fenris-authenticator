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
import se.koditoriet.fenris.ui.components.backupseed.BackupSeedDisplay
import se.koditoriet.fenris.ui.components.backupseed.SeedPhraseInput
import se.koditoriet.fenris.ui.components.backupseed.SeedQRCodeInput
import se.koditoriet.fenris.ui.components.dialogs.LocalDialogHost
import se.koditoriet.fenris.ui.components.dialogs.showBadInput
import se.koditoriet.fenris.ui.components.dialogs.showSuccess
import se.koditoriet.fenris.ui.components.dialogs.showWarning
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
    val dialogHost = LocalDialogHost.current

    var viewState by rememberSerializable {
        mutableStateOf(RegenerateBackupSeedViewState.InputSeedPhrase)
    }

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
                dialogHost.showBadInput(
                    title = screenStrings.invalidSeedDialogTitle,
                    text = screenStrings.invalidSeedDialogText,
                )

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
                                val success = withContext(Dispatchers.IO) {
                                    Log.d(TAG, "Rekeying backups")
                                    viewModel.rekeyBackups(oldSeed, viewModel.newSeed)
                                }

                                if (success) {
                                    dialogHost.showSuccess(
                                        title = screenStrings.successDialogTitle,
                                        text = screenStrings.successDialogText,
                                        onDismiss = { close() },
                                    )
                                } else {
                                    dialogHost.showWarning(
                                        title = screenStrings.failureDialogTitle,
                                        text = screenStrings.failureDialogText,
                                    )
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
    }
}

private enum class RegenerateBackupSeedViewState {
    InputSeedPhrase,
    InputSeedQR,
    ShowNewSeed,
}

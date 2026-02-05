package se.koditoriet.fenris.ui.screens.setup

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.crypto.wordMap
import se.koditoriet.fenris.ui.onIOThread
import se.koditoriet.fenris.vault.ImportFailedException
import se.koditoriet.fenris.viewmodel.FenrisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentActivity.SetupScreen() {
    val viewModel = viewModel<FenrisViewModel>()
    var viewState by remember { mutableStateOf<ViewState>(ViewState.InitialSetup) }
    var importProgress by remember { mutableStateOf(Pair(0, 0)) }

    BackHandler {
        viewState.previousViewState?.apply {
            viewState = this
        }
    }

    when (viewState) {
        ViewState.InitialSetup -> {
            InitialSetupScreen(
                onEnableBackups = { viewState = ViewState.ShowBackupSeed },
                onSkipBackups = onIOThread { viewModel.createVault(null) },
                onRestoreBackup = { viewState = ViewState.RestoreBackup }
            )
        }
        ViewState.ShowBackupSeed -> {
            val seed = remember { BackupSeed.generate() }
            BackupSeedScreen(
                backupSeed = seed,
                onContinue = onIOThread {
                    viewModel.createVault(seed)
                    seed.wipe()
                }
            )
        }
        ViewState.RestoreBackup -> {
            RestoreBackupScreen(
                seedWords = wordMap.keys,
                onRestore = onIOThread { backupSeed, uri ->
                    viewState = ViewState.RestoringBackup
                    try {
                        viewModel.restoreVaultFromBackup(
                            backupSeed = backupSeed,
                            uri = uri,
                            onSecretImported = { done, total -> importProgress = Pair(done, total) },
                        )
                    } catch (e: ImportFailedException) {
                        viewState = ViewState.RestoreBackupFailed(e)
                    } finally {
                        backupSeed.wipe()
                    }
                }
            )
        }
        ViewState.RestoringBackup -> {
            BackupRestoreProgressScreen(
                importedSecrets = importProgress.first,
                secretsToImport = importProgress.second,
            )
        }
        is ViewState.RestoreBackupFailed -> {
            RestoreBackupFailedScreen(
                onDismiss = { viewState = ViewState.InitialSetup }
            )
        }
    }
}

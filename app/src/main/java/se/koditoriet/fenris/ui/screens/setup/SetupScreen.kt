package se.koditoriet.fenris.ui.screens.setup

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.crypto.wordMap
import se.koditoriet.fenris.ui.components.LoadingOverlayImpl
import se.koditoriet.fenris.ui.components.LocalLoadingOverlay
import se.koditoriet.fenris.ui.onIOThread
import se.koditoriet.fenris.viewmodel.SetupViewModel

private const val TAG = "SetupScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentActivity.SetupScreen() {
    val viewModel = viewModel<SetupViewModel>()
    var viewState by remember { mutableStateOf<ViewState>(ViewState.InitialSetup) }

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
            val scope = LocalLifecycleOwner.current.lifecycleScope
            val loadingOverlay = LocalLoadingOverlay.current
            val screenStrings = appStrings.restoringBackupScreen
            RestoreBackupScreen(
                seedWords = wordMap.keys,
                onRestore = onIOThread { backupSeed, password, uri ->
                    loadingOverlay.show(screenStrings.restoredSecrets(0, 0))
                    try {
                        viewModel.restoreVaultFromBackup(
                            backupSeed = backupSeed,
                            backupPassword = password,
                            uri = uri,
                            onSecretImported = { done, total ->
                                loadingOverlay.update(scope, screenStrings.restoredSecrets(done, total))
                            },
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restore from backup", e)
                        withContext(Dispatchers.Main) {
                            loadingOverlay.hide()
                            viewState = ViewState.RestoreBackupFailed
                        }
                    } finally {
                        backupSeed.wipe()
                        loadingOverlay.done(scope, appStrings.generic.ok, screenStrings.backupRestored)
                    }
                }
            )
        }

        ViewState.RestoreBackupFailed -> {
            RestoreBackupFailedScreen(
                onDismiss = { viewState = ViewState.InitialSetup }
            )
        }
    }

    LoadingOverlayImpl.Render()
}

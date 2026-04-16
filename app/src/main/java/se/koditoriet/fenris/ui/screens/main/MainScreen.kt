package se.koditoriet.fenris.ui.screens.main

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import se.koditoriet.fenris.crypto.BiometricPromptAuthenticator
import se.koditoriet.fenris.ui.components.LoadingOverlayImpl
import se.koditoriet.fenris.ui.screens.main.passkeys.ManagePasskeysScreen
import se.koditoriet.fenris.ui.screens.main.secrets.ListSecretsScreen
import se.koditoriet.fenris.ui.screens.main.settings.RegenerateBackupSeedScreen
import se.koditoriet.fenris.ui.screens.main.settings.SettingsScreen
import se.koditoriet.fenris.viewmodel.MainScreenViewModel

private const val TAG = "MainScreen"

@Composable
fun FragmentActivity.MainScreen(
    credentialProviderEnabled: Boolean,
    viewModel: MainScreenViewModel,
) {
    val authFactory = remember { BiometricPromptAuthenticator.Factory(this) }
    var viewState: ViewState by rememberSerializable { mutableStateOf(ViewState.ListSecrets) }

    BackHandler {
        Log.d(TAG, "Back pressed on main screen")
        viewState.previousViewState?.apply {
            Log.d(TAG, "Going back to view '$this'")
            viewState = this
        } ?: lifecycleScope.launch {
            Log.d(TAG, "No view to go back to; locking vault and backgrounding")
            viewModel.lockVault()
            moveTaskToBack(true)
        }
    }

    when (viewState) {
        ViewState.ListSecrets -> {
            ListSecretsScreen(
                onSettings = { viewState = ViewState.Settings },
                authFactory = authFactory,
            )
        }
        ViewState.Settings -> {
            SettingsScreen(
                credentialProviderEnabled = credentialProviderEnabled,
                onRegenerateBackupSeed = { viewState = ViewState.RegenerateBackupSeed },
                onManagePasskeys = { viewState = ViewState.ManagePasskeys },
                authFactory = authFactory,
            )
        }
        ViewState.ManagePasskeys -> {
            ManagePasskeysScreen()
        }
        ViewState.RegenerateBackupSeed -> {
            RegenerateBackupSeedScreen(onClose = { viewState = ViewState.Settings })
        }
    }

    LoadingOverlayImpl.Render()
}

enum class ViewState(val previousViewState: ViewState?) {
    ListSecrets(null),
    Settings(ListSecrets),
    ManagePasskeys(Settings),
    RegenerateBackupSeed(Settings);
}

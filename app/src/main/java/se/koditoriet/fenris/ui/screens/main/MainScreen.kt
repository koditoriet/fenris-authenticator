package se.koditoriet.fenris.ui.screens.main

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import se.koditoriet.fenris.BiometricPromptAuthenticator
import se.koditoriet.fenris.ui.screens.main.passkeys.ManagePasskeysScreen
import se.koditoriet.fenris.ui.screens.main.secrets.ListSecretsScreen
import se.koditoriet.fenris.ui.screens.main.settings.SettingsScreen
import se.koditoriet.fenris.viewmodel.FenrisViewModel

private const val TAG = "MainScreen"

@Composable
fun FragmentActivity.MainScreen(
    credentialProviderEnabled: Boolean,
    viewModel: FenrisViewModel,
) {
    val authFactory = remember { BiometricPromptAuthenticator.Factory(this) }
    var viewState by remember { mutableStateOf<ViewState>(ViewState.ListSecrets) }

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
            RegenerateBackupSeedScreen()
        }
    }
}

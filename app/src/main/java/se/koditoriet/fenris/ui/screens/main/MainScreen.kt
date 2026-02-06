package se.koditoriet.fenris.ui.screens.main

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.koditoriet.fenris.BiometricPromptAuthenticator
import se.koditoriet.fenris.Config
import se.koditoriet.fenris.ui.ignoreAuthFailure
import se.koditoriet.fenris.ui.onIOThread
import se.koditoriet.fenris.ui.screens.main.passkeys.ManagePasskeysScreen
import se.koditoriet.fenris.ui.screens.main.secrets.ListSecretsScreen
import se.koditoriet.fenris.ui.screens.main.settings.SettingsScreen
import se.koditoriet.fenris.viewmodel.FenrisViewModel

private const val TAG = "MainScreen"

@Composable
fun FragmentActivity.MainScreen(
    credentialProviderEnabled: Boolean,
    config: Config,
    viewModel: FenrisViewModel,
) {
    val authFactory = remember { BiometricPromptAuthenticator.Factory(this) }
    val totpSecrets by viewModel.secrets.collectAsState(emptyList())
    val passkeys by viewModel.passkeys.collectAsState(emptyList())
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
                secrets = totpSecrets,
                sortMode = config.totpSecretSortMode,
                enableDeveloperFeatures = config.enableDeveloperFeatures,
                hideSecretsFromAccessibility = config.hideSecretsFromAccessibility,
                getTotpCodes = { secret -> viewModel.getTotpCodes(authFactory, secret, 2) },
                onLockVault = onIOThread { viewModel.lockVault() },
                onSettings = { viewState = ViewState.Settings },
                onAddSecret = onIOThread { secret -> viewModel.addTotpSecret(secret) },
                onSortModeChange = onIOThread { mode -> viewModel.setTotpSecretSortMode(mode) },
                onUpdateSecret = onIOThread { secret -> viewModel.updateTotpSecret(secret) },
                onDeleteSecret = onIOThread { secret -> viewModel.deleteTotpSecret(secret.id) },
                onImportFile = onIOThread { uri -> viewModel.importFromFile(uri) },
                onReindexSecrets = onIOThread { viewModel.reindexTotpSecrets() },
            )
        }
        ViewState.Settings -> {
            SettingsScreen(
                enableBackups = config.backupsEnabled,
                protectAccountList = config.protectAccountList,
                lockOnClose = config.lockOnClose,
                lockOnCloseGracePeriod = config.lockOnCloseGracePeriod,
                screenSecurityEnabled = config.screenSecurityEnabled,
                hideSecretsFromAccessibility = config.hideSecretsFromAccessibility,
                credentialProviderEnabled = credentialProviderEnabled,
                onDisableBackups = onIOThread(viewModel::disableBackups),
                onLockOnCloseChange = onIOThread(viewModel::setLockOnClose),
                onLockOnCloseGracePeriodChange = onIOThread(viewModel::setLockOnCloseGracePeriod),
                onProtectAccountListChange = onIOThread { it ->
                    ignoreAuthFailure {
                        viewModel.rekeyVault(authFactory, it)
                    }
                },
                onScreenSecurityEnabledChange = onIOThread(viewModel::setScreenSecurity),
                onHideSecretsFromAccessibilityChange = onIOThread(viewModel::setHideSecretsFromAccessibility),
                enableDeveloperFeatures = config.enableDeveloperFeatures,
                onEnableDeveloperFeaturesChange = onIOThread(viewModel::setEnableDeveloperFeatures),
                onWipeVault = onIOThread(viewModel::wipeVault),
                onExport = onIOThread(viewModel::exportVault),
                onManagePasskeys = { viewState = ViewState.ManagePasskeys },
                getSecurityReport = {
                    withContext(Dispatchers.IO) {
                        viewModel.getSecurityReport()
                    }
                },
            )
        }
        ViewState.ManagePasskeys -> {
            ManagePasskeysScreen(
                passkeys = passkeys,
                sortMode = config.passkeySortMode,
                onSortModeChange = onIOThread { it -> viewModel.setPasskeySortMode(it) },
                onUpdatePasskey = onIOThread { it -> viewModel.updatePasskey(it) },
                onDeletePasskey = onIOThread { it -> viewModel.deletePasskey(it.credentialId) },
                onReindexPasskeys = onIOThread { viewModel.reindexPasskeys() }
            )
        }
    }
}

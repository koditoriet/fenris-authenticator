package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.util.Log
import kotlinx.coroutines.flow.first
import se.koditoriet.fenris.FenrisApp
import se.koditoriet.fenris.crypto.AuthenticatorFactory

private const val TAG = "ActivityViewModel"

/**
 * Functionality shared among all activity level view models.
 */
abstract class ActivityViewModel(app: Application) : ViewModelBase(app) {
    suspend fun lockVault() = vault.lockVault()

    suspend fun unlockVault(authFactory: AuthenticatorFactory) {
        Log.i(TAG, "Attempting to unlock vault")
        val config = config.first()
        check(config.encryptedDbKey != null)
        val authenticator = authFactory.withReason(
            reason = appStrings.viewModel.authUnlockVault,
            subtitle = appStrings.viewModel.authUnlockVaultSubtitle,
        )
        vault.unlockVault(authenticator, config.encryptedDbKey, config.backupKeys?.toVaultBackupKeys())
        Log.i(TAG, "Vault unlocked")
    }
}

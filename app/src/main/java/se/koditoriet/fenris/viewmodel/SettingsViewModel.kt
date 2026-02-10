package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.koditoriet.fenris.crypto.AuthenticatorFactory
import se.koditoriet.fenris.crypto.KeySecurityLevel
import se.koditoriet.fenris.ui.ignoreAuthFailure

private const val TAG = "SettingsViewModel"

class SettingsViewModel(private val app: Application) : ViewModelBase(app) {
    fun onDisableBackups() = onIOThread {
        vault.withLock {
            Log.i(TAG, "Disabling backups")
            configDatastore.updateData {
                Log.d(TAG, "Wiping backup keys")
                it.copy(backupKeys = null)
            }
            Log.d(TAG, "Erasing encrypted backup secrets")
            eraseBackups()
        }
    }

    fun onLockOnCloseChange(enabled: Boolean, gracePeriod: Int) = updateConfig {
        it.copy(lockOnClose = enabled, lockOnCloseGracePeriod = gracePeriod)
    }

    fun onPrivacyLockChange(authFactory: AuthenticatorFactory, enabled: Boolean) = withVault {
        ignoreAuthFailure {
            Log.i(TAG, "Rekeying vault")
            val dbKey = authFactory.withReason(
                reason = appStrings.viewModel.authToggleBioprompt(enabled),
                subtitle = appStrings.viewModel.authToggleBiopromptSubtitle(enabled),
            ) {
                rekey(it, enabled)
            }
            configDatastore.updateData {
                it.copy(
                    encryptedDbKey = dbKey,
                    protectAccountList = enabled,
                )
            }
        }
    }

    fun onScreenSecurityEnabledChange(enabled: Boolean) = updateConfig {
        it.copy(screenSecurityEnabled = enabled)
    }

    fun onHideSecretsFromAccessibilityChange(enabled: Boolean) = updateConfig {
        it.copy(hideSecretsFromAccessibility = enabled)
    }

    fun onEnableDeveloperFeaturesChange(enabled: Boolean) = updateConfig {
        it.copy(enableDeveloperFeatures = enabled)
    }

    fun onWipeVault() = withVault {
        Log.i(TAG, "Wiping vault")
        wipe()
    }

    fun onExportVault(uri: Uri) = withVault {
        Log.i(TAG, "Exporting backup to $uri")
        app.contentResolver.openOutputStream(uri)!!.use { stream ->
            stream.write(export().encode().toByteArray())
        }
    }

    suspend fun getSecurityReport(): SecurityReport = withContext(Dispatchers.IO) {
        vault.withLock {
            getSecurityReport()
        }
    }
}

data class SecurityReport(
    val backupKeyStatus: KeySecurityLevel?,
    val secretsStatus: Map<KeySecurityLevel, Int>,
    val passkeysStatus: Map<KeySecurityLevel, Int>,
) {
    val totalSecrets: Int
        get() = secretsStatus.values.sum()

    val totalPasskeys: Int
        get() = passkeysStatus.values.sum()
}

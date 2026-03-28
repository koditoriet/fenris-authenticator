package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.first
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.vault.VaultExportEnvelope

private const val TAG = "SetupViewModel"

class SetupViewModel(private val app: Application) : ViewModelBase(app) {
    suspend fun createVault(backupSeed: BackupSeed?) = vault.withLock {
        Log.i(TAG, "Creating vault; enable backups: ${backupSeed != null}")
        val (dbKey, backupKey) = create(
            requiresAuthentication = config.first().protectAccountList,
            backupSeed = backupSeed,
        )
        configDatastore.updateData {
            it.copy(
                encryptedDbKey = dbKey,
                backupKeyAlias = backupKey?.alias,
            )
        }
    }

    suspend fun restoreVaultFromBackup(
        backupSeed: BackupSeed,
        backupPassword: String,
        uri: Uri,
        onSecretImported: (Int, Int) -> Unit = { _, _ -> },
    ): Unit = vault.withLock {
        Log.i(TAG, "Restoring vault from backup")
        try {
            val backupData = app.contentResolver.openInputStream(uri)!!.use { stream ->
                VaultExportEnvelope.decode(stream.readBytes())
            }

            Log.d(TAG, "Restoring vault backup")
            val (dbKey, backupKey) = create(
                requiresAuthentication = config.first().protectAccountList,
                backupSeed = backupSeed,
                backupPassword = backupPassword,
                backupData = backupData,
                onSecretImported = onSecretImported,
            )

            Log.d(TAG, "Updating config data")
            configDatastore.updateData {
                it.copy(
                    encryptedDbKey = dbKey,
                    backupKeyAlias = backupKey?.alias,
                )
            }
            Log.i(TAG, "Backup successfully imported")
        } catch (e: Exception) {
            // Make sure we go back to a clean slate if something went wrong
            Log.e(TAG, "Unable to restore backup", e)
            wipe()
            throw e
        }
    }
}

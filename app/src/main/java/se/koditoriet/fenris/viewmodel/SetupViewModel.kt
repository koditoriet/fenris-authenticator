package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.first
import se.koditoriet.fenris.BACKUP_SEED_MNEMONIC_LENGTH_WORDS
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.vault.VaultExportEnvelope

private const val TAG = "SetupViewModel"

class SetupViewModel(private val app: Application) : ViewModelBase(app) {
    suspend fun createVault(backupSeed: BackupSeed?) = vault.withLock {
        Log.i(TAG, "Creating vault; enable backups: ${backupSeed != null}")
        val (dbKey, backupKey) = create(
            // we have to use config.first(), because currentConfig() blocks until we have an initialized config
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
            // throw an NPE instead of null checking to make sure all error handling goes through the catch
            val backupData = app.contentResolver.openInputStream(uri)?.use { stream ->
                VaultExportEnvelope.decode(stream.readBytes())
            } ?: throw IllegalStateException("unable to open input stream")

            Log.d(TAG, "Restoring vault backup")
            val (dbKey, backupKey) = create(
                // we have to use config.first(), because currentConfig() blocks until we have an initialized config
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

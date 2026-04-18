package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import se.koditoriet.fenris.BACKUP_SEED_MNEMONIC_LENGTH_WORDS
import se.koditoriet.fenris.crypto.BackupSeed

private const val TAG = "RegenerateBackupSeedViewModel"

class RegenerateBackupSeedViewModel(app: Application) : ViewModelBase(app) {
    suspend fun validateSeed(seed: BackupSeed) = vault.withLock { validateSeed(seed) }

    suspend fun rekeyBackups(oldSeed: BackupSeed, newSeed: BackupSeed): Boolean = vault.withLock {
        try {
            rekeyBackups(oldSeed, newSeed)
            Log.e(TAG, "Backups successfully re-encrypted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-encrypt backups secrets", e)
            false
        }
    }
}

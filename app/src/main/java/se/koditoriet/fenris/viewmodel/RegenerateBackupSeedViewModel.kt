package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.util.Log
import se.koditoriet.fenris.crypto.BackupSeed

private const val TAG = "RegenerateBackupSeedViewModel"

class RegenerateBackupSeedViewModel(app: Application) : ViewModelBase(app) {
    suspend fun validateSeed(seed: BackupSeed) = vault.withLock { validateSeed(seed) }
    suspend fun rekeyBackups(oldSeed: BackupSeed, newSeed: BackupSeed): Boolean = vault.withLock {
        try {
            rekeyBackups(oldSeed, newSeed)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-encrypt backups secrets", e)
            false
        }
    }
}

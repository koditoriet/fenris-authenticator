package se.koditoriet.snout

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import se.koditoriet.snout.crypto.Cryptographer
import se.koditoriet.snout.repository.VaultRepository
import se.koditoriet.snout.vault.Vault

private const val TAG = "SnoutApp"

class SnoutApp : Application() {
    val cryptographer: Cryptographer
    val vault: Vault
    val config: DataStore<Config> by dataStore("config", ConfigSerializer)

    init {
        Log.i(TAG, "Loading sqlcipher native libraries")
        System.loadLibrary("sqlcipher")
        val repositoryFactory = { dbName: String, key: ByteArray ->
            VaultRepository.open(this, dbName, key)
        }

        Log.i(TAG, "Creating cryptographer")
        cryptographer = Cryptographer()

        Log.i(TAG, "Creating vault")
        vault = Vault(
            repositoryFactory = repositoryFactory,
            cryptographer = cryptographer,
            dbFile = lazy { getDatabasePath("vault")!! },
        )
    }
}

package se.koditoriet.fenris.vault

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import se.koditoriet.fenris.DbKey
import se.koditoriet.fenris.crypto.Authenticator
import se.koditoriet.fenris.crypto.types.EncryptionAlgorithm
import se.koditoriet.fenris.crypto.types.KeyHandle
import java.security.Signature

class SynchronizedVault(vaultFactory: () -> Vault) {
    private val mutex = Mutex()
    private val vault = vaultFactory()

    suspend fun <T> withLock(action: suspend Vault.() -> T): T {
        mutex.lock()
        try {
            return vault.action()
        } finally {
            mutex.unlock()
        }
    }

    suspend fun lockVault() = withLock { lock() }

    suspend fun unlockVault(authenticator: Authenticator, dbKey: DbKey, backupKey: KeyHandle<EncryptionAlgorithm>?) {
        val unlockingAuthenticator = UnlockingAuthenticator(
            authenticator = authenticator,
            acquireLock = { mutex.lock() },
            releaseLock = { mutex.unlock() },
        )
        mutex.lock()
        try {
            vault.unlock(unlockingAuthenticator, dbKey, backupKey)
        } finally {
            mutex.unlock()
        }
    }

    val state: StateFlow<Vault.State> by lazy { vault.observeState() }
    val totpSecrets: StateFlow<List<TotpSecret>> by lazy { vault.observeTotpSecrets() }
    val passkeys: StateFlow<List<Passkey>> by lazy { vault.observePasskeys() }
}

private class UnlockingAuthenticator(
    private val authenticator: Authenticator,
    private val acquireLock: suspend () -> Unit,
    private val releaseLock: suspend () -> Unit,
) : Authenticator {
    override suspend fun authenticate() {
        releaseLock()
        try {
            authenticator.authenticate()
        } finally {
            acquireLock()
        }
    }

    override suspend fun authenticate(sig: Signature): Signature {
        releaseLock()
        try {
            return authenticator.authenticate(sig)
        } finally {
            acquireLock()
        }
    }

}

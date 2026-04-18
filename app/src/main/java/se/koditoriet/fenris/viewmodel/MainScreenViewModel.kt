package se.koditoriet.fenris.viewmodel

import android.app.Application
import kotlinx.coroutines.flow.StateFlow
import se.koditoriet.fenris.crypto.EphemeralSymmetricKey
import se.koditoriet.fenris.vault.Vault

class MainScreenViewModel(app: Application) : ActivityViewModel(app) {
    val vaultState: StateFlow<Vault.State>
        get() = vault.state

    val ephemeralKey: EphemeralSymmetricKey
        get() = vault.ephemeralKey

    suspend fun setPasskeyScreenDismissed() {
        configDatastore.updateData { it.copy(passkeyScreenDismissed = true) }
    }
}

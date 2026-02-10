package se.koditoriet.fenris.viewmodel

import android.app.Application

class FenrisViewModel(app: Application) : ActivityViewModel(app) {
    val vaultState
        get() = vault.state

    suspend fun setPasskeyScreenDismissed() {
        configDatastore.updateData { it.copy(passkeyScreenDismissed = true) }
    }
}

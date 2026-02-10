package se.koditoriet.fenris.viewmodel

import android.app.Application
import kotlinx.coroutines.flow.Flow
import se.koditoriet.fenris.crypto.AuthenticatorFactory
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Passkey

class CredentialProviderViewModel(app: Application) : ActivityViewModel(app) {
    val passkeys: Flow<List<Passkey>>
        get() = vault.passkeys

    suspend fun getPasskey(credentialId: CredentialId) = vault.withLock { getPasskey(credentialId) }
    suspend fun updatePasskey(passkey: Passkey) = vault.withLock { updatePasskey(passkey) }

    suspend fun addPasskey(rpId: String, userId: ByteArray, userName: String, displayName: String) = vault.withLock {
        addPasskey(rpId, userId, userName, displayName)
    }

    suspend fun signWithPasskey(authFactory: AuthenticatorFactory, passkey: Passkey, data: ByteArray) = vault.withLock {
        // TODO: explain where and as who the user is signing in?
        authFactory.withReason(appStrings.viewModel.authUsePasskey, appStrings.viewModel.authUsePasskeySubtitle) {
            signWithPasskey(it, passkey, data)
        }
    }
}

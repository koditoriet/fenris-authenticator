package se.koditoriet.fenris.viewmodel

import android.app.Application
import kotlinx.coroutines.flow.StateFlow
import se.koditoriet.fenris.crypto.AuthenticatorFactory
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Passkey

class CredentialProviderViewModel(app: Application) : ActivityViewModel(app) {
    val passkeys: StateFlow<List<Passkey>> by lazy { vault.passkeys }

    suspend fun getPasskey(credentialId: CredentialId) = vault.withLock { getPasskey(credentialId) }
    suspend fun updatePasskey(passkey: Passkey) = vault.withLock { updatePasskey(passkey) }

    suspend fun addPasskey(rpId: String, userId: ByteArray, userName: String, displayName: String) = vault.withLock {
        addPasskey(rpId, userId, userName, displayName)
    }

    suspend fun signWithPasskey(authFactory: AuthenticatorFactory, passkey: Passkey, data: ByteArray) = vault.withLock {
        val reason = appStrings.viewModel.authUsePasskey(passkey.displayName)
        val subtitle = appStrings.viewModel.authUsePasskeySubtitle(passkey.userName)
        authFactory.withReason(reason, subtitle) {
            signWithPasskey(it, passkey, data)
        }
    }
}

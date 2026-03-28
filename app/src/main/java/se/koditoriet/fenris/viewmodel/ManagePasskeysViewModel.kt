package se.koditoriet.fenris.viewmodel

import android.app.Application
import kotlinx.coroutines.flow.StateFlow
import se.koditoriet.fenris.SortMode
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Passkey

class ManagePasskeysViewModel(app: Application) : ViewModelBase(app) {
    val passkeys: StateFlow<List<Passkey>> by lazy { vault.passkeys }

    fun onSortModeChange(sortMode: SortMode) = updateConfig { it.copy(passkeySortMode = sortMode) }
    fun onUpdatePasskey(passkey: Passkey) = withVault { updatePasskey(passkey) }
    fun onDeletePasskey(credentialId: CredentialId) = withVault { deletePasskey(credentialId) }
    fun onReindexPasskeys() = withVault { reindexPasskeys() }
}

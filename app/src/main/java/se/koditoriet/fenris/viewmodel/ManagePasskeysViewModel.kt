package se.koditoriet.fenris.viewmodel

import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import se.koditoriet.fenris.SortMode
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Passkey

class ManagePasskeysViewModel(app: Application) : ViewModelBase(app) {
    val passkeys: Flow<List<Passkey>>
        get() = vault.passkeys.distinctUntilChanged { oldList, newList ->
            oldList.map { it.copy(timeOfLastUse = null) } == newList.map { it.copy(timeOfLastUse = null) }
        }

    fun onSortModeChange(sortMode: SortMode) = updateConfig { it.copy(passkeySortMode = sortMode) }
    fun onUpdatePasskey(passkey: Passkey) = withVault { updatePasskey(passkey) }
    fun onDeletePasskey(credentialId: CredentialId) = withVault { deletePasskey(credentialId) }
    fun onReindexPasskeys() = withVault { reindexPasskeys() }
}

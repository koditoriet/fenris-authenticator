package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import se.koditoriet.fenris.SortMode
import se.koditoriet.fenris.crypto.AuthenticatorFactory
import se.koditoriet.fenris.vault.NewPasskey
import se.koditoriet.fenris.vault.NewTotpSecret
import se.koditoriet.fenris.vault.TotpSecret
import kotlin.time.Clock

private const val TAG = "ListSecretsViewModel"

class ListSecretsViewModel(app: Application) : ViewModelBase(app) {
    val secrets: StateFlow<List<TotpSecret>> by lazy { vault.totpSecrets }

    fun onLockVault() = onIOThread { vault.lockVault() }
    fun onAddSecret(newSecret: NewTotpSecret) = withVault { addTotpSecret(newSecret) }
    fun onUpdateSecret(totpSecret: TotpSecret) = withVault { updateSecret(totpSecret) }
    fun onDeleteSecret(id: TotpSecret.Id) = withVault { deleteSecret(id) }
    fun onReindexSecrets() = withVault { reindexSecrets() }
    fun onSortModeChange(sortMode: SortMode) = updateConfig { it.copy(totpSecretSortMode = sortMode) }

    fun onImportCredentials(
        secrets: Set<NewTotpSecret>,
        passkeys: Set<NewPasskey>,
        onSuccess: () -> Unit,
        onFailure: (Set<NewTotpSecret>) -> Unit,
    ) = withVault {
        require(passkeys.isEmpty()) {
            "passkey imports are not supported yet"
        }

        val failures = mutableSetOf<NewTotpSecret>()
        secrets.forEach {
            try {
                addTotpSecret(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import secret", e)
                failures += it
            }
        }

        if (failures.isEmpty()) {
            onSuccess()
        } else {
            onFailure(failures)
        }
    }

    suspend fun getTotpCodes(
        authFactory: AuthenticatorFactory,
        totpSecret: TotpSecret,
        codes: Int = 2,
        clock: Clock = Clock.System,
    ): List<String> {
        require(codes >= 2)
        Log.d(TAG, "Generating $codes codes for TOTP secret with id ${totpSecret.id}")
        return vault.withLock {
            authFactory.withReason(
                reason = appStrings.viewModel.authRevealCode,
                subtitle = appStrings.viewModel.authRevealCodeSubtitle,
            ) {
                getTotpCodes(it, totpSecret, clock::now, codes)
            }
        }
    }
}


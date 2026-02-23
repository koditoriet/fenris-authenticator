package se.koditoriet.fenris.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import se.koditoriet.fenris.SortMode
import se.koditoriet.fenris.crypto.AuthenticatorFactory
import se.koditoriet.fenris.vault.NewTotpSecret
import se.koditoriet.fenris.vault.TotpAlgorithm
import se.koditoriet.fenris.vault.TotpSecret
import se.koditoriet.fenris.vault.Vault
import kotlin.time.Clock

private const val TAG = "ListSecretsViewModel"

class ListSecretsViewModel(private val app: Application) : ViewModelBase(app) {
    val secrets: Flow<List<TotpSecret>>
        get() = vault.totpSecrets

    fun onLockVault() = onIOThread { vault.lockVault() }
    fun onAddSecret(newSecret: NewTotpSecret) = withVault { addTotpSecret(newSecret) }
    fun onUpdateSecret(totpSecret: TotpSecret) = withVault { updateSecret(totpSecret) }
    fun onDeleteSecret(id: TotpSecret.Id) = withVault { deleteSecret(id) }
    fun onReindexSecrets() = withVault { reindexSecrets() }
    fun onSortModeChange(sortMode: SortMode) = updateConfig { it.copy(totpSecretSortMode = sortMode) }

    @OptIn(ExperimentalSerializationApi::class)
    fun onImportFile(uri: Uri): Unit = withVault {
        Log.i(TAG, "Importing secrets from file $uri")
        check(config.first().enableDeveloperFeatures) {
            "tried to use developer feature without being a developer"
        }
        check(state == Vault.State.Unlocked)
        app.contentResolver.openInputStream(uri)!!.use { stream ->
            // Only JSON supported for now
            Json.decodeFromStream<Map<String, JsonImportItem>>(stream).forEach {
                Log.d(TAG, "Adding secret '${it.key} (${it.value.account})'")
                val account = it.value.toNewTotpSecret(it.key)
                addTotpSecret(account)
                account.secretData.secret.fill('\u0000')
            }
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

@Serializable
private class JsonImportItem(
    val secret: String,
    val account: String? = null,
    val digits: Int = 6,
    val period: Int = 30,
    val algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
) {
    fun toNewTotpSecret(issuer: String): NewTotpSecret =
        NewTotpSecret(
            metadata = NewTotpSecret.Metadata(
                issuer = issuer,
                account = account,
            ),
            secretData = NewTotpSecret.SecretData(
                secret = secret.toCharArray(),
                digits = digits,
                period = period,
                algorithm = algorithm,
            )
        )
}

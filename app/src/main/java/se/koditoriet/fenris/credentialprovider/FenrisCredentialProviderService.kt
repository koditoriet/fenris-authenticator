package se.koditoriet.fenris.credentialprovider

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import se.koditoriet.fenris.FenrisApp
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.ui.activities.credentialprovider.CreatePasskeyActivity
import se.koditoriet.fenris.ui.activities.credentialprovider.ListPasskeysActivity
import se.koditoriet.fenris.crypto.DummyAuthenticator
import se.koditoriet.fenris.vault.Vault

private val TAG: String = "FenrisCredentialProviderService"

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class FenrisCredentialProviderService : CredentialProviderService() {
    private val supervisorJob by lazy {
        SupervisorJob()
    }

    private val scope by lazy {
        CoroutineScope(Dispatchers.IO + supervisorJob)
    }

    private val strings by lazy {
        appStrings.credentialProvider
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        // we currently don't keep any credential state around
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        Log.i(TAG, "Credential list requested")
        scope.launch {
            (application as FenrisApp).vault.withLock {
                val config = (application as FenrisApp).config.data.first()
                when {
                    state == Vault.State.Locked && config.protectAccountList -> {
                        Log.i(TAG, "Vault is locked; presenting unlock option")
                        val pendingIntent = createPendingIntent(applicationContext, ListPasskeysActivity::class.java)
                        val authAction = AuthenticationAction(
                            title = strings.authenticationActionTitle,
                            pendingIntent = pendingIntent,
                        )
                        callback.onResult(BeginGetCredentialResponse(authenticationActions = listOf(authAction)))
                    }
                    else -> {
                        // unlock is idempotent and we know for a fact that the DB KEK doesn't require authentication
                        unlock(
                            DummyAuthenticator,
                            config.encryptedDbKey!!,
                            config.backupKeys?.toVaultBackupKeys(),
                        )
                        callback.onResult(
                            createBeginGetCredentialResponse(
                                vault = this,
                                context = applicationContext,
                                request = request,
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        if (request !is BeginCreatePublicKeyCredentialRequest) {
            Log.i(TAG, "Got (and ignored) non-public key credential creation request: ${request::class.simpleName}")
            callback.onError(CreateCredentialUnknownException())
            return
        }
        val pendingIntent = createPendingIntent(applicationContext, CreatePasskeyActivity::class.java)
        val entries = listOf(CreateEntry(appStrings.generic.appName, pendingIntent))
        callback.onResult(BeginCreateCredentialResponse(entries))
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
    }
}

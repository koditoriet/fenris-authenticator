package se.koditoriet.snout.credentialprovider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
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
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import se.koditoriet.snout.BiometricPromptAuthenticator
import se.koditoriet.snout.SnoutApp
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.codec.Base64Url
import se.koditoriet.snout.codec.webauthn.WebAuthnCreateResponse
import se.koditoriet.snout.codec.webauthn.AuthDataFlag
import se.koditoriet.snout.codec.webauthn.WebAuthnAuthResponse
import se.koditoriet.snout.crypto.DummyAuthenticator
import se.koditoriet.snout.vault.CredentialId
import se.koditoriet.snout.vault.Vault
import se.koditoriet.snout.viewmodel.SnoutViewModel
import kotlin.random.Random

private val TAG: String = "SnoutCredentialProviderService"

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class SnoutCredentialProviderService : CredentialProviderService() {
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
            (application as SnoutApp).vault.withLock {
                val config = (application as SnoutApp).config.data.first()
                when {
                    state == Vault.State.Locked && config.protectAccountList -> {
                        Log.i(TAG, "Vault is locked; presenting unlock option")
                        callback.onResult(
                            BeginGetCredentialResponse(
                                authenticationActions = listOf(
                                    AuthenticationAction(
                                        strings.authenticationActionTitle,
                                        createPendingIntent(applicationContext, UnlockVaultActivity::class.java),
                                    )
                                )
                            )
                        )
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
            callback.onError(CreateCredentialUnknownException())
            return
        }
        val pendingIntent = createPendingIntent(applicationContext, CreatePasskeyActivity::class.java)
        val entries = listOf(CreateEntry("Snout", pendingIntent))
        callback.onResult(BeginCreateCredentialResponse(entries))
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
    }
}

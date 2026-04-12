package se.koditoriet.fenris.credentialprovider.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import se.koditoriet.fenris.PASSKEY_AUTH_FLAGS
import se.koditoriet.fenris.crypto.BiometricPromptAuthenticator
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.credentialprovider.CREDENTIAL_DATA
import se.koditoriet.fenris.credentialprovider.CREDENTIAL_ID
import se.koditoriet.fenris.credentialprovider.webAuthnValidator
import se.koditoriet.fenris.credentialprovider.webauthn.AuthResponse
import se.koditoriet.fenris.credentialprovider.webauthn.PublicKeyCredentialRequestOptions
import se.koditoriet.fenris.credentialprovider.webauthn.SignedAuthResponse
import se.koditoriet.fenris.credentialprovider.webauthn.WebAuthnValidator
import se.koditoriet.fenris.crypto.AuthenticationFailedException
import se.koditoriet.fenris.ui.components.PasskeyIcon
import se.koditoriet.fenris.ui.components.PasskeyIconFlavor
import se.koditoriet.fenris.ui.components.ThemedEmptySpace
import se.koditoriet.fenris.ui.components.WarningInformationDialog
import se.koditoriet.fenris.ui.theme.BACKGROUND_ICON_SIZE
import se.koditoriet.fenris.ui.theme.FenrisTheme
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Passkey
import se.koditoriet.fenris.viewmodel.CredentialProviderViewModel
import kotlin.time.Clock

private const val TAG = "AuthenticateActivity"

class AuthenticateActivity : FragmentActivity() {
    private val viewModel by viewModels<CredentialProviderViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenStrings = appStrings.credentialProvider
        val authFactory = BiometricPromptAuthenticator.Factory(this@AuthenticateActivity)
        val clock: Clock = Clock.System

        val intent = intent
        if (intent == null) {
            Log.w(TAG, "Activity called without intent")
            finishWithResult(null)
            return
        }

        val requestInfo = GetRequestInfo.fromIntent(webAuthnValidator, intent)
        if (requestInfo == null) {
            finishWithResult(null)
            return
        }

        enableEdgeToEdge()
        setContent {
            val showUnableToEstablishTrustDialog = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                try {
                    viewModel.unlockVault(authFactory)
                } catch (_: AuthenticationFailedException) {
                    finishWithResult(null)
                    return@LaunchedEffect
                }

                Log.d(TAG, "Fetching passkey with credential ID ${requestInfo.credentialId}")
                val passkey = viewModel.getPasskey(requestInfo.credentialId)

                if (!requestInfo.isValid(passkey)) {
                    showUnableToEstablishTrustDialog.value = true
                    return@LaunchedEffect
                }

                val response = AuthResponse(
                    rpId = passkey.rpId,
                    credentialId = passkey.credentialId.id,
                    userId = passkey.userId.id,
                    flags = PASSKEY_AUTH_FLAGS,
                    providedClientDataHash = requestInfo.clientDataHash,
                    challenge = requestInfo.challenge,
                    origin = requestInfo.origin,
                    packageName = requestInfo.packageName,
                )

                try {
                    val signedResponse = response.sign {
                        Log.i(TAG, "Signing authentication request")
                        viewModel.signWithPasskey(authFactory, passkey, it)
                    }
                    viewModel.updatePasskey(passkey.copy(timeOfLastUse = clock.now().toEpochMilliseconds()))
                    finishWithResult(signedResponse)
                } catch (_: AuthenticationFailedException) {
                    finishWithResult(null)
                }
            }

            FenrisTheme {
                ThemedEmptySpace {
                    PasskeyIcon(
                        modifier = Modifier.size(BACKGROUND_ICON_SIZE),
                        flavor = PasskeyIconFlavor.Fenris,
                    )
                }

                if (showUnableToEstablishTrustDialog.value) {
                    WarningInformationDialog(
                        title = screenStrings.unableToEstablishTrust,
                        text = screenStrings.unableToEstablishTrustExplanation,
                        onDismiss = { finishWithResult(null) },
                    )
                }
            }
        }
    }

    private fun finishWithResult(signedResponse: SignedAuthResponse?) {
        Intent().let { intent ->
            if (signedResponse != null) {
                val publicKeyCredential = PublicKeyCredential(signedResponse.json)
                PendingIntentHandler.setGetCredentialResponse(
                    intent = intent,
                    response = GetCredentialResponse(publicKeyCredential)
                )
                setResult(RESULT_OK, intent)
            } else {
                Log.i(TAG, "Aborting signing")
                setResult(RESULT_CANCELED, intent)
            }
        }
        Log.d(TAG, "Finishing activity")
        finish()
    }
}

private class GetRequestInfo(
    val credentialId: CredentialId,
    val callingAppInfo: CallingAppInfo,
    val clientDataHash: ByteArray?,
    val requestJson: PublicKeyCredentialRequestOptions,
    private val validator: WebAuthnValidator,
) {
    val origin: String by lazy {
        validator.appInfoToOrigin(callingAppInfo)
    }

    val challenge: Base64Url
        get() = requestJson.challenge

    val packageName: String
        get() = callingAppInfo.packageName

    fun isValid(storedPasskey: Passkey): Boolean {
        val rpId = requestJson.rpId ?: validator.appInfoToRpId(callingAppInfo)
        if (rpId == null) {
            Log.e(TAG, "Unable to obtain RP")
            return false
        }

        if (!validator.rpIsValid(rpId)) {
            Log.e(TAG, "Request RP is invalid!")
            return false
        }

        if (!validator.originIsValid(callingAppInfo)) {
            Log.e(TAG, "Origin is invalid!")
            return false
        }

        if (storedPasskey.rpId != rpId) {
            Log.e(TAG, "Request RP does not match passkey RP!")
            return false
        }

        return true
    }

    companion object {
        fun fromIntent(validator: WebAuthnValidator, intent: Intent): GetRequestInfo? {
            Log.d(TAG, "Extracting selected credential ID")
            val credentialId = intent
                .getBundleExtra(CREDENTIAL_DATA)
                ?.getString(CREDENTIAL_ID)

            if (credentialId == null) {
                Log.e(TAG, "Intent does not contain a credential ID")
                return null
            }

            Log.i(TAG, "User requested signing with credential $credentialId")

            Log.d(TAG, "Extracting credential options")
            val request = PendingIntentHandler
                .retrieveProviderGetCredentialRequest(intent)

            if (request == null) {
                Log.e(TAG, "Intent does not contain a ProviderGetCredentialRequest")
                return null
            }

            val credentialOption = request.credentialOptions.first() as GetPublicKeyCredentialOption

            Log.d(TAG, "Parsing request JSON")
            return GetRequestInfo(
                credentialId = CredentialId.fromString(credentialId),
                callingAppInfo = request.callingAppInfo,
                clientDataHash = credentialOption.clientDataHash,
                requestJson = PublicKeyCredentialRequestOptions.fromJSON(credentialOption.requestJson),
                validator = validator,
            )
        }
    }
}

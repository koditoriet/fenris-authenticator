package se.koditoriet.fenris.credentialprovider.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import se.koditoriet.fenris.PASSKEY_CREATE_FLAGS
import se.koditoriet.fenris.crypto.BiometricPromptAuthenticator
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.codec.Base64Url.Companion.toBase64Url
import se.koditoriet.fenris.credentialprovider.webAuthnValidator
import se.koditoriet.fenris.credentialprovider.webauthn.CreateResponse
import se.koditoriet.fenris.credentialprovider.webauthn.PublicKeyCredentialCreationOptions
import se.koditoriet.fenris.credentialprovider.webauthn.WebAuthnValidator
import se.koditoriet.fenris.crypto.AuthenticationFailedException
import se.koditoriet.fenris.ui.components.BadInputInformationDialog
import se.koditoriet.fenris.ui.components.PasskeyIcon
import se.koditoriet.fenris.ui.components.sheet.BottomSheet
import se.koditoriet.fenris.ui.onIOThread
import se.koditoriet.fenris.ui.components.ThemedEmptySpace
import se.koditoriet.fenris.ui.components.WarningInformationDialog
import se.koditoriet.fenris.ui.screens.main.passkeys.sheets.EditPasskeyNameSheet
import se.koditoriet.fenris.ui.theme.BACKGROUND_ICON_SIZE
import se.koditoriet.fenris.ui.theme.FenrisTheme
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Passkey
import se.koditoriet.fenris.viewmodel.CredentialProviderViewModel

private const val TAG = "CreatePasskeyActivity"

class CreatePasskeyActivity : FragmentActivity() {
    private val viewModel by viewModels<CredentialProviderViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenStrings = appStrings.credentialProvider
        val authFactory = BiometricPromptAuthenticator.Factory(this@CreatePasskeyActivity)
        val requestInfo = CreateRequestInfo.fromIntent(intent!!)
        val requestInfoIsValid = requestInfo.isValid(webAuthnValidator)

        enableEdgeToEdge()
        setContent {
            Log.d(TAG, "Starting activity")
            val passkeys by viewModel.passkeys.collectAsState(emptyList())
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            LaunchedEffect(Unit) {
                try {
                    if (requestInfoIsValid) {
                        viewModel.unlockVault(authFactory)
                    }
                } catch (_: AuthenticationFailedException) {
                    finishWithResponse(null)
                    return@LaunchedEffect
                }
            }

            FenrisTheme {
                ThemedEmptySpace {
                    PasskeyIcon(Modifier.size(BACKGROUND_ICON_SIZE))

                    if (credentialAlreadyExists(passkeys, requestInfo)) {
                        BadInputInformationDialog(
                            title = screenStrings.passkeyAlreadyExists,
                            text = screenStrings.passkeyAlreadyExistsExplanation,
                            onDismiss = { finishWithResponse(null) }
                        )
                        return@ThemedEmptySpace
                    }

                    if (!requestInfoIsValid) {
                        WarningInformationDialog(
                            title = screenStrings.unableToEstablishTrust,
                            text = screenStrings.unableToEstablishTrustExplanation,
                            onDismiss = { finishWithResponse(null) }
                        )
                        return@ThemedEmptySpace
                    }

                    BottomSheet(
                        hideSheet = { finishWithResponse(null) },
                        sheetState = sheetState,
                        sheetViewState = Unit,
                    ) { _ ->
                        EditPasskeyNameSheet(
                            prefilledDisplayName = requestInfo.requestJson.rp.id,
                            onSave = onIOThread { displayName ->
                                val response = createPasskey(webAuthnValidator, displayName, requestInfo)
                                Log.i(
                                    TAG,
                                    "Created passkey with credential id ${response.credentialId.toBase64Url()}"
                                )
                                finishWithResponse(response)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun credentialAlreadyExists(passkeys: List<Passkey>, requestInfo: CreateRequestInfo): Boolean {
        val excludeCredentials = requestInfo.requestJson.excludeCredentials.map { CredentialId(it.id) }
        val excludedCredentials = passkeys.filter {
            it.credentialId in excludeCredentials
        }

        // If any credential in excludeCredentials is already in the vault, we already have a credential that is
        // recognized by both us and the RP, so we should not create a new one.
        return excludedCredentials.isNotEmpty()
    }

    private suspend fun createPasskey(
        validator: WebAuthnValidator,
        displayName: String,
        requestInfo: CreateRequestInfo,
    ): CreateResponse {
        val (credentialId, pubkey) = viewModel.addPasskey(
            rpId = requestInfo.requestJson.rp.id,
            userId = requestInfo.requestJson.user.id.toByteArray(),
            userName = requestInfo.requestJson.user.displayName,
            displayName = displayName,
        )

        return CreateResponse(
            rpId = requestInfo.requestJson.rp.id,
            credentialId = credentialId.toByteArray(),
            publicKey = pubkey,
            flags = PASSKEY_CREATE_FLAGS,
            origin = validator.appInfoToOrigin(requestInfo.callingAppInfo),
            packageName = requestInfo.callingAppInfo.packageName,
            challenge = Base64Url.fromBase64UrlString(requestInfo.requestJson.challenge),
        )
    }

    private fun finishWithResponse(response: CreateResponse?) {
        Intent().apply {
            if (response != null) {
                PendingIntentHandler.setCreateCredentialResponse(
                    intent = this,
                    response = CreatePublicKeyCredentialResponse(response.json)
                )
                setResult(RESULT_OK, this)
            } else {
                Log.i(TAG, "Aborting passkey creation")
                setResult(RESULT_CANCELED, this)
            }
        }
        Log.d(TAG, "Finishing activity")
        finish()
    }
}

private class CreateRequestInfo(
    val callingAppInfo: CallingAppInfo,
    val requestJson: PublicKeyCredentialCreationOptions,
) {
    fun isValid(browserList: WebAuthnValidator): Boolean {
        if (!browserList.rpIsValid(requestJson.rp.id)) {
            Log.e(TAG, "Request RP is invalid!")
            return false
        }

        if (!browserList.originIsValid(callingAppInfo)) {
            Log.e(TAG, "Origin is invalid!")
            return false
        }

        return true
    }

    companion object {
        fun fromIntent(intent: Intent): CreateRequestInfo {
            Log.d(TAG, "Extracting credential options")

            val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)!!
            val publicKeyCredentialRequest = request.callingRequest as CreatePublicKeyCredentialRequest

            Log.d(TAG, "Parsing request JSON")
            return CreateRequestInfo(
                callingAppInfo = request.callingAppInfo,
                requestJson = PublicKeyCredentialCreationOptions.fromJSON(publicKeyCredentialRequest.requestJson),
            )
        }
    }
}

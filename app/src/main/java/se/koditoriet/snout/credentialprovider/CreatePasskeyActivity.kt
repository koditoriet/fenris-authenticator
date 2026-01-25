package se.koditoriet.snout.credentialprovider

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import se.koditoriet.snout.BiometricPromptAuthenticator
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.codec.Base64Url
import se.koditoriet.snout.codec.webauthn.AuthDataFlag
import se.koditoriet.snout.codec.webauthn.WebAuthnCreateResponse
import se.koditoriet.snout.viewmodel.SnoutViewModel
import kotlin.getValue


class CreatePasskeyActivity : FragmentActivity() {
    private val TAG = "CreatePasskeyActivity"
    private val viewModel: SnoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)!!
            val actualRequest = request.callingRequest as CreatePublicKeyCredentialRequest
            val requestOptions = JSONObject(actualRequest.requestJson)

            // TODO: if we have a credential where credentialId in option.requestJson.excludeCredentials.map { it.id },
            //       refuse to create a new one

            val rpId = requestOptions.getJSONObject("rp").getString("id")
            val user = requestOptions.getJSONObject("user")

            val authenticator = BiometricPromptAuthenticator
                .Factory(this@CreatePasskeyActivity)
                .withReason(
                    appStrings.viewModel.authCreatePasskey,
                    appStrings.viewModel.authCreatePasskeySubtitle
                )

            authenticator.authenticate {
                val (credentialId, pubkey) = viewModel.addPasskey(
                    rpId = rpId,
                    userId = Base64Url.fromBase64UrlString(user.getString("id")).toByteArray(),
                    userName = user.getString("name"),
                    displayName = user.getString("displayName"),
                )

                val response = WebAuthnCreateResponse(
                    rpId = rpId,
                    credentialId = credentialId.toByteArray(),
                    publicKey = pubkey,
                    callingAppInfo = request.callingAppInfo,
                    flags = AuthDataFlag.defaultFlags,
                )

                Intent().apply {
                    PendingIntentHandler.setCreateCredentialResponse(
                        this,
                        CreatePublicKeyCredentialResponse(response.json)
                    )
                    setResult(RESULT_OK, this)
                }
                finish()
            }
        }
    }
}

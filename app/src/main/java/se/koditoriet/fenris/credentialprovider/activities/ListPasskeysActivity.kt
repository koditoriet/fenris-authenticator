package se.koditoriet.fenris.credentialprovider.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import se.koditoriet.fenris.crypto.BiometricPromptAuthenticator
import se.koditoriet.fenris.FenrisApp
import se.koditoriet.fenris.credentialprovider.createBeginGetCredentialResponse
import se.koditoriet.fenris.crypto.AuthenticationFailedException
import se.koditoriet.fenris.ui.components.PasskeyIcon
import se.koditoriet.fenris.ui.components.PasskeyIconFlavor
import se.koditoriet.fenris.ui.components.ThemedEmptySpace
import se.koditoriet.fenris.ui.screenLockEnabled
import se.koditoriet.fenris.ui.screens.EnableScreenLockScreen
import se.koditoriet.fenris.ui.theme.BACKGROUND_ICON_SIZE
import se.koditoriet.fenris.ui.theme.FenrisTheme
import se.koditoriet.fenris.viewmodel.CredentialProviderViewModel

private const val TAG = "ListPasskeysActivity"

class ListPasskeysActivity : FragmentActivity() {
    private val viewModel by viewModels<CredentialProviderViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        if (intent == null) {
            Log.w(TAG, "Activity created with null intent")
            finishWithResult(null)
            return
        }

        val request = try {
            PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)!!
        } catch (e: Throwable) {
            Log.e(TAG, "Unable to get BeginGetCredentialRequest from intent", e)
            finishWithResult(null)
            return
        }

        enableEdgeToEdge()
        setContent {
            if (!screenLockEnabled()) {
                EnableScreenLockScreen()
                return@setContent
            }

            LaunchedEffect(Unit) {
                try {
                    viewModel.unlockVault(BiometricPromptAuthenticator.Factory(this@ListPasskeysActivity))
                } catch (_: AuthenticationFailedException) {
                    finishWithResult(null)
                    return@LaunchedEffect
                } catch (e: Throwable) {
                    Log.e(TAG, "Vault unlock failed", e)
                    finishWithResult(null)
                    return@LaunchedEffect
                }

                Log.i(TAG, "Vault successfully unlocked, creating credential response")
                (application as FenrisApp).vault.withLock {
                    try {
                        val response = createBeginGetCredentialResponse(
                            vault = this,
                            context = this@ListPasskeysActivity,
                            request = request,
                        )

                        Log.i(TAG, "Sending BeginGetCredentialResponse to credential manager")
                        finishWithResult(response)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed to parse BeginGetCredentialRequest", e)
                        finishWithResult(null)
                    }
                }
            }
            FenrisTheme {
                ThemedEmptySpace {
                    PasskeyIcon(
                        modifier = Modifier.size(BACKGROUND_ICON_SIZE),
                        flavor = PasskeyIconFlavor.Fenris,
                    )
                }
            }
        }
    }

    private fun finishWithResult(response: BeginGetCredentialResponse?) {
        Intent().let { intent ->
            if (response != null) {
                PendingIntentHandler.setBeginGetCredentialResponse(intent, response)
                setResult(RESULT_OK, intent)
            } else {
                Log.i(TAG, "Abort passkey listing")
                setResult(RESULT_CANCELED, intent)
            }
        }
        Log.d(TAG, "Finishing activity")
        finish()
    }
}

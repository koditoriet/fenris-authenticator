package se.koditoriet.fenris

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.koditoriet.fenris.crypto.AuthenticationFailedException
import se.koditoriet.fenris.crypto.Authenticator
import se.koditoriet.fenris.crypto.AuthenticatorFactory
import java.security.Signature
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "BiometricPromptAuthenticator"

class BiometricPromptAuthenticator(
    private val activity: FragmentActivity,
    private val reason: String,
    private val subtitle: String,
) : Authenticator {
    override suspend fun authenticate() {
        authenticate(null)
    }

    override suspend fun authenticate(sig: Signature): Signature =
        authenticate(BiometricPrompt.CryptoObject(sig))?.signature!!

    private suspend fun authenticate(cryptoObject: BiometricPrompt.CryptoObject?): BiometricPrompt.CryptoObject? {
        Log.i(TAG, "Authenticating user")
        val result = withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                val callback = AuthenticationCallback(continuation)
                val prompt = BiometricPrompt(activity, callback)
                when (cryptoObject) {
                    null -> prompt.authenticate(promptInfo)
                    else -> prompt.authenticate(promptInfo, cryptoObject)
                }
            }
        }
        when (result) {
            AuthenticationResult.Failure -> throw AuthenticationFailedException("user canceled authentication")
            is AuthenticationResult.Success -> return result.cryptoObject
        }
    }

    private val promptInfo by lazy {
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(reason)
            setSubtitle(subtitle)
            setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        }.build()
    }

    class Factory(private val activity: FragmentActivity) : AuthenticatorFactory {
        override fun withReason(reason: String, subtitle: String) =
            BiometricPromptAuthenticator(activity, reason, subtitle)
    }
}

private class AuthenticationCallback(
    private val continuation: Continuation<AuthenticationResult>,
) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        Log.i(TAG, "Authentication succeeded")
        continuation.resume(AuthenticationResult.Success(result.cryptoObject))
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        Log.d(TAG, "Authentication failed")
        // Do nothing here; we'll be getting more callbacks as the user keeps trying
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Log.w(TAG, "Authentication errored ($errorCode): $errString")
        continuation.resume(AuthenticationResult.Failure)
    }
}

private sealed interface AuthenticationResult {
    class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : AuthenticationResult
    object Failure : AuthenticationResult
}
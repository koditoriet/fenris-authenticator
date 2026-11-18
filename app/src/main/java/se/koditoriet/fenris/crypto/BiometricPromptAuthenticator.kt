package se.koditoriet.fenris.crypto

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.room.concurrent.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.Signature
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val TAG = "BiometricPromptAuthenticator"

class BiometricPromptAuthenticator(
    private val activity: FragmentActivity,
    private val reason: String,
    private val subtitle: String,
) : Authenticator {
    override suspend fun authenticate() {
        authenticate(null)
    }

    override suspend fun authenticate(sig: Signature): Signature {
        val cryptoObject = authenticate(BiometricPrompt.CryptoObject(sig))
        if (cryptoObject == null) {
            Log.e(TAG, "BUG: CryptoObject was null after parameterized authentication")
            throw IllegalStateException()
        }

        val signature = cryptoObject.signature
        if (signature == null) {
            Log.e(TAG, "BUG: signature was null after parameterized authentication")
            throw IllegalStateException()
        }

        return signature
    }

    private suspend fun authenticate(cryptoObject: BiometricPrompt.CryptoObject?): BiometricPrompt.CryptoObject? {
        Log.i(TAG, "Authenticating user")
        val result = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val callback = AuthenticationCallback(continuation)
                val prompt = BiometricPrompt(activity, callback)
                continuation.invokeOnCancellation {
                    prompt.cancelAuthentication()
                }
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
    private val resumed = AtomicBoolean(false)

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        if (resumed.compareAndSet(false, true)) {
            Log.i(TAG, "Authentication succeeded")
            continuation.resume(AuthenticationResult.Success(result.cryptoObject))
        } else {
            Log.w(TAG, "Authentication completed more than once. Ignoring.")
        }
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        Log.d(TAG, "Authentication failed")
        // Do nothing here; we'll be getting more callbacks as the user keeps trying
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        if (resumed.compareAndSet(false, true)) {
            Log.w(TAG, "Authentication errored ($errorCode): $errString")
            continuation.resume(AuthenticationResult.Failure)
        } else {
            Log.w(TAG, "Authentication completed more than once. Ignoring.")
        }
    }
}

private sealed interface AuthenticationResult {
    class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : AuthenticationResult
    object Failure : AuthenticationResult
}
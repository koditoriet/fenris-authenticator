package se.koditoriet.fenris.crypto

import java.security.Signature

/**
 * An object that contains enough information to create a new authenticator if given a reason and subtitle for a prompt.
 */
interface AuthenticatorFactory {
    fun withReason(reason: String, subtitle: String): Authenticator

    suspend fun <T> withReason(reason: String, subtitle: String, action: suspend (Authenticator) -> T): T =
        action(withReason(reason, subtitle))
}

interface Authenticator {
    /**
     * Authenticate the user to use a key for some period of time following authentication.
     * Must throw AuthenticationFailedException if authentication fails.
     */
    suspend fun authenticate()

    /**
     * Authenticate the user to use the given Signature object for a single signature.
     * Must throw AuthenticationFailedException if authentication fails.
     */
    suspend fun authenticate(sig: Signature): Signature
}

/**
 * Authenticator that always fails.
 */
object DummyAuthenticator : Authenticator {
    override suspend fun authenticate() =
        throw AuthenticationFailedException(
            "Authentication always fails with DummyAuthenticator"
        )

    override suspend fun authenticate(sig: Signature): Signature =
        throw AuthenticationFailedException(
            "Authentication always fails with DummyAuthenticator"
        )

    class Factory : AuthenticatorFactory {
        override fun withReason(reason: String, subtitle: String) =
            DummyAuthenticator
    }
}

data class AuthenticationFailedException(val reason: String) : Exception(reason)

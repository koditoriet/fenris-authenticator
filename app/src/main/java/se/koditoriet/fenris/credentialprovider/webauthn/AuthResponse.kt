package se.koditoriet.fenris.credentialprovider.webauthn

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.codec.Base64Url.Companion.toBase64Url
import java.security.MessageDigest

class AuthResponse(
    val rpId: String,
    val credentialId: Base64Url,
    val userId: Base64Url?,
    val flags: Set<AuthDataFlag>,
    val providedClientDataHash: ByteArray?,
    val challenge: Base64Url,
    val origin: String,
    val packageName: String?,
) {
    val authenticatorData: ByteArray by lazy {
        val md = MessageDigest.getInstance("SHA-256")
        val rpHash = md.digest(rpId.toByteArray(Charsets.UTF_8))
        val flags = byteArrayOf(flags.toByte())
        val signCount = ByteArray(4)
        rpHash + flags + signCount
    }

    val clientDataJSON: String by lazy {
        if (providedClientDataHash != null) {
            // If we got a clientDataHash from GCM, GCM will also handle the clientDataJSON.
            // In this case, clientDataJSON MUST be either the empty string, valid JSON, or some variant on the string
            // "<placeholder>", or remote/hybrid transport passkey authentication will fail.
            // Local passkey authentication will not, however. I don't know why, and I'm not sure my mind could
            // contain the reason without breaking if someone were to tell me.
            return@lazy ""
        }
        Json.encodeToString(
            ClientData(
                type = "webauthn.get",
                challenge = challenge.string,
                origin = origin,
                androidPackageName = packageName,
            )
        )
    }

    val clientDataHash: ByteArray by lazy {
        providedClientDataHash ?: clientDataJSON.let {
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(it.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun sign(signer: suspend (ByteArray) -> ByteArray): SignedAuthResponse {
        val signData = authenticatorData + clientDataHash
        val signature = signer(signData)
        return SignedAuthResponse(
            authenticatorData = authenticatorData.toBase64Url(),
            signature = signature.toBase64Url(),
            userId = userId,
            credentialId = credentialId,
            clientDataJSON = clientDataJSON.toByteArray(Charsets.UTF_8).toBase64Url(),
        )
    }
}

class SignedAuthResponse(
    val authenticatorData: Base64Url,
    val signature: Base64Url,
    val userId: Base64Url?,
    val credentialId: Base64Url,
    val clientDataJSON: Base64Url,
) {
    val response by lazy {
        Response(
            clientDataJSON = clientDataJSON.string,
            authenticatorData = authenticatorData.string,
            signature = signature.string,
            userHandle = userId?.string,
        )
    }

    val credential by lazy {
        Credential(
            type = "public-key",
            id = credentialId.string,
            rawId = credentialId.string,
            response = response,
            clientExtensionResults = emptyMap(),
            authenticatorAttachment = "platform",
        )
    }

    @Serializable
    class Response(
        val authenticatorData: String,
        val signature: String,
        val userHandle: String?,
        val clientDataJSON: String,
    )

    @Serializable
    class Credential(
        val type: String,
        val id: String,
        val rawId: String,
        val response: Response,
        val clientExtensionResults: Map<String, String>,
        val authenticatorAttachment: String,
    )

    val json: String by lazy {
        Json.encodeToString(credential)
    }
}

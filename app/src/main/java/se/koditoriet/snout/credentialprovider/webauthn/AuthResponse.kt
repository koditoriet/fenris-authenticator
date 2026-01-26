package se.koditoriet.snout.credentialprovider.webauthn

import org.json.JSONObject
import se.koditoriet.snout.codec.Base64Url
import se.koditoriet.snout.codec.Base64Url.Companion.toBase64Url
import java.security.MessageDigest

class AuthResponse(
    val rpId: String,
    val credentialId: Base64Url,
    val userId: Base64Url?,
    val flags: Set<AuthDataFlag>,
    val clientDataHash: ByteArray,
) {
    val authenticatorData: ByteArray by lazy {
        val md = MessageDigest.getInstance("SHA-256")
        val rpHash = md.digest(rpId.toByteArray(Charsets.UTF_8))
        val flags = byteArrayOf(flags.toByte())
        val signCount = ByteArray(4)
        rpHash + flags + signCount
    }

    suspend fun sign(signer: suspend (ByteArray) -> ByteArray): SignedAuthResponse {
        val signData = authenticatorData + clientDataHash
        val signature = signer(signData)
        return SignedAuthResponse(
            authenticatorData = authenticatorData.toBase64Url(),
            signature = signature.toBase64Url(),
            userId = userId,
            credentialId = credentialId,
        )
    }
}

class SignedAuthResponse(
    val authenticatorData: Base64Url,
    val signature: Base64Url,
    val userId: Base64Url?,
    val credentialId: Base64Url,
) {
    val response: JSONObject by lazy {
        JSONObject().apply {
            put("clientDataJSON", "dummy value; Android replaces it anyway")
            put("authenticatorData", authenticatorData.string)
            put("signature", signature.string)
            if (userId != null) {
                put("userHandle", userId.string)
            }
        }
    }

    val credential: JSONObject by lazy {
        JSONObject().apply {
            put("type", "public-key")
            put("id", credentialId.string)
            put("rawId", credentialId.string)
            put("response", response)
            put("clientExtensionResults", JSONObject())
            put("authenticatorAttachment", "platform")
        }
    }

    val json: String by lazy { credential.toString() }
}

package se.koditoriet.snout.credentialprovider.webauthn

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.koditoriet.snout.codec.Base64Url

/**
 * Contains the subset of PublicKeyCredentialCreationOptions that we use.
 */
@Serializable
class CreateRequest(
    val rp: RP,
    val user: User,
    val timeout: Int,
    val excludeCredentials: List<CredentialDescriptor> = emptyList(),
) {
    @Serializable
    class CredentialDescriptor(
        val id: Base64Url,
        val type: String,
        val transports: List<String>,
    )

    @Serializable
    class RP(val id: String)

    @Serializable
    class User(
        val id: Base64Url,
        val displayName: String,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJSON(s: String): CreateRequest =
            json.decodeFromString(s)
    }
}

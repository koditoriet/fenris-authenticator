package se.koditoriet.fenris.credentialprovider.webauthn

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class ClientData(
    val type: String,
    val challenge: String,
    val origin: String,
    val androidPackageName: String?,
) {
    val json: String by lazy {
        Json.encodeToString(this)
    }
}

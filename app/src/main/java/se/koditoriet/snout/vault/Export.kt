package se.koditoriet.snout.vault

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val BACKUP_FORMAT_VERSION: Int = 2

@Serializable
data class VaultExport(
    val secrets: List<TotpSecret>,
    val passkeys: List<Passkey>,
    val format: Int = BACKUP_FORMAT_VERSION,
) {
    fun encode(): ByteArray =
        json.encodeToString(this).toByteArray(Charsets.UTF_8)

    companion object {
        fun decode(data: ByteArray): VaultExport =
            json.decodeFromString(data.toString(Charsets.UTF_8))
    }
}

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

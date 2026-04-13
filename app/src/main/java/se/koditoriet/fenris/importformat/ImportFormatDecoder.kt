package se.koditoriet.fenris.importformat

import se.koditoriet.fenris.vault.NewPasskey
import se.koditoriet.fenris.vault.NewTotpSecret

sealed interface ImportFormatDecoder {
    val formatName: String
    val formatMimeTypes: Set<String>

    fun decode(bytes: ByteArray): DecodedImport

    data class DecodedImport(
        val totpSecrets: List<NewTotpSecret> = emptyList(),
        val passkeys: List<NewPasskey> = emptyList(),
        val incompatible: List<IncompatibleItem> = emptyList(),
    ) {
        data class IncompatibleItem(val type: Type, val displayName: String) {
            enum class Type {
                TOTP,
                HOTP,
                Passkey,
                Other,
            }
        }
    }

    companion object {
        val decoders: List<ImportFormatDecoder> by lazy {
            listOf(
                GoogleAuthenticatorDecoder,
            )
        }
    }
}

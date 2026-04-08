package se.koditoriet.fenris.vault

import se.koditoriet.fenris.vault.TotpSecret.Id

/**
 * An obfuscated version of the VaultExport for carrying data between the encoded export file and the screen listing which secrets to import.
 */
data class MiniVaultExport(
    val secrets: List<MiniTotpSecret>,
    val passkeys: List<MiniPassKey>
)

data class MiniTotpSecret(
    val id: Id,
    val sortOrder: Long,
    val issuer: String,
)

data class MiniPassKey(
    val credentialId: CredentialId,
    val sortOrder: Long,
    val displayName: String,
)
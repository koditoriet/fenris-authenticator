package se.koditoriet.fenris.vault

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.koditoriet.fenris.SYMMETRIC_KEY_ALGORITHM
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.codec.Base64Url.Companion.toBase64Url
import se.koditoriet.fenris.crypto.KeyDerivationParams
import se.koditoriet.fenris.crypto.Authenticator
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.crypto.Cryptographer
import se.koditoriet.fenris.crypto.types.EncryptionAlgorithm
import se.koditoriet.fenris.crypto.types.KeyHandle
import se.koditoriet.fenris.crypto.deriveBackupKey
import se.koditoriet.fenris.crypto.types.EncryptedData

const val LATEST_BACKUP_FORMAT: Int = 1

@Serializable
data class VaultExportEnvelope(
    val encryptedExportJson: String,
    val params: EncryptionParams,
    val format: Int = LATEST_BACKUP_FORMAT,
) {
    fun encode(): ByteArray =
        json.encodeToString(this).toByteArray(Charsets.UTF_8)

    suspend fun decrypt(cryptographer: Cryptographer, seed: BackupSeed, password: String): VaultExport {
        val seedKey = seed.deriveBackupKey()
        val backupDek = try {
            cryptographer.withDecryptionKey(seedKey, EncryptionAlgorithm.AES_GCM) {
                deriveBackupKey(params.keyDerivationParams, password)
            }
        } finally {
            seedKey.fill(0)
        }

        try {
            return decrypt(cryptographer, backupDek)
        } finally {
            backupDek.fill(0)
        }
    }

    private suspend fun decrypt(cryptographer: Cryptographer, backupDek: ByteArray): VaultExport {
        val vaultExportJson = cryptographer.withDecryptionKey(backupDek, EncryptionAlgorithm.AES_GCM) {
            decrypt(EncryptedData.decode(encryptedExportJson))
        }

        try {
            return VaultExport.decode(vaultExportJson)
        } finally {
            vaultExportJson.fill(0)
        }
    }

    companion object {
        suspend fun encrypt(
            cryptographer: Cryptographer,
            authenticator: Authenticator,
            export: VaultExport,
            backupKey: KeyHandle<EncryptionAlgorithm>,
            password: String,
        ): VaultExportEnvelope {
            val (params, backupDek) = cryptographer.withEncryptionKey(authenticator, backupKey) {
                deriveBackupKey(password)
            }
            val encryptedExportJson = try {
                cryptographer.withEncryptionKey(backupDek, SYMMETRIC_KEY_ALGORITHM) {
                    encrypt(export.encode())
                }
            } finally {
                backupDek.fill(0)
            }
            return VaultExportEnvelope(
                encryptedExportJson = encryptedExportJson.encode(),
                params = EncryptionParams.fromArgon2Params(params),
            )
        }

        fun decode(data: ByteArray): VaultExportEnvelope {
            val jsonData = data.decodeToString()
            val envelope = json.decodeFromString<VaultExportEnvelope>(jsonData)
            if (envelope.format > LATEST_BACKUP_FORMAT) {
                throw UnknownExportFormatException(
                    latestSupportedFormat = LATEST_BACKUP_FORMAT,
                    actualFormat = envelope.format,
                )
            }
            return envelope
        }
    }
}

@Serializable
data class EncryptionParams(
    val parallelism: Int,
    val memorySizeKb: Int,
    val iterations: Int,
    val salt: Base64Url,
    val encryptedRandomKey: String,
) {
    val keyDerivationParams: KeyDerivationParams by lazy {
        KeyDerivationParams(
            parallelism = parallelism,
            memorySizeKb = memorySizeKb,
            iterations = iterations,
            salt = salt.toByteArray(),
            encryptedRandomKey = EncryptedData.decode(encryptedRandomKey),
        )
    }

    companion object {
        fun fromArgon2Params(params: KeyDerivationParams): EncryptionParams = EncryptionParams(
            parallelism = params.parallelism,
            memorySizeKb = params.memorySizeKb,
            iterations = params.iterations,
            salt = params.salt.toBase64Url(),
            encryptedRandomKey = params.encryptedRandomKey.encode(),
        )
    }
}

@Serializable
data class VaultExport(
    val secrets: List<TotpSecret>,
    val passkeys: List<Passkey>,
) {
    fun encode(): ByteArray =
        json.encodeToString(this).toByteArray(Charsets.UTF_8)

    companion object {
        fun decode(data: ByteArray) =
            json.decodeFromString<VaultExport>(data.toString(Charsets.UTF_8))
    }
}

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class UnknownExportFormatException(
    val latestSupportedFormat: Int,
    val actualFormat: Int,
) : Exception()

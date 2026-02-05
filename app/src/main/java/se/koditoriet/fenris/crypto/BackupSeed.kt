package se.koditoriet.fenris.crypto

import android.net.Uri
import androidx.core.net.toUri
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.codec.Base64Url.Companion.toBase64Url
import java.security.MessageDigest
import java.security.MessageDigest.getInstance
import java.security.SecureRandom

private const val BACKUP_KEY_SIZE: Int = 32
private const val DOMAIN_BACKUP_SECRET_DEK: String = "backup_secret_dek"
private const val DOMAIN_BACKUP_METADATA_DEK: String = "backup_metadata_dek"

class BackupSeed(private val secret: ByteArray) {
    fun deriveBackupSecretKey(): ByteArray =
        deriveKey(DOMAIN_BACKUP_SECRET_DEK)

    fun deriveBackupMetadataKey(): ByteArray =
        deriveKey(DOMAIN_BACKUP_METADATA_DEK)

    fun wipe() {
        secret.fill(0)
    }

    fun toMnemonic(): List<String> {
        val checksum = getInstance("SHA-256").digest(secret).take(1)
        return (secret + checksum).bitChunks(11).map { wordList[it] }
    }

    fun toBase64Url(): Base64Url =
        secret.toBase64Url()

    fun toUri(): Uri =
        "fenris://seed/${toBase64Url().string}".toUri()

    private fun deriveKey(domain: String): ByteArray {
        val prk = HmacContext.create(ByteArray(BACKUP_KEY_SIZE), HmacAlgorithm.SHA256).run {
            hmac(secret)
        }
        return HmacContext.create(prk, HmacAlgorithm.SHA256).run {
            hmac(domain.toByteArray() + byteArrayOf(1))
        }
    }

    companion object {
        const val URI_PREFIX: String = "seed"

        fun generate(secureRandom: SecureRandom = SecureRandom()): BackupSeed =
            BackupSeed(ByteArray(BACKUP_KEY_SIZE).apply(secureRandom::nextBytes))

        fun fromUri(uri: Uri): BackupSeed {
            require(uri.scheme == "fenris")
            require(uri.host == URI_PREFIX)
            require(uri.pathSegments.size == 1)
            val base64url = Base64Url.fromBase64UrlString(uri.pathSegments.first())
            return BackupSeed(base64url.toByteArray())
        }

        fun fromMnemonic(words: List<String>): BackupSeed {
            require(words.size == 24)
            val bitWriter = BitWriter()
            for (word in words) {
                val index = wordMap[word.lowercase().trim()] ?: throw AssertionError(
                    "'${word.lowercase().trim()}' is not a valid seed word"
                )
                bitWriter.write(index.shr(8).toByte(), 3)
                bitWriter.write(index.toByte(), 8)
            }
            val mnemonicBytes = bitWriter.getBytes()
            val secretBytes = mnemonicBytes.take(BACKUP_KEY_SIZE).toByteArray()
            val expectedChecksum = mnemonicBytes.drop(BACKUP_KEY_SIZE).first()
            val actualChecksum = MessageDigest
                .getInstance("SHA-256")
                .digest(secretBytes)
                .take(1)
                .first()
            if (expectedChecksum != actualChecksum) {
                throw IllegalArgumentException("checksum mismatch; mnemonic is corrupted")
            }
            return BackupSeed(secretBytes)
        }
    }
}

package se.koditoriet.fenris.crypto

import android.net.Uri
import androidx.core.net.toUri
import se.koditoriet.fenris.BACKUP_SEED_MNEMONIC_LENGTH_WORDS
import se.koditoriet.fenris.BACKUP_SEED_URI_HOST
import se.koditoriet.fenris.FENRIS_URI_SCHEME
import se.koditoriet.fenris.SYMMETRIC_KEY_SIZE
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.codec.Base64Url.Companion.toBase64Url
import java.security.MessageDigest
import java.security.MessageDigest.getInstance
import java.security.SecureRandom

private const val DOMAIN_BACKUP_SECRET_DEK: String = "backup_secret_dek"

class BackupSeed(private val secret: ByteArray) {
    fun deriveBackupKey(): ByteArray =
        deriveKey(DOMAIN_BACKUP_SECRET_DEK)

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
        "$FENRIS_URI_SCHEME://$BACKUP_SEED_URI_HOST/${toBase64Url().string}".toUri()

    private fun deriveKey(domain: String): ByteArray =
        hkdf(secret, domain)

    companion object {
        fun generate(secureRandom: SecureRandom = SecureRandom()): BackupSeed =
            BackupSeed(ByteArray(SYMMETRIC_KEY_SIZE).apply(secureRandom::nextBytes))

        fun fromUri(uri: Uri): BackupSeed {
            require(uri.scheme == FENRIS_URI_SCHEME)
            require(uri.host == BACKUP_SEED_URI_HOST)
            require(uri.pathSegments.size == 1)
            val base64url = Base64Url.fromBase64UrlString(uri.pathSegments.first())
            val bytes = base64url.toByteArray()
            require(bytes.size == SYMMETRIC_KEY_SIZE)
            return BackupSeed(bytes)
        }

        fun fromMnemonic(words: List<String>): BackupSeed {
            require(words.size == BACKUP_SEED_MNEMONIC_LENGTH_WORDS)
            val bitWriter = BitWriter()
            for (word in words) {
                val index = wordMap[word.lowercase().trim()] ?: throw IllegalArgumentException(
                    "'${word.lowercase().trim()}' is not a valid seed word"
                )
                bitWriter.write(index.shr(8).toByte(), 3)
                bitWriter.write(index.toByte(), 8)
            }
            val mnemonicBytes = bitWriter.getBytes()
            val secretBytes = mnemonicBytes.take(SYMMETRIC_KEY_SIZE).toByteArray()
            val expectedChecksum = mnemonicBytes.drop(SYMMETRIC_KEY_SIZE).first()
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

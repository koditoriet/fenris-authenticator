package se.koditoriet.fenris.crypto

import android.util.Log
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import se.koditoriet.fenris.PASSWORD_SALT_SIZE
import se.koditoriet.fenris.SYMMETRIC_KEY_SIZE
import se.koditoriet.fenris.crypto.types.EncryptedData
import java.security.SecureRandom

private const val TAG = "BackupKeyDerivation"

/**
 * Derive a data encryption key for backups.
 * The associated KeyDerivationParameters and password will be needed to recover the key.
 */
fun EncryptionContext.deriveBackupKey(password: String): Pair<KeyDerivationParams, ByteArray> {
    val rng = SecureRandom()
    val salt = ByteArray(PASSWORD_SALT_SIZE).apply { rng.nextBytes(this) }
    val randomKey = ByteArray(SYMMETRIC_KEY_SIZE).apply { rng.nextBytes(this) }

    try {
        val params = KeyDerivationParams.defaultParams(salt, encrypt(randomKey))
        return Pair(params, deriveBackupKey(params, randomKey, password))
    } finally {
        randomKey.fill(0)
    }
}

/**
 * Recover data encryption key for backups from the given key derivation parameters and password.
 */
fun DecryptionContext.deriveBackupKey(params: KeyDerivationParams, password: String): ByteArray {
    val randomKey = decrypt(params.encryptedRandomKey)
    try {
        return deriveBackupKey(params, randomKey, password)
    } finally {
        randomKey.fill(0)
    }
}

private fun deriveBackupKey(params: KeyDerivationParams, randomKey: ByteArray, password: String): ByteArray {
    check(randomKey.size >= SYMMETRIC_KEY_SIZE)
    Log.d(
        TAG,
        "Deriving backup key with parameters t=${params.iterations}/m=${params.memorySizeKb}/p=${params.parallelism}"
    )
    val passwordPart = Argon2Kt().run {
        val hashResult = hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password.toByteArray(Charsets.UTF_8),
            salt = params.salt,
            tCostInIterations = params.iterations,
            mCostInKibibyte = params.memorySizeKb,
            parallelism = params.parallelism,
            hashLengthInBytes = SYMMETRIC_KEY_SIZE,
            version = Argon2Version.V13,
        )
        hashResult.rawHashAsByteArray()
    }

    try {
        return hkdf(randomKey + passwordPart)
    } finally {
        passwordPart.fill(0)
    }
}

data class KeyDerivationParams(
    val parallelism: Int,
    val memorySizeKb: Int,
    val iterations: Int,
    val salt: ByteArray,
    val encryptedRandomKey: EncryptedData,
) {
    companion object {
        fun defaultParams(
            salt: ByteArray,
            encryptedRandomKey: EncryptedData,
        ): KeyDerivationParams = KeyDerivationParams(
            parallelism = 4,
            memorySizeKb = 1024 * 1024,
            iterations = 4,
            salt = salt,
            encryptedRandomKey = encryptedRandomKey,
        )
    }
}

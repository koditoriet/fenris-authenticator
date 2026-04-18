package se.koditoriet.fenris.crypto

import okio.withLock
import se.koditoriet.fenris.crypto.types.EncryptedData
import se.koditoriet.fenris.crypto.types.EncryptionAlgorithm
import java.security.SecureRandom
import java.util.concurrent.locks.ReentrantLock

class EphemeralSymmetricKey(
    val algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_GCM,
    private val secureRandom: SecureRandom = SecureRandom()
) {
    private val lock: ReentrantLock = ReentrantLock()
    private var _keyMaterial: ByteArray? = null
    private val keyMaterial: ByteArray
        get() = lock.withLock {
            _keyMaterial ?: ByteArray(algorithm.keySizeBytes).apply(secureRandom::nextBytes).also {
                _keyMaterial = it
            }
        }

    fun encrypt(data: ByteArray): EncryptedData =
        EncryptionContext.create(keyMaterial, algorithm).encrypt(data)

    fun decrypt(data: EncryptedData): ByteArray =
        DecryptionContext.create(keyMaterial, algorithm).decrypt(data)

    fun wipe() {
        _keyMaterial?.fill(0)
        _keyMaterial = null
    }

    protected fun finalize() {
        wipe()
    }
}

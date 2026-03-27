package se.koditoriet.fenris.crypto

import se.koditoriet.fenris.crypto.types.ECAlgorithm
import se.koditoriet.fenris.crypto.types.EncryptedData
import se.koditoriet.fenris.crypto.types.EncryptionAlgorithm
import se.koditoriet.fenris.crypto.types.HmacAlgorithm
import java.security.Key
import java.security.PrivateKey
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface HmacContext {
    fun hmac(data: ByteArray): ByteArray

    companion object {
        fun create(key: Key, algorithm: HmacAlgorithm): HmacContext =
            HmacContextImpl(key, algorithm)

        fun create(key: ByteArray, algorithm: HmacAlgorithm): HmacContext =
            SecretKeySpec(key, algorithm.algorithmName).run { HmacContextImpl(this, algorithm) }
    }
}

interface SignatureContext {
    fun sign(data: ByteArray): ByteArray

    companion object {
        fun create(privateKey: PrivateKey, algorithm: ECAlgorithm): SignatureContext =
            Signature.getInstance(algorithm.algorithmName).run {
                initSign(privateKey)
                SignatureContextImpl(this)
            }

        fun create(sig: Signature): SignatureContext =
            SignatureContextImpl(sig)
    }
}

interface EncryptionContext {
    fun encrypt(data: ByteArray, iv: ByteArray? = null): EncryptedData

    companion object {
        fun create(key: Key, algorithm: EncryptionAlgorithm): EncryptionContext =
            SymmetricContextImpl(key, algorithm)

        fun create(key: ByteArray, algorithm: EncryptionAlgorithm): EncryptionContext =
            SecretKeySpec(key, algorithm.algorithmName).run { SymmetricContextImpl(this, algorithm) }
    }
}

interface DecryptionContext {
    fun decrypt(data: EncryptedData): ByteArray

    companion object {
        fun create(key: Key, algorithm: EncryptionAlgorithm): DecryptionContext =
            SymmetricContextImpl(key, algorithm)

        fun create(keyMaterial: ByteArray, algorithm: EncryptionAlgorithm): DecryptionContext {
            val key = SecretKeySpec(keyMaterial, algorithm.secretKeySpecName)
            return SymmetricContextImpl(key, algorithm)
        }
    }
}

private class HmacContextImpl(
    private val key: Key,
    private val algorithm: HmacAlgorithm,
) : HmacContext {
    override fun hmac(data: ByteArray): ByteArray = Mac.getInstance(algorithm.algorithmName).run {
        init(key)
        doFinal(data)
    }
}

private class SymmetricContextImpl(
    private val key: Key,
    private val algorithm: EncryptionAlgorithm,
) : EncryptionContext, DecryptionContext {
    override fun encrypt(data: ByteArray, iv: ByteArray?): EncryptedData =
        Cipher.getInstance(algorithm.algorithmName).run {
            init(Cipher.ENCRYPT_MODE, key)
            val ciphertext = doFinal(data)
            EncryptedData.from(iv ?: this.iv, ciphertext)
        }

    override fun decrypt(data: EncryptedData): ByteArray = Cipher.getInstance(algorithm.algorithmName).run {
        val gcmSpec = GCMParameterSpec(128, data.iv.asBytes)
        init(Cipher.DECRYPT_MODE, key, gcmSpec)
        doFinal(data.ciphertext.asBytes)
    }
}

class SignatureContextImpl(
    private val sig: Signature,
) : SignatureContext {
    override fun sign(data: ByteArray): ByteArray {
        sig.update(data)
        return sig.sign()
    }
}

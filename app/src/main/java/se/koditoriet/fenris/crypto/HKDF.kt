package se.koditoriet.fenris.crypto

import se.koditoriet.fenris.SYMMETRIC_KEY_SIZE
import se.koditoriet.fenris.crypto.types.HmacAlgorithm

/**
 * Derives a 256 bit key using an all zero salt from the given input key material and domain.
 */
fun hkdf(keyMaterial: ByteArray, domain: String = ""): ByteArray {
    val prk = HmacContext.create(ByteArray(SYMMETRIC_KEY_SIZE), HmacAlgorithm.SHA256).run {
        hmac(keyMaterial)
    }
    return HmacContext.create(prk, HmacAlgorithm.SHA256).run {
        hmac(domain.toByteArray() + byteArrayOf(1))
    }
}

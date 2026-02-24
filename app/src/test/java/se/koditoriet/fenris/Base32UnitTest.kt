package se.koditoriet.fenris

import org.junit.Test

import org.junit.Assert.*
import se.koditoriet.fenris.codec.base32Decode
import se.koditoriet.fenris.codec.isValidBase32
import kotlin.random.Random

class Base32UnitTest {
    private val validBase32Examples = listOf(
        "NBSWY3DPEB3W64TMMQ======" to "hello world".encodeToByteArray(),
        "NBSWY3DPEB3W64TMMQ" to "hello world".encodeToByteArray(),
        "nbswy3dpeb3w64tmmq" to "hello world".encodeToByteArray(),
        "nBSwY3dpeb3w64tmMq" to "hello world".encodeToByteArray(),
        "" to byteArrayOf(),
        "===" to byteArrayOf(),
        "PA" to "x".encodeToByteArray(),
        "AAAA" to byteArrayOf(0, 0),
        "MFRGGZDF" to "abcde".encodeToByteArray(),
    )

    @Test
    fun `base32 can decode base32`() {
        for ((base32, bytes) in validBase32Examples) {
            assertArrayEquals(bytes, base32Decode(base32.toCharArray()))
        }
    }

    @Test
    fun `isValidBase32 accepts valid base32 strings`() {
        for ((base32, _) in validBase32Examples) {
            assertTrue(isValidBase32(base32))
        }
    }

    @Test
    fun `isValidBase32 rejects invalid base32 strings`() {
        assertFalse(isValidBase32("HEL1O"))
        assertFalse(isValidBase32("hel1o"))
        assertFalse(isValidBase32("-"))
        assertFalse(isValidBase32("MFRG0GZDF"))
    }
}

package se.koditoriet.fenris

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.codec.Base64Url.Companion.toBase64Url
import kotlin.random.Random

class Base64UrlUnitTest {
    private val rng = Random(42)

    @Test
    fun `toByteArray and toBase64Url are inverses`() {
        for (n in 1..10_000) {
            val bytes = rng.nextBytes(rng.nextInt(0, 100))
            val base64Url = bytes.toBase64Url()
            val decodedBytes = base64Url.toByteArray()
            assertArrayEquals(
                bytes,
                decodedBytes,
            )
        }
    }

    @Test
    fun `string and fromBase64UrlString are inverses`() {
        for (n in 1..10_000) {
            val bytes = rng.nextBytes(rng.nextInt(0, 100))
            val base64Url = bytes.toBase64Url()
            val string = base64Url.string
            val decodedBase64Url = Base64Url.fromBase64UrlString(string)
            assertEquals(
                base64Url,
                decodedBase64Url,
            )
        }
    }
}

package se.koditoriet.fenris

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import se.koditoriet.fenris.crypto.BackupSeed
import java.security.SecureRandom
import kotlin.random.Random

class BackupSeedUnitTest {
    // Android's SecureRandom refuses to be deterministic, so let's just mock it
    private class DeterministicSecureRandom(private val rng: Random) : SecureRandom() {
        override fun nextBytes(bytes: ByteArray?) {
            require(bytes != null)
            rng.nextBytes(bytes)
        }
    }

    private val rng = DeterministicSecureRandom(Random(42))


    @Test
    fun `wipe actually wipes seed`() {
        val emptySeed = BackupSeed(ByteArray(32))
        val seed = BackupSeed.generate(rng)
        seed.wipe()
        assertArrayEquals(
            emptySeed.toBase64Url().toByteArray(),
            seed.toBase64Url().toByteArray(),
        )
    }

    @Test
    fun `generate doesnt produce the same seed twice`() {
        val seeds = mutableSetOf<ByteArray>()
        for (n in 1..10_000) {
            seeds.add(BackupSeed.generate(rng).toBase64Url().toByteArray())
        }
        assertEquals(10_000, seeds.size)
    }

    @Test
    fun `generate is deterministic given the same rng`() {
        val r1 = DeterministicSecureRandom(Random(42))
        val r2 = DeterministicSecureRandom(Random(42))
        val seed1 = BackupSeed.generate(r1).toBase64Url().toByteArray()
        val seed2 = BackupSeed.generate(r2).toBase64Url().toByteArray()
        assertArrayEquals(seed1, seed2)
    }

    @Test
    fun `mnemonic encoding and decoding are inverses`() {
        for (n in 1..10_000) {
            val seed = BackupSeed.generate(rng)
            val mnemonic = seed.toMnemonic()
            val decodedSeed = BackupSeed.fromMnemonic(mnemonic)
            assertArrayEquals(
                seed.toBase64Url().toByteArray(),
                decodedSeed.toBase64Url().toByteArray(),
            )
        }
    }

    @Test
    fun `uri encoding and decoding are inverses`() {
        for (n in 1..10_000) {
            val seed = BackupSeed.generate(rng)
            val uri = seed.toUri()
            val decodedSeed = BackupSeed.fromUri(uri)
            assertArrayEquals(
                seed.toBase64Url().toByteArray(),
                decodedSeed.toBase64Url().toByteArray(),
            )
        }
    }

    @Test
    fun `mnemonic is always the same length`() {
        for (n in 1..10_000) {
            val seed = BackupSeed.generate(rng)
            val mnemonic = seed.toMnemonic()
            assertEquals(BackupSeed.MNEMONIC_LENGTH_WORDS, mnemonic.size)
        }
    }

    @Test
    fun `derived keys in different domains are always different`() {
        for (n in 1..10_000) {
            val seed = BackupSeed.generate(rng)
            val secretKey = seed.deriveBackupSecretKey()
            val metadataKey = seed.deriveBackupMetadataKey()
            assertNotEquals(secretKey.toHexString(), metadataKey.toHexString())
        }
    }

    @Test
    fun `invalid uris are rejected by fromUri`() {
        val seed = BackupSeed.generate(rng)
        val uri = seed.toUri()
        val base64Url = seed.toBase64Url()
        assertThrows("wrong scheme", IllegalArgumentException::class.java) {
            BackupSeed.fromUri(uri.buildUpon().scheme("http").build())
        }
        assertThrows("wrong authority", IllegalArgumentException::class.java) {
            BackupSeed.fromUri(uri.buildUpon().authority("foo").build())
        }
        assertThrows("wrong number of path segments", IllegalArgumentException::class.java) {
            BackupSeed.fromUri(uri.buildUpon().path("${base64Url.string}/${base64Url.string}").build())
        }
        assertThrows("seed is not a valid base64url", IllegalArgumentException::class.java) {
            BackupSeed.fromUri(uri.buildUpon().path("***").build())
        }
        assertThrows("wrong seed size", IllegalArgumentException::class.java) {
            BackupSeed.fromUri(uri.buildUpon().path("wrongsize").build())
        }
    }

    @Test
    fun `invalid mnemonics are rejected by fromMnemonic`() {
        val seed = BackupSeed.generate(rng)
        val mnemonic = seed.toMnemonic()
        assertThrows("wrong number of words", IllegalArgumentException::class.java) {
            BackupSeed.fromMnemonic(mnemonic.drop(1))
        }
        assertThrows("word not on word list", IllegalArgumentException::class.java) {
            BackupSeed.fromMnemonic(mnemonic.mapIndexed { i, x -> if (i == 5) "bleh" else x })
        }
        assertThrows("replaced word", IllegalArgumentException::class.java) {
            BackupSeed.fromMnemonic(mnemonic.mapIndexed { i, x -> if (i == 5) mnemonic[0] else x })
        }
    }
}

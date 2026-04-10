package se.koditoriet.fenris

import junit.framework.AssertionFailedError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import se.koditoriet.fenris.crypto.Cryptographer
import se.koditoriet.fenris.crypto.DummyAuthenticator
import se.koditoriet.fenris.crypto.types.HmacAlgorithm
import java.security.InvalidKeyException

@OptIn(ExperimentalCoroutinesApi::class)
class CryptographerUnitTest {
    private val dummyAuthenticatorFactory = DummyAuthenticator.Factory()
    private val dummyAuthenticator = dummyAuthenticatorFactory.withReason("", "")

    @Test
    fun `can create use and delete encryption keys`() = runTest {
        val cryptographer = Cryptographer()
        val keys = (0..5).map {
            cryptographer.storeSymmetricKey(
                keyIdentifier = null,
                allowDecrypt = true,
                allowDeviceCredential = true,
                requiresAuthentication = false,
                keyMaterial = ByteArray(SYMMETRIC_KEY_SIZE),
            )
        }

        val deletedKeyIndex = 2
        cryptographer.deleteKey(keys[deletedKeyIndex])

        val expectedPlaintext = "hello".toByteArray()
        keys.forEach { key ->
            try {
                val ciphertext = cryptographer.withEncryptionKey(dummyAuthenticator, key) {
                    encrypt(expectedPlaintext)
                }

                val actualPlaintext = cryptographer.withDecryptionKey(dummyAuthenticator, key) {
                    decrypt(ciphertext)
                }

                assertEquals(
                    expectedPlaintext.toHexString(),
                    actualPlaintext.toHexString(),
                )
            } catch (_: IllegalArgumentException) {
                if (key == keys[deletedKeyIndex]) {
                    // great, this one's supposed to be missing!
                } else {
                    throw AssertionFailedError("could perform operations over deleted key")
                }
            }
        }

        cryptographer.wipeKeys()
        keys.forEach { key ->
            try {
                cryptographer.withKey(dummyAuthenticator, key) {
                    throw AssertionFailedError("could perform operations over deleted key")
                }
            } catch (_: IllegalArgumentException) {
                // great, all keys are supposed to be gone!
            }
        }
    }

    @Test
    fun `hmac key produces correct hmac`() = runTest {
        val cryptographer = Cryptographer()
        val message = "hello".toByteArray()
        val hmacSums = listOf(
            "e50e18e1fd9aba78bbc54b712c507c849b837f41",
            "ee10f25230525db2a6b8d2cfc7bcc3bca86595933aea58f2210388eb80d76e2f",
            "0c383202f42a52f1522cf80b78e898271ca099dabcff9cc56cfe133423a15c48e0fd7263db6ddd70c97d6388e3d0a2f0370be715837ca0e86852b1b57a66fa22",
        )
        HmacAlgorithm.entries.zip(hmacSums).forEach { (algorithm, expectedHmac) ->
            val key = cryptographer.storeHmacKey(
                keyIdentifier = null,
                hmacAlgorithm = algorithm,
                allowDeviceCredential = true,
                requiresAuthentication = false,
                keyMaterial = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx".toByteArray(),
            )
            val actualHmac = cryptographer.withHmacKey(dummyAuthenticator, key) {
                hmac(message)
            }
            assertEquals(
                expectedHmac,
                actualHmac.toHexString(),
            )
        }
    }

    @Test
    fun `symmetric key respects decrypt restriction`() = runTest {
        val cryptographer = Cryptographer()
        val key = cryptographer.storeSymmetricKey(
            keyIdentifier = null,
            allowDecrypt = false,
            allowDeviceCredential = true,
            requiresAuthentication = false,
            keyMaterial = ByteArray(SYMMETRIC_KEY_SIZE),
        )

        val expectedPlaintext = "hello".toByteArray()
        val ciphertext = cryptographer.withEncryptionKey(dummyAuthenticator, key) {
            encrypt(expectedPlaintext)
        }

        try {
            cryptographer.withDecryptionKey(dummyAuthenticator, key) {
                decrypt(ciphertext)
            }
            throw AssertionFailedError("withDecryptionKey did not raise an exception")
        } catch (_: InvalidKeyException) {
            // all clear!
        }
    }
}

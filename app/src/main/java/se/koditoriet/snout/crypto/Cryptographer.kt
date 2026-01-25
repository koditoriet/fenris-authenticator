package se.koditoriet.snout.crypto

import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import android.util.Log
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
import org.bouncycastle.operator.bc.BcECContentSignerBuilder
import java.math.BigInteger
import java.security.Key
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.PrivateKey
import java.security.ProviderException
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Calendar
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.DestroyFailedException


private const val KEY_AUTHENTICATION_LIFETIME: Int = 1
private const val TAG = "Cryptographer"

class Cryptographer(
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) },
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun wipeKeys() =
        keyStore.aliases().iterator().forEach { keyStore.deleteEntry(it) }

    fun isInitialized(): Boolean =
        keyStore.aliases().hasMoreElements()

    @Suppress("UNCHECKED_CAST")
    fun <T : KeyAlgorithm> getKeySecurityLevel(keyHandle: KeyHandle<T>): KeySecurityLevel {
        val keyInfo = when (keyHandle.usage) {
            KeyUsage.Sign -> getPrivateKeyInfo(keyHandle as KeyHandle<ECAlgorithm>)
            else -> getSecretKeyInfo(keyHandle as KeyHandle<SymmetricAlgorithm>)
        }

        check(keyInfo != null) {
            "key '${keyHandle.alias}' does not exist"
        }

        return try {
            when (keyInfo.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> KeySecurityLevel.StrongBox
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> KeySecurityLevel.TEE
                KeyProperties.SECURITY_LEVEL_SOFTWARE -> KeySecurityLevel.Software
                else -> {
                    error("'${keyInfo.securityLevel}' is not a valid security level!")
                }
            }
        } catch (_: ProviderException) {
            when (keyHandle.isStrongBoxBacked) {
                true -> KeySecurityLevel.StrongBox
                false -> KeySecurityLevel.Unknown
            }
        }
    }

    private fun getSecretKeyInfo(keyHandle: KeyHandle<SymmetricAlgorithm>): KeyInfo? =
        (keyStore.getEntry(keyHandle.alias, null) as? KeyStore.SecretKeyEntry)?.let {
            val factory = SecretKeyFactory.getInstance(keyHandle.algorithm.secretKeySpecName, "AndroidKeyStore")
            factory.getKeySpec(it.secretKey, KeyInfo::class.java) as KeyInfo
        }

    private fun getPrivateKeyInfo(keyHandle: KeyHandle<ECAlgorithm>): KeyInfo? =
        (keyStore.getEntry(keyHandle.alias, null) as? KeyStore.PrivateKeyEntry)?.let {
            val factory = KeyFactory.getInstance(keyHandle.algorithm.secretKeySpecName, "AndroidKeyStore")
            factory.getKeySpec(it.privateKey, KeyInfo::class.java) as KeyInfo
        }

    suspend fun <T> withHmacKey(
        authenticator: Authenticator,
        keyHandle: KeyHandle<HmacAlgorithm>,
        action: suspend HmacContext.() -> T,
    ): T =
        withKey(authenticator, keyHandle) {
            HmacContext.create(this, keyHandle.algorithm).action()
        }

    suspend fun <T> withEncryptionKey(
        authenticator: Authenticator,
        keyHandle: KeyHandle<EncryptionAlgorithm>,
        action: suspend EncryptionContext.() -> T,
    ): T =
        withKey(authenticator, keyHandle) {
            EncryptionContext.create(this, keyHandle.algorithm).action()
        }

    suspend fun <T> withDecryptionKey(
        authenticator: Authenticator,
        keyHandle: KeyHandle<EncryptionAlgorithm>,
        action: suspend DecryptionContext.() -> T,
    ): T =
        withKey(authenticator, keyHandle) {
            DecryptionContext.create(this, keyHandle.algorithm).action()
        }

    suspend fun <T> withSigningKey(
        authenticator: Authenticator,
        keyHandle: KeyHandle<ECAlgorithm>,
        action: suspend SignatureContext.() -> T,
    ): T =
        (keyStore.getKey(keyHandle.alias, null) as? PrivateKey)?.run {
            if (keyHandle.requiresAuthentication) {
                val sig = Signature.getInstance(keyHandle.algorithm.algorithmName)
                sig.initSign(this)
                authenticator.authenticate(sig) {
                    SignatureContext.create(it).action()
                }
            } else {
                SignatureContext.create(this, keyHandle.algorithm).action()
            }
        } ?: throw IllegalArgumentException("key '${keyHandle.alias}' does not exist")

    suspend fun <T> withDecryptionKey(
        keyMaterial: ByteArray,
        algorithm: EncryptionAlgorithm,
        action: suspend DecryptionContext.() -> T,
    ): T =
        DecryptionContext.create(keyMaterial, algorithm).action()

    suspend fun <T> withEncryptionKey(
        keyMaterial: ByteArray,
        algorithm: EncryptionAlgorithm,
        action: suspend EncryptionContext.() -> T,
    ): T =
        EncryptionContext.create(keyMaterial, algorithm).action()

    suspend fun <T> withKey(
        authenticator: Authenticator,
        keyHandle: KeyHandle<*>,
        action: suspend Key.() -> T,
    ): T =
        keyStore.getKey(keyHandle.alias, null)?.run {
            if (keyHandle.requiresAuthentication) {
                authenticator.authenticate { action() }
            } else {
                action()
            }
        } ?: throw IllegalArgumentException("key '${keyHandle.alias}' does not exist")

    fun deleteKey(keyHandle: KeyHandle<*>): Unit =
        keyStore.deleteEntry(keyHandle.alias)

    fun storeSymmetricKey(
        keyIdentifier: KeyIdentifier?,
        allowDecrypt: Boolean,
        allowDeviceCredential: Boolean,
        requiresAuthentication: Boolean,
        keyMaterial: ByteArray,
        algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_GCM,
    ): KeyHandle<EncryptionAlgorithm> {
        require(keyMaterial.size == algorithm.keySize / 8)

        val keyHandle = KeyHandle(
            usage = if (allowDecrypt) { KeyUsage.EncryptDecrypt } else { KeyUsage.Encrypt },
            algorithm = algorithm,
            requiresAuthentication = requiresAuthentication,
            isStrongBoxBacked = true,
            identifier = keyIdentifier ?: randomKeyIdentifier(),
        )

        return storeSymmetricKey(keyHandle, keyMaterial, allowDeviceCredential) {
            setBlockModes(keyHandle.algorithm.blockMode)
            setEncryptionPaddings(keyHandle.algorithm.paddingScheme)
        }
    }

    fun storeHmacKey(
        keyIdentifier: KeyIdentifier?,
        hmacAlgorithm: HmacAlgorithm,
        allowDeviceCredential: Boolean,
        requiresAuthentication: Boolean,
        keyMaterial: ByteArray,
    ): KeyHandle<HmacAlgorithm> {
        val keyHandle = KeyHandle(
            usage = KeyUsage.HMAC,
            algorithm = hmacAlgorithm,
            requiresAuthentication = requiresAuthentication,
            isStrongBoxBacked = true,
            identifier = keyIdentifier ?: randomKeyIdentifier(),
        )

        return storeSymmetricKey(keyHandle, keyMaterial, allowDeviceCredential) {
            setDigests(keyHandle.algorithm.keyStoreDigestName)
        }
    }

    /**
     * Note that this function modifies the key handle to set the correct StrongBox backing state
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : SymmetricAlgorithm> storeSymmetricKey(
        keyHandle: KeyHandle<T>,
        keyMaterial: ByteArray,
        allowDeviceCredential: Boolean,
        setProtectionParams: KeyProtection.Builder.() -> Unit,
    ): KeyHandle<T> =
        keyStore.importKey(
            keyHandle = keyHandle,
            keyEntry = KeyEntry.create(keyHandle as KeyHandle<SymmetricAlgorithm>, keyMaterial),
            allowDeviceCredential = allowDeviceCredential,
            setProtectionParams = setProtectionParams,
        )

    suspend fun generateECKeyPair(
        keyIdentifier: KeyIdentifier?,
        requiresAuthentication: Boolean,
        allowDeviceCredential: Boolean,
        backupKeyHandle: KeyHandle<EncryptionAlgorithm>? = null,
    ): ECKeyPairInfo {
        val preliminaryHandle = KeyHandle(
            usage = KeyUsage.Sign,
            algorithm = ECAlgorithm.ES256,
            requiresAuthentication = requiresAuthentication,
            isStrongBoxBacked = true,
            identifier = keyIdentifier ?: randomKeyIdentifier(),
        )
        Log.i(
            TAG,
            "Generating EC key pair with identifier '${preliminaryHandle.identifier}'" +
            " and algorithm ${preliminaryHandle.algorithm}",
        )
        val keyPair = KeyPairGenerator.getInstance(preliminaryHandle.algorithm.secretKeySpecName).run {
            initialize(ECGenParameterSpec(preliminaryHandle.algorithm.curve))
            generateKeyPair()
        }
        val keyEntry = KeyEntry.create(preliminaryHandle, keyPair)

        val result = ECKeyPairInfo(
            publicKey = keyPair.public as ECPublicKey,
            keyHandle = keyStore.importKey(preliminaryHandle, keyEntry, allowDeviceCredential) {
                setDigests(preliminaryHandle.algorithm.keyStoreDigestName)
            },
            encryptedPrivateKey = backupKeyHandle?.let {
                // use DummyAuthenticator since backup key never requires authentication
                withEncryptionKey(DummyAuthenticator, it) {
                    encrypt(keyPair.private.encoded)
                }
            },
        )

        try {
            Log.d(TAG, "Attempting to destroy private key material")
            keyPair.private.destroy()
        } catch (e: DestroyFailedException) {
            Log.d(TAG, "Destroying private key failed", e)
        }

        return result
    }

    private fun randomKeyIdentifier(): KeyIdentifier {
        val identifierBytes = ByteArray(16)
        secureRandom.nextBytes(identifierBytes)
        return KeyIdentifier.Random(Base64.encodeToString(identifierBytes, Base64.NO_WRAP))
    }
}

enum class KeySecurityLevel {
    StrongBox,
    TEE,
    Software,
    Unknown,
}

data class ECKeyPairInfo(
    val keyHandle: KeyHandle<ECAlgorithm>,
    val publicKey: ECPublicKey,
    val encryptedPrivateKey: EncryptedData?,
)

private sealed interface KeyEntry {
    class Symmetric(val entry: KeyStore.SecretKeyEntry) : KeyEntry
    class Asymmetric(val entry: KeyStore.PrivateKeyEntry) : KeyEntry

    companion object {
        fun create(keyHandle: KeyHandle<SymmetricAlgorithm>, key: ByteArray): KeyEntry =
            create(keyHandle, SecretKeySpec(key, keyHandle.algorithm.secretKeySpecName))

        fun create(keyHandle: KeyHandle<SymmetricAlgorithm>, key: SecretKey): KeyEntry =
            Symmetric(KeyStore.SecretKeyEntry(key))

        fun create(keyHandle: KeyHandle<ECAlgorithm>, keyPair: KeyPair): KeyEntry =
            create(keyHandle, keyPair.private as ECPrivateKey)

        // Source - https://stackoverflow.com/a
        // Posted by markw, modified by community. See post 'Timeline' for change history
        // Retrieved 2026-01-19, License - CC BY-SA 4.0
        fun create(keyHandle: KeyHandle<ECAlgorithm>, privKey: ECPrivateKey): KeyEntry {
            Log.d(TAG, "Reconstructing public key from private key '${keyHandle.alias}'")
            val keyFactory = KeyFactory.getInstance(keyHandle.algorithm.secretKeySpecName, bouncyCastleProvider)
            val ecSpec = ECNamedCurveTable.getParameterSpec(keyHandle.algorithm.curve)
            val q = ecSpec.g.multiply(privKey.s)
            val pubSpec = ECPublicKeySpec(q, ecSpec)
            val publicKey = keyFactory.generatePublic(pubSpec) as ECPublicKey
            val keyPair = KeyPair(publicKey, privKey)
            val cert = generateSelfSignedCertificate(keyHandle, keyPair)
            return Asymmetric(KeyStore.PrivateKeyEntry(privKey, arrayOf(cert)))
        }

        private val bouncyCastleProvider by lazy {
            BouncyCastleProvider()
        }
    }
}

private fun KeyStore.setEntry(keyHandle: KeyHandle<*>, entry: KeyEntry, protParam: KeyStore.ProtectionParameter) {
    when (entry) {
        is KeyEntry.Symmetric -> { setEntry(keyHandle.alias, entry.entry, protParam) }
        is KeyEntry.Asymmetric -> { setEntry(keyHandle.alias, entry.entry, protParam) }
    }
}

// Adapted from https://stackoverflow.com/a/59182063
// Posted by Tolga Okur, modified by community. See post 'Timeline' for change history
// Retrieved 2026-01-17, License - CC BY-SA 4.0
private fun generateSelfSignedCertificate(keyHandle: KeyHandle<ECAlgorithm>, keyPair: KeyPair): X509Certificate {
    Log.d(TAG, "Generating self-signed dummy cert for '${keyHandle.alias}'")
    val sigAlgId = DefaultSignatureAlgorithmIdentifierFinder().find(keyHandle.algorithm.algorithmName)
    val digAlgId = DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
    val keyParam = PrivateKeyFactory.createKey(keyPair.private.encoded)
    val spki = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
    val signer = BcECContentSignerBuilder(sigAlgId, digAlgId).build(keyParam)
    val issuer = X500Name("CN=Snout Authenticator")
    val subject = X500Name("CN=Passkey")
    val serial = BigInteger.valueOf(1)
    val notBefore = Calendar.getInstance()
    val notAfter = Calendar.getInstance()
    notAfter.add(Calendar.YEAR, 1000)

    val v3CertGen = X509v3CertificateBuilder(
        issuer,
        serial,
        notBefore.getTime(),
        notAfter.getTime(),
        subject,
        spki
    )
    val certificateHolder = v3CertGen.build(signer)

    return JcaX509CertificateConverter().getCertificate(certificateHolder)
}

private fun <T : KeyAlgorithm> KeyStore.importKey(
    keyHandle: KeyHandle<T>,
    keyEntry: KeyEntry,
    allowDeviceCredential: Boolean,
    setProtectionParams: KeyProtection.Builder.() -> Unit = {},
): KeyHandle<T> {
    Log.i(TAG, "Importing key to preliminary handle ${keyHandle.alias}")
    val protectionParamsBuilder = KeyProtection.Builder(keyHandle.usage.purposes).apply {
        setProtectionParams()
        setIsStrongBoxBacked(true)
        if (keyHandle.requiresAuthentication) {
            setUserAuthenticationRequired(true)
            var allowedAuthTypes = KeyProperties.AUTH_BIOMETRIC_STRONG
            if (allowDeviceCredential) {
                allowedAuthTypes = allowedAuthTypes or KeyProperties.AUTH_DEVICE_CREDENTIAL
            }

            // timeout needs to be >0 if key is symmetric, since symmetric every symmetric operation we do requires
            // more than one call to Android Key Store
            val timeout = if (keyEntry is KeyEntry.Symmetric) {
                Log.i(
                    TAG,
                    "Key ${keyHandle.alias} is symmetric; setting auth timeout to $KEY_AUTHENTICATION_LIFETIME",
                )
                1
            } else {
                Log.i(
                    TAG,
                    "Key ${keyHandle.alias} is asymmetric; setting auth timeout to 0",
                )
                0
            }
            setUserAuthenticationParameters(timeout, allowedAuthTypes)
        }
    }
    try {
        val updatedKeyHandle = keyHandle.copy(isStrongBoxBacked = true)
        setEntry(updatedKeyHandle, keyEntry, protectionParamsBuilder.build())
        Log.i(
            TAG,
            "Key ${keyHandle.alias} successfully stored in StrongBox",
        )
        return updatedKeyHandle
    } catch (_: KeyStoreException) {
        val updatedKeyHandle = keyHandle.copy(isStrongBoxBacked = false)
        Log.i(
            TAG,
            "Unable to store key ${keyHandle.alias} in StrongBox, retrying without, as ${updatedKeyHandle.alias}",
        )
        protectionParamsBuilder.setIsStrongBoxBacked(false)
        setEntry(updatedKeyHandle, keyEntry, protectionParamsBuilder.build())
        Log.i(
            TAG,
            "Key ${updatedKeyHandle.alias} successfully stored OUTSIDE StrongBox",
        )
        return updatedKeyHandle
    }
}

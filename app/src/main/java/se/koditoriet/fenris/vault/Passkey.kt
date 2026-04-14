package se.koditoriet.fenris.vault

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.crypto.types.ECAlgorithm
import se.koditoriet.fenris.crypto.types.KeyHandle

@Serializable
@Entity(tableName = "passkeys")
data class Passkey(
    @PrimaryKey(autoGenerate = false)
    val credentialId: CredentialId,
    val sortOrder: Long,
    val userId: UserId,
    val userName: String,
    val displayName: String,
    val rpId: String,
    val keyAlias: String,
    val publicKey: Base64Url,
    val encryptedBackupPrivateKey: String?,
    val timeOfCreation: Long,
    val timeOfLastUse: Long? = null,
    val algorithm: PasskeyAlgorithm = PasskeyAlgorithm.ES256,
) {
    val keyHandle: KeyHandle<ECAlgorithm> by lazy {
        KeyHandle.fromAlias(keyAlias)
    }

    /**
     * Pretty-print human-readable description of the passkey
     */
    val description: String by lazy {
        "$rpId \u2022 $userName"
    }
}

@JvmInline
@Serializable
value class CredentialId(val id: Base64Url) {
    val string: String
        get() = id.string

    fun toByteArray(): ByteArray =
        id.toByteArray()

    class TypeConverters {
        @TypeConverter
        fun toId(id: String): CredentialId = fromString(id)

        @TypeConverter
        fun fromId(id: CredentialId): String = id.string
    }

    companion object {
        fun fromString(id: String) =
            CredentialId(Base64Url.fromBase64UrlString(id))
    }
}

@JvmInline
@Serializable
value class UserId(val id: Base64Url) {
    val string: String
        get() = id.string

    fun toByteArray(): ByteArray =
        id.toByteArray()

    class TypeConverters {
        @TypeConverter
        fun toId(id: String): UserId = fromString(id)

        @TypeConverter
        fun fromId(id: UserId): String = id.string
    }

    companion object {
        fun fromString(id: String) =
            UserId(Base64Url.fromBase64UrlString(id))
    }
}

@Serializable
data class NewPasskey(
    val credentialId: ByteArray,
    val rpId: String,
    val userId: ByteArray,
    val userName: String? = null,
    val displayName: String? = null,
    val algorithm: PasskeyAlgorithm = PasskeyAlgorithm.ES256,
    val privateKeyDER: ByteArray,
    val publicKeyDER: ByteArray? = null,
    val timeOfCreation: Long? = null,
    val timeOfLastUse: Long? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewPasskey

        if (timeOfCreation != other.timeOfCreation) return false
        if (timeOfLastUse != other.timeOfLastUse) return false
        if (!credentialId.contentEquals(other.credentialId)) return false
        if (rpId != other.rpId) return false
        if (!userId.contentEquals(other.userId)) return false
        if (userName != other.userName) return false
        if (displayName != other.displayName) return false
        if (algorithm != other.algorithm) return false
        if (!privateKeyDER.contentEquals(other.privateKeyDER)) return false
        if (!publicKeyDER.contentEquals(other.publicKeyDER)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timeOfCreation?.hashCode() ?: 0
        result = 31 * result + (timeOfLastUse?.hashCode() ?: 0)
        result = 31 * result + credentialId.contentHashCode()
        result = 31 * result + rpId.hashCode()
        result = 31 * result + userId.contentHashCode()
        result = 31 * result + (userName?.hashCode() ?: 0)
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + privateKeyDER.contentHashCode()
        result = 31 * result + (publicKeyDER?.contentHashCode() ?: 0)
        return result
    }
}

enum class PasskeyAlgorithm { ES256 }

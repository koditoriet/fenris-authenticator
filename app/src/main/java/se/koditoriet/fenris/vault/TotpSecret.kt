package se.koditoriet.fenris.vault

import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import se.koditoriet.fenris.codec.totpAccount
import se.koditoriet.fenris.codec.totpAlgorithm
import se.koditoriet.fenris.codec.totpDigits
import se.koditoriet.fenris.codec.totpIssuer
import se.koditoriet.fenris.codec.totpPeriod
import se.koditoriet.fenris.codec.totpSecret
import se.koditoriet.fenris.crypto.HmacAlgorithm
import se.koditoriet.fenris.crypto.KeyHandle

@Serializable
@Entity(tableName = "totp_secrets")
data class TotpSecret(
    @PrimaryKey(autoGenerate = true)
    val id: Id,
    val sortOrder: Long,
    val issuer: String,
    val account: String?,
    val digits: Int,
    val period: Int,
    val algorithm: TotpAlgorithm,
    val keyAlias: String,
    val encryptedBackupSecret: String?,
    val timeOfCreation: Long,
    val timeOfLastUse: Long? = null,
) {
    val keyHandle: KeyHandle<HmacAlgorithm> by lazy {
        KeyHandle.fromAlias(keyAlias)
    }

    @Serializable
    @JvmInline
    value class Id(private val id: Int) {
        class TypeConverters {
            @TypeConverter
            fun toId(id: Int): Id= Id(id)

            @TypeConverter
            fun fromId(id: Id): Int = id.id
        }

        companion object {
            val None: Id = Id(0)
        }
    }
}

data class NewTotpSecret(
    val metadata: Metadata,
    val secretData: SecretData,
) {
    data class Metadata(
        val issuer: String,
        val account: String?,
    )

    data class SecretData(
        val secret: CharArray,
        val digits: Int,
        val period: Int,
        val algorithm: TotpAlgorithm,
    )

    companion object {
        fun fromUri(uri: String): NewTotpSecret {
            val parsedUri = uri.toUri()
            require(parsedUri.scheme == "otpauth")
            require(parsedUri.host == "totp")
            return NewTotpSecret(
                metadata = Metadata(
                    issuer = parsedUri.totpIssuer,
                    account = parsedUri.totpAccount,
                ),
                secretData = SecretData(
                    secret = parsedUri.totpSecret.toCharArray(),
                    digits = parsedUri.totpDigits,
                    period = parsedUri.totpPeriod,
                    algorithm = TotpAlgorithm.valueOf(parsedUri.totpAlgorithm)
                )
            )
        }
    }
}

enum class TotpAlgorithm { SHA1, SHA256, SHA512 }

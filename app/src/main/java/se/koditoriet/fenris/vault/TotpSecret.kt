package se.koditoriet.fenris.vault

import android.net.Uri
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
import se.koditoriet.fenris.crypto.types.HmacAlgorithm
import se.koditoriet.fenris.crypto.types.KeyHandle

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
    ) {
        init {
            require(digits in 4..10)
            require(period > 0)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SecretData

            if (digits != other.digits) return false
            if (period != other.period) return false
            if (!secret.contentEquals(other.secret)) return false
            if (algorithm != other.algorithm) return false

            return true
        }

        override fun hashCode(): Int {
            var result = digits
            result = 31 * result + period
            result = 31 * result + secret.contentHashCode()
            result = 31 * result + algorithm.hashCode()
            return result
        }
    }

    companion object {
        fun fromUri(uri: String): NewTotpSecret =
            fromUri(uri.toUri())

        fun fromUri(uri: Uri): NewTotpSecret {
            require(uri.scheme == "otpauth")
            require(uri.host == "totp")
            return NewTotpSecret(
                metadata = Metadata(
                    issuer = uri.totpIssuer,
                    account = uri.totpAccount,
                ),
                secretData = SecretData(
                    secret = uri.totpSecret.toCharArray(),
                    digits = uri.totpDigits,
                    period = uri.totpPeriod,
                    algorithm = TotpAlgorithm.valueOf(uri.totpAlgorithm)
                )
            )
        }
    }
}

enum class TotpAlgorithm { SHA1, SHA256, SHA512 }

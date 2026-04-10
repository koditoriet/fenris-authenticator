package se.koditoriet.fenris.importformat

import android.graphics.BitmapFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import se.koditoriet.fenris.codec.QRCodeData
import se.koditoriet.fenris.codec.QRCodeReader
import se.koditoriet.fenris.codec.base32Encode
import se.koditoriet.fenris.vault.NewTotpSecret
import se.koditoriet.fenris.vault.TotpAlgorithm

object GoogleAuthenticatorDecoder : ImportFormatDecoder {
    override val formatName = "Google Authenticator"
    override val formatMimeTypes = setOf("image/png", "image/jpeg")

    override fun decode(bytes: ByteArray): ImportFormatDecoder.DecodedImport {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        require(bitmap != null) {
            "file is not a supported image type"
        }

        val qrCodePayload = QRCodeReader.tryScanBitmap(bitmap)
        require(qrCodePayload != null) {
            "image does not contain a recognizable qr code"
        }

        val qrCodeData = QRCodeData.parse(qrCodePayload)
        require(qrCodeData is QRCodeData.GoogleAuthenticatorExport) {
            "qr code does not contain a google authenticator export"
        }

        return decodeProtobufPayload(qrCodeData.protobufPayload)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun decodeProtobufPayload(bytes: ByteArray): ImportFormatDecoder.DecodedImport {
        val payload = ProtoBuf.decodeFromByteArray<MigrationPayload>(bytes)
        val (totpSecrets, incompatible) = payload.otpParameters.partition {
            it.type == MigrationPayload.OtpParameters.OtpType.TOTP &&
                    it.algorithm in supportedTotpAlgorithms
        }
        return ImportFormatDecoder.DecodedImport(
            totpSecrets = totpSecrets.map { it.toNewTotpSecret() },
            incompatible = incompatible.map { it.toIncompatibleItem() }
        )
    }
}

private val supportedTotpAlgorithms = setOf(
    MigrationPayload.OtpParameters.Algorithm.SHA1,
    MigrationPayload.OtpParameters.Algorithm.SHA256,
    MigrationPayload.OtpParameters.Algorithm.SHA512,
)

@Serializable
private data class MigrationPayload(
    val otpParameters: List<OtpParameters> = emptyList(),
    val version: Int?,
    val batchSize: Int?,
    val batchIndex: Int?,
    val batchId: Int?,
) {
    @Serializable
    class OtpParameters(
        val secret: ByteArray? = null,
        val name: String? = null,
        val issuer: String? = null,
        val algorithm: Algorithm? = null,
        val digits: DigitCount? = null,
        val type: OtpType? = null,
        val counter: Long? = null,
    ) {
        enum class Algorithm {
            ALGORITHM_TYPE_UNSPECIFIED,
            SHA1,
            SHA256,
            SHA512,
            MD5;

            fun toTotpAlgorithm(): TotpAlgorithm = when (this) {
                ALGORITHM_TYPE_UNSPECIFIED -> TotpAlgorithm.SHA1
                SHA1 -> TotpAlgorithm.SHA1
                SHA256 -> TotpAlgorithm.SHA256
                SHA512 -> TotpAlgorithm.SHA512
                MD5 -> error("unreachable")
            }
        }

        enum class DigitCount {
            DIGIT_COUNT_UNSPECIFIED,
            SIX,
            EIGHT;

            fun toInt(): Int = when (this) {
                DIGIT_COUNT_UNSPECIFIED -> 6
                SIX -> 6
                EIGHT -> 8
            }
        }

        enum class OtpType {
            OTP_TYPE_UNSPECIFIED,
            HOTP,
            TOTP,
        }

        fun toNewTotpSecret(): NewTotpSecret {
            return NewTotpSecret(
                metadata = NewTotpSecret.Metadata(
                    issuer = issuer ?: "",
                    account = name,
                ),
                secretData = NewTotpSecret.SecretData(
                    secret = base32Encode(secret!!),
                    digits = digits?.toInt() ?: 6,
                    period = 30,
                    algorithm = algorithm?.toTotpAlgorithm() ?: TotpAlgorithm.SHA1,
                )
            )
        }

        fun toIncompatibleItem(): ImportFormatDecoder.DecodedImport.IncompatibleItem =
            ImportFormatDecoder.DecodedImport.IncompatibleItem(
                displayName = "$issuer ($name)",
                type = when (type) {
                    OtpType.OTP_TYPE_UNSPECIFIED -> ImportFormatDecoder.DecodedImport.IncompatibleItem.Type.Other
                    OtpType.HOTP -> ImportFormatDecoder.DecodedImport.IncompatibleItem.Type.HOTP
                    OtpType.TOTP -> ImportFormatDecoder.DecodedImport.IncompatibleItem.Type.TOTP
                    null -> ImportFormatDecoder.DecodedImport.IncompatibleItem.Type.Other
                },
            )
    }
}

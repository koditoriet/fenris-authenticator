package se.koditoriet.fenris.codec

import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import se.koditoriet.fenris.vault.NewTotpSecret
import kotlin.math.min

object QRCodeReader {
    private val reader: MultiFormatReader by lazy {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true,
                )
            )
        }
    }

    /**
     * Try to scan an image from camera.
     * Image will be cropped to the center 2/3rds.
     */
    fun tryScanImage(image: Image): String? = try {
        val bitmap = createBinaryBitmap(image)
        reader.decode(bitmap)?.text
    } catch (_: Exception) {
        null
    }

    /**
     * Try to scan a bitmap image.
     * The entire image is scanned; no cropping.
     */
    fun tryScanBitmap(bitmap: Bitmap): String? = try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        reader.decode(binaryBitmap)?.text
    } catch (_: Exception) {
        null
    }

    private fun createBinaryBitmap(image: Image): BinaryBitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width = image.width
        val height = image.height

        val size = min(width, height) * 2 / 3
        val left = (width - size) / 2
        val top = (height - size) / 2

        val source = PlanarYUVLuminanceSource(
            bytes, width, height,
            left, top, size, size, false
        )
        return BinaryBitmap(HybridBinarizer(source))
    }
}

sealed interface QRCodeData {
    class TotpSecret(val newTotpSecret: NewTotpSecret) : QRCodeData
    class FidoRequest(val request: Uri) : QRCodeData
    class GoogleAuthenticatorExport(val protobufPayload: ByteArray) : QRCodeData

    companion object {
        fun parse(s: String): QRCodeData {
            val uri = s.toUri()
            return when (uri.scheme?.lowercase()) {
                "otpauth" -> { TotpSecret(NewTotpSecret.fromUri(uri)) }
                "fido" -> { FidoRequest(uri) }
                "otpauth-migration" -> {
                    require(uri.host == "offline") { "invalid otpauth-migration uri host part" }
                    val base64Data = uri.getQueryParameter("data")
                        ?: throw IllegalArgumentException("invalid otpauth-migration uri query part")
                    GoogleAuthenticatorExport(Base64.decode(base64Data, Base64.DEFAULT))
                }
                else -> { throw IllegalArgumentException("unsupported qr code") }
            }
        }
    }
}

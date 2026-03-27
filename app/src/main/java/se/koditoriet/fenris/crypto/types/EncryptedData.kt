package se.koditoriet.fenris.crypto.types

import android.util.Base64

data class EncryptedData(
    internal val iv: IV,
    internal val ciphertext: Ciphertext,
) {
    fun encode(): String {
        val iv = Base64.encodeToString(iv.asBytes, Base64.NO_WRAP)
        val ciphertext = Base64.encodeToString(ciphertext.asBytes, Base64.NO_WRAP)
        return "${iv}:${ciphertext}"
    }

    companion object {
        fun from(iv: ByteArray, ciphertext: ByteArray): EncryptedData =
            EncryptedData(IV(iv), Ciphertext(ciphertext))

        fun decode(data: String): EncryptedData {
            val parts = data.split(":", limit = 2)
            if (parts.size != 2) {
                throw IllegalArgumentException("argument is not valid encrypted data")
            }
            return EncryptedData(
                iv = IV.decode(parts[0]),
                ciphertext = Ciphertext.decode(parts[1]),
            )
        }
    }
}


@JvmInline
value class IV(val asBytes: ByteArray) {
    companion object {
        fun decode(data: String): IV =
            IV(Base64.decode(data, Base64.NO_WRAP))
    }
}

@JvmInline
value class Ciphertext(val asBytes: ByteArray) {
    companion object {
        fun decode(data: String): Ciphertext =
            Ciphertext(Base64.decode(data, Base64.NO_WRAP))
    }
}

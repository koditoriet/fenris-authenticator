package se.koditoriet.fenris.codec

import se.koditoriet.fenris.crypto.BitReader
import se.koditoriet.fenris.crypto.BitWriter

fun base32Decode(base32: CharArray): ByteArray {
    val buffer = BitWriter()
    try {
        for (c in base32) {
            val bits = when (c.uppercaseChar()) {
                in 'A'..'Z' -> c - 'A'
                in '2'..'7' -> c - ('2' - 26)
                '=' -> break
                else -> throw IllegalArgumentException("invalid base32 string")
            }
            buffer.write(bits.toByte(), 5)
        }
        val bytes = buffer.getBytes()
        return bytes
    } finally {
        buffer.wipe()
    }
}

fun base32Encode(bytes: ByteArray): CharArray {
    val chars = BitReader(bytes).chunks(5, zeroPadEnd = true).map {
        when (it) {
            in 0..25 -> (it + 65).toChar()
            in 26..32 -> (it + 50 - 26).toChar()
            else -> error("unreachable")
        }
    }.toCharArray()
    return if (chars.size % 8 == 0) {
        chars
    } else {
        chars + "=".repeat(8 - (chars.size % 8)).toCharArray()
    }
}

fun isValidBase32(base32: String): Boolean {
    val base32Array = base32.toCharArray()
    try {
        base32Decode(base32Array)
        return true
    } catch (_: IllegalArgumentException) {
        return false
    } finally {
        base32Array.fill('\u0000')
    }
}

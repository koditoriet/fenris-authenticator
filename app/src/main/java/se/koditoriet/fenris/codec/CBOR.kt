package se.koditoriet.fenris.codec

private typealias CborMapEntry = Pair<Any, Any>

fun ctapCborMap(vararg entries: CborMapEntry): ByteArray =
    encodeCbor(entries.toList().associate { it })

private fun encodeCbor(value: Any): ByteArray =
    when (value) {
        is Int -> encodeInt(value)
        is String -> encodeTextString(value)
        is ByteArray -> encodeByteString(value)
        is Map<*, *> -> encodeMap(value)
        else -> error("Unsupported CBOR type: ${value::class.qualifiedName}")
    }

private fun encodeMap(map: Map<*, *>): ByteArray {
    val encodedEntries = map.entries.map { entry ->
        val key = requireNotNull(entry.key) { "CBOR map keys must not be null" }
        val encodedKey = encodeCbor(key)
        encodedKey to encodeCbor(requireNotNull(entry.value) { "CBOR map values must not be null" })
    }.sortedWith { left, right ->
        val sizeComparison = left.first.size.compareTo(right.first.size)
        if (sizeComparison != 0) {
            sizeComparison
        } else {
            compareByteArrays(left.first, right.first)
        }
    }

    return buildList {
        add(encodeMajorTypeWithLength(5, encodedEntries.size.toLong()))
        encodedEntries.forEach { (key, value) ->
            add(key)
            add(value)
        }
    }.flatten()
}

private fun encodeInt(value: Int): ByteArray =
    if (value >= 0) {
        encodeMajorTypeWithLength(0, value.toLong())
    } else {
        encodeMajorTypeWithLength(1, -1L - value)
    }

private fun encodeTextString(value: String): ByteArray {
    val bytes = value.toByteArray(Charsets.UTF_8)
    return encodeMajorTypeWithLength(3, bytes.size.toLong()) + bytes
}

private fun encodeByteString(value: ByteArray): ByteArray =
    encodeMajorTypeWithLength(2, value.size.toLong()) + value

private fun encodeMajorTypeWithLength(majorType: Int, length: Long): ByteArray {
    require(length >= 0) { "CBOR length must be non-negative" }

    val prefix = majorType shl 5
    return when {
        length < 24 -> byteArrayOf((prefix or length.toInt()).toByte())
        length <= 0xff -> byteArrayOf((prefix or 24).toByte(), length.toByte())
        length <= 0xffff -> byteArrayOf(
            (prefix or 25).toByte(),
            (length shr 8).toByte(),
            length.toByte(),
        )
        length <= 0xffff_ffff -> byteArrayOf(
            (prefix or 26).toByte(),
            (length shr 24).toByte(),
            (length shr 16).toByte(),
            (length shr 8).toByte(),
            length.toByte(),
        )
        else -> byteArrayOf(
            (prefix or 27).toByte(),
            (length shr 56).toByte(),
            (length shr 48).toByte(),
            (length shr 40).toByte(),
            (length shr 32).toByte(),
            (length shr 24).toByte(),
            (length shr 16).toByte(),
            (length shr 8).toByte(),
            length.toByte(),
        )
    }
}

private fun compareByteArrays(left: ByteArray, right: ByteArray): Int {
    val commonLength = minOf(left.size, right.size)
    for (index in 0 until commonLength) {
        val cmp = left[index].toUByte().compareTo(right[index].toUByte())
        if (cmp != 0) return cmp
    }
    return left.size.compareTo(right.size)
}

private fun List<ByteArray>.flatten(): ByteArray {
    val totalSize = sumOf { it.size }
    val result = ByteArray(totalSize)
    var position = 0
    forEach { bytes ->
        bytes.copyInto(result, destinationOffset = position)
        position += bytes.size
    }
    return result
}

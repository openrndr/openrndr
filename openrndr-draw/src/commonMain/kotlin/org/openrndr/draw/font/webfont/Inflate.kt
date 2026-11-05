package org.openrndr.draw.font.webfont

/**
 * A decoder of zlib streams (RFC 1950) of Deflate compressed data (RFC 1951), as used by WOFF
 */
internal class InflateException(message: String) : RuntimeException(message)

private val LENGTH_BASE = intArrayOf(
    3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258
)
private val LENGTH_EXTRA = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
)
private val DISTANCE_BASE = intArrayOf(
    1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097,
    6145, 8193, 12289, 16385, 24577
)
private val DISTANCE_EXTRA = intArrayOf(
    0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
)
private val CODE_LENGTH_ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

/** a Deflate prefix code, a code with a single symbol takes a bit like the other codes */
private fun inflateCode(lengths: IntArray): PrefixCode = PrefixCode(lengths, singleSymbolTakesNoBits = false)

/** decompresses the zlib stream in [data] from [start] to [end], [size] is the size of the decompressed data */
internal fun zlibDecompress(data: ByteArray, start: Int, end: Int, size: Int): ByteArray {
    if (end - start < 2) throw InflateException("zlib stream too short")
    val cmf = data[start].toInt() and 0xff
    val flg = data[start + 1].toInt() and 0xff
    if (cmf and 0x0f != 8 || (cmf * 256 + flg) % 31 != 0) throw InflateException("not a zlib deflate stream")
    if (flg and 0x20 != 0) throw InflateException("zlib preset dictionaries are not supported")
    return inflate(data, start + 2, end, size)
}

/** decompresses the raw Deflate data in [data] from [start] to [end], [size] is the expected size */
internal fun inflate(data: ByteArray, start: Int, end: Int, size: Int): ByteArray {
    val reader = BitReader(data, start, end)
    var output = ByteArray(maxOf(size, 1024))
    var length = 0
    fun add(b: Int) {
        if (length == output.size) output = output.copyOf(output.size * 2)
        output[length++] = b.toByte()
    }

    val fixedLiterals by lazy {
        inflateCode(IntArray(288) { when { it < 144 -> 8; it < 256 -> 9; it < 280 -> 7; else -> 8 } })
    }
    val fixedDistances by lazy { inflateCode(IntArray(30) { 5 }) }

    var last = false
    while (!last) {
        last = reader.read(1) == 1
        val type = reader.read(2)
        when (type) {
            0 -> {
                reader.alignToByte()
                val p = reader.bytePosition
                if (p + 4 > end) throw InflateException("unexpected end of a stored block")
                val len = (data[p].toInt() and 0xff) or ((data[p + 1].toInt() and 0xff) shl 8)
                val nlen = (data[p + 2].toInt() and 0xff) or ((data[p + 3].toInt() and 0xff) shl 8)
                if (len != nlen.inv() and 0xffff) throw InflateException("invalid stored block length")
                if (p + 4 + len > end) throw InflateException("unexpected end of a stored block")
                for (i in 0 until len) add(data[p + 4 + i].toInt())
                reader.skipBytes(4 + len)
            }

            1, 2 -> {
                val literals: PrefixCode
                val distances: PrefixCode
                if (type == 1) {
                    literals = fixedLiterals
                    distances = fixedDistances
                } else {
                    val nlen = reader.read(5) + 257
                    val ndist = reader.read(5) + 1
                    val ncode = reader.read(4) + 4
                    val codeLengths = IntArray(19)
                    for (i in 0 until ncode) codeLengths[CODE_LENGTH_ORDER[i]] = reader.read(3)
                    val codeLengthCode = inflateCode(codeLengths)
                    val lengths = IntArray(nlen + ndist)
                    var i = 0
                    while (i < nlen + ndist) {
                        val symbol = codeLengthCode.decode(reader)
                        when {
                            symbol < 16 -> lengths[i++] = symbol
                            else -> {
                                val (value, repeat) = when (symbol) {
                                    16 -> {
                                        if (i == 0) throw InflateException("repeat without a previous length")
                                        lengths[i - 1] to 3 + reader.read(2)
                                    }
                                    17 -> 0 to 3 + reader.read(3)
                                    else -> 0 to 11 + reader.read(7)
                                }
                                if (i + repeat > nlen + ndist) throw InflateException("code lengths beyond the alphabet")
                                for (r in 0 until repeat) lengths[i++] = value
                            }
                        }
                    }
                    literals = inflateCode(lengths.copyOfRange(0, nlen))
                    distances = inflateCode(lengths.copyOfRange(nlen, nlen + ndist))
                }
                while (true) {
                    val symbol = literals.decode(reader)
                    if (symbol < 256) {
                        add(symbol)
                    } else if (symbol == 256) {
                        break
                    } else {
                        val lengthCode = symbol - 257
                        if (lengthCode >= 29) throw InflateException("invalid length code")
                        val copyLength = LENGTH_BASE[lengthCode] + reader.read(LENGTH_EXTRA[lengthCode])
                        val distanceCode = distances.decode(reader)
                        if (distanceCode >= 30) throw InflateException("invalid distance code")
                        val distance = DISTANCE_BASE[distanceCode] + reader.read(DISTANCE_EXTRA[distanceCode])
                        if (distance > length) throw InflateException("distance beyond the start of the data")
                        val from = length - distance
                        for (c in 0 until copyLength) add(output[from + c].toInt())
                    }
                }
            }

            else -> throw InflateException("invalid block type")
        }
    }
    return output.copyOf(length)
}

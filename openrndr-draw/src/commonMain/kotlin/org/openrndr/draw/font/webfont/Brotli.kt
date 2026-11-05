package org.openrndr.draw.font.webfont

/**
 * A decoder of Brotli compressed data, RFC 7932, as used by WOFF2. The tables are those of the reference decoder.
 */
internal class BrotliException(message: String) : RuntimeException(message)

/** reads bits least significant bit first, like Brotli and Deflate pack them */
internal class BitReader(private val data: ByteArray, start: Int = 0, private val end: Int = data.size) {
    private var position = start * 8L

    /** reads [count] bits, at most 24, bits beyond the end of the data are zero */
    fun read(count: Int): Int {
        var value = 0
        for (i in 0 until count) {
            value = value or (bit() shl i)
        }
        return value
    }

    fun bit(): Int {
        val index = (position ushr 3).toInt()
        if (index >= end) throw BrotliException("unexpected end of the compressed data")
        val bit = (data[index].toInt() ushr (position and 7).toInt()) and 1
        position++
        return bit
    }

    /** skips to the next byte boundary */
    fun alignToByte() {
        position = (position + 7) and 7L.inv()
    }

    val bytePosition: Int get() = (position ushr 3).toInt()

    /** the next 4 bits without reading them, bits beyond the end of the data are zero */
    fun peekLowBits4(): Int {
        var value = 0
        for (i in 0 until 4) {
            val p = position + i
            val index = (p ushr 3).toInt()
            if (index < end) value = value or (((data[index].toInt() ushr (p and 7).toInt()) and 1) shl i)
        }
        return value
    }

    fun skipBits(count: Int) {
        position += count
    }

    fun skipBytes(count: Int) {
        position += count * 8L
    }
}

/**
 * A canonical prefix code, decoded bit by bit: the codes of [lengths] are assigned in order of length and symbol. In
 * Brotli a code with a single symbol takes no bits, in Deflate it takes the bits of its length.
 */
internal class PrefixCode(lengths: IntArray, singleSymbolTakesNoBits: Boolean = true) {
    private val counts = IntArray(16)
    private val symbols: IntArray
    private val single: Int

    init {
        for (length in lengths) counts[length]++
        counts[0] = 0
        val offsets = IntArray(16)
        for (i in 1 until 15) offsets[i + 1] = offsets[i] + counts[i]
        symbols = IntArray(lengths.size)
        for ((symbol, length) in lengths.withIndex()) {
            if (length != 0) symbols[offsets[length]++] = symbol
        }
        val used = lengths.count { it != 0 }
        single = if (used == 1 && singleSymbolTakesNoBits) lengths.indexOfFirst { it != 0 } else -1
    }

    fun decode(reader: BitReader): Int {
        if (single >= 0) return single
        var code = 0
        var first = 0
        var index = 0
        for (length in 1..15) {
            code = code or reader.bit()
            val count = counts[length]
            if (code - first < count) return symbols[index + code - first]
            index += count
            first += count
            first = first shl 1
            code = code shl 1
        }
        throw BrotliException("invalid prefix code")
    }
}

internal class Transform(val prefix: ByteArray, val type: Int, val suffix: ByteArray)

private const val LITERAL_ALPHABET = 256
private const val COMMAND_ALPHABET = 704
private const val BLOCK_COUNT_ALPHABET = 26

/** the bits of the alphabet of [size] symbols, of the symbols of simple prefix codes */
private fun alphabetBits(size: Int): Int {
    var bits = 0
    var n = size - 1
    while (n != 0) {
        n = n ushr 1
        bits++
    }
    return bits
}

/** reads a prefix code of an alphabet of [alphabetSize] symbols, RFC 7932 section 3.4 and 3.5 */
private fun readPrefixCode(alphabetSize: Int, reader: BitReader): PrefixCode {
    val lengths = IntArray(alphabetSize)
    val hskip = reader.read(2)
    if (hskip == 1) {
        // a simple prefix code
        val count = reader.read(2) + 1
        val bits = alphabetBits(alphabetSize)
        val symbols = IntArray(count) { reader.read(bits) }
        for (symbol in symbols) {
            if (symbol >= alphabetSize) throw BrotliException("invalid symbol in a simple prefix code")
        }
        when (count) {
            1 -> lengths[symbols[0]] = 1
            2 -> { lengths[symbols[0]] = 1; lengths[symbols[1]] = 1 }
            3 -> { lengths[symbols[0]] = 1; lengths[symbols[1]] = 2; lengths[symbols[2]] = 2 }
            else -> if (reader.read(1) == 0) {
                for (symbol in symbols) lengths[symbol] = 2
            } else {
                lengths[symbols[0]] = 1; lengths[symbols[1]] = 2; lengths[symbols[2]] = 3; lengths[symbols[3]] = 3
            }
        }
        if (symbols.toSet().size != count) throw BrotliException("repeated symbol in a simple prefix code")
        return PrefixCode(lengths)
    }

    // the code lengths of the code length alphabet, coded with a fixed code
    val codeLengthLengths = IntArray(18)
    var space = 32
    var codes = 0
    var i = hskip
    while (i < 18 && space > 0) {
        val entry = FIXED_TABLE[reader.peekLowBits4()]
        reader.skipBits(entry ushr 16)
        val length = entry and 0xffff
        codeLengthLengths[CODE_LENGTH_CODE_ORDER[i]] = length
        if (length != 0) {
            space -= 32 shr length
            codes++
        }
        i++
    }
    if (space != 0 && codes != 1) throw BrotliException("invalid code length code")
    val codeLengthCode = PrefixCode(codeLengthLengths)

    var symbol = 0
    var previousLength = 8
    var repeat = 0
    var repeatLength = 0
    var remaining = 32768
    while (symbol < alphabetSize && remaining > 0) {
        val length = codeLengthCode.decode(reader)
        if (length < 16) {
            repeat = 0
            lengths[symbol++] = length
            if (length != 0) {
                previousLength = length
                remaining -= 32768 shr length
            }
        } else {
            val extraBits = length - 14
            val newLength = if (length == 16) previousLength else 0
            if (repeatLength != newLength) {
                repeat = 0
                repeatLength = newLength
            }
            val oldRepeat = repeat
            if (repeat > 0) {
                repeat -= 2
                repeat = repeat shl extraBits
            }
            repeat += reader.read(extraBits) + 3
            val delta = repeat - oldRepeat
            if (symbol + delta > alphabetSize) throw BrotliException("code lengths beyond the alphabet")
            for (j in 0 until delta) lengths[symbol++] = repeatLength
            if (repeatLength != 0) remaining -= delta shl (15 - repeatLength)
        }
    }
    if (remaining != 0) throw BrotliException("invalid code lengths")
    return PrefixCode(lengths)
}

/** VarLenUint8, RFC 7932 section 9.2 */
private fun readVarLenUint8(reader: BitReader): Int {
    if (reader.read(1) == 0) return 0
    val n = reader.read(3)
    return if (n == 0) 1 else (1 shl n) + reader.read(n)
}

private fun readBlockLength(code: PrefixCode, reader: BitReader): Int {
    val symbol = code.decode(reader)
    return BLOCK_LENGTH_OFFSET[symbol] + reader.read(BLOCK_LENGTH_N_BITS[symbol])
}

/** the context map of [size] entries of [trees] prefix codes, RFC 7932 section 7.3 */
private fun readContextMap(size: Int, trees: Int, reader: BitReader): IntArray {
    val map = IntArray(size)
    if (trees < 2) return map
    val rleMax = if (reader.read(1) == 1) reader.read(4) + 1 else 0
    val code = readPrefixCode(trees + rleMax, reader)
    var i = 0
    while (i < size) {
        val symbol = code.decode(reader)
        when {
            symbol == 0 -> map[i++] = 0
            symbol > rleMax -> map[i++] = symbol - rleMax
            else -> {
                var repeat = (1 shl symbol) + reader.read(symbol)
                if (i + repeat > size) throw BrotliException("context map run beyond its size")
                while (repeat-- > 0) map[i++] = 0
            }
        }
    }
    if (reader.read(1) == 1) {
        // inverse move-to-front
        val mtf = IntArray(256) { it }
        for (j in 0 until size) {
            val index = map[j]
            val value = mtf[index]
            map[j] = value
            for (k in index downTo 1) mtf[k] = mtf[k - 1]
            mtf[0] = value
        }
    }
    return map
}

/** the block types and block lengths of a category of a meta-block, RFC 7932 section 6 */
private class BlockSwitch(reader: BitReader) {
    val types = readVarLenUint8(reader) + 1
    private val typeCode = if (types >= 2) readPrefixCode(types + 2, reader) else null
    private val countCode = if (types >= 2) readPrefixCode(BLOCK_COUNT_ALPHABET, reader) else null
    var type = 0
    private var previousType = 1
    var remaining = if (countCode != null) readBlockLength(countCode, reader) else 1 shl 24

    /** counts a symbol of the current block, switching to the next block when the current one ends */
    fun next(reader: BitReader) {
        if (remaining == 0) {
            val code = typeCode!!.decode(reader)
            var newType = when (code) {
                0 -> previousType
                1 -> type + 1
                else -> code - 2
            }
            if (newType >= types) newType -= types
            previousType = type
            type = newType
            remaining = readBlockLength(countCode!!, reader)
        }
        remaining--
    }
}

/** the transformed dictionary word, RFC 7932 section 8 */
private fun dictionaryWord(length: Int, wordId: Int): ByteArray {
    val bits = DICTIONARY_SIZE_BITS_BY_LENGTH[length]
    val index = wordId and ((1 shl bits) - 1)
    val transformIndex = wordId ushr bits
    if (transformIndex >= TRANSFORMS.size) throw BrotliException("invalid dictionary word transform")
    val transform = TRANSFORMS[transformIndex]
    val offset = DICTIONARY_OFFSETS_BY_LENGTH[length] + index * length
    val dictionary = brotliDictionary
    val type = transform.type
    var start = offset
    var end = offset + length
    when {
        type in 1..9 -> end -= type
        type in 12..20 -> start += minOf(type - 11, length)
    }
    val word = dictionary.copyOfRange(start, maxOf(start, end))
    if (type == 10 || type == 11) {
        var i = 0
        while (i < word.size) {
            val c = word[i].toInt() and 0xff
            val step = when {
                c < 0xc0 -> {
                    if (c in 97..122) word[i] = (c xor 32).toByte()
                    1
                }
                c < 0xe0 -> {
                    if (i + 1 < word.size) word[i + 1] = (word[i + 1].toInt() xor 32).toByte()
                    2
                }
                else -> {
                    if (i + 2 < word.size) word[i + 2] = (word[i + 2].toInt() xor 5).toByte()
                    3
                }
            }
            if (type == 10) break
            i += step
        }
    }
    return transform.prefix + word + transform.suffix
}

/** a growable output of bytes */
private class Output(capacity: Int) {
    var data = ByteArray(maxOf(capacity, 1024))
    var size = 0

    fun add(byte: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = byte.toByte()
    }

    operator fun get(index: Int): Int = data[index].toInt() and 0xff

    fun toByteArray(): ByteArray = data.copyOf(size)
}

/**
 * Decompresses the Brotli stream in [data] from [start] to [end], [sizeHint] is the expected size of the output
 */
internal fun brotliDecompress(data: ByteArray, start: Int = 0, end: Int = data.size, sizeHint: Int = 0): ByteArray {
    val reader = BitReader(data, start, end)
    val output = Output(sizeHint)

    // the window size
    val windowBits = if (reader.read(1) == 0) 16 else {
        val n = reader.read(3)
        if (n != 0) 17 + n else {
            val m = reader.read(3)
            when (m) {
                0 -> 17
                1 -> throw BrotliException("large window streams are not supported")
                else -> 8 + m
            }
        }
    }
    val maxBackwardDistance = (1 shl windowBits) - 16

    val distances = intArrayOf(16, 15, 11, 4)
    var distanceIndex = 0

    var last = false
    while (!last) {
        last = reader.read(1) == 1
        if (last && reader.read(1) == 1) break

        val nibbles = reader.read(2)
        if (nibbles == 3) {
            // metadata
            if (reader.read(1) != 0) throw BrotliException("invalid reserved bit")
            val skipBytes = reader.read(2)
            var skipLength = 0
            if (skipBytes > 0) {
                for (i in 0 until skipBytes) skipLength = skipLength or (reader.read(8) shl (8 * i))
                skipLength += 1
            }
            reader.alignToByte()
            reader.skipBytes(skipLength)
            continue
        }
        var metaBlockLength = 0
        for (i in 0 until nibbles + 4) metaBlockLength = metaBlockLength or (reader.read(4) shl (4 * i))
        metaBlockLength += 1

        if (!last && reader.read(1) == 1) {
            // uncompressed
            reader.alignToByte()
            val position = reader.bytePosition
            if (position + metaBlockLength > end) throw BrotliException("unexpected end of uncompressed data")
            for (i in 0 until metaBlockLength) output.add(data[position + i].toInt())
            reader.skipBytes(metaBlockLength)
            continue
        }

        val literalSwitch = BlockSwitch(reader)
        val commandSwitch = BlockSwitch(reader)
        val distanceSwitch = BlockSwitch(reader)

        val postfixBits = reader.read(2)
        val directDistances = reader.read(4) shl postfixBits
        val postfixMask = (1 shl postfixBits) - 1
        val distanceAlphabet = 16 + directDistances + (48 shl postfixBits)

        val contextModes = IntArray(literalSwitch.types) { reader.read(2) }
        val literalTrees = readVarLenUint8(reader) + 1
        val literalMap = readContextMap(64 * literalSwitch.types, literalTrees, reader)
        val distanceTrees = readVarLenUint8(reader) + 1
        val distanceMap = readContextMap(4 * distanceSwitch.types, distanceTrees, reader)

        val literalCodes = Array(literalTrees) { readPrefixCode(LITERAL_ALPHABET, reader) }
        val commandCodes = Array(commandSwitch.types) { readPrefixCode(COMMAND_ALPHABET, reader) }
        val distanceCodes = Array(distanceTrees) { readPrefixCode(distanceAlphabet, reader) }

        val metaBlockEnd = output.size + metaBlockLength
        while (output.size < metaBlockEnd) {
            commandSwitch.next(reader)
            val command = commandCodes[commandSwitch.type].decode(reader)
            var range = command ushr 6
            var implicitDistance = false
            if (range >= 2) range -= 2 else implicitDistance = true
            val insertCode = INSERT_RANGE_LUT[range] + ((command ushr 3) and 7)
            val copyCode = COPY_RANGE_LUT[range] + (command and 7)
            val insertLength = INSERT_LENGTH_OFFSET[insertCode] + reader.read(INSERT_LENGTH_N_BITS[insertCode])
            val copyLength = COPY_LENGTH_OFFSET[copyCode] + reader.read(COPY_LENGTH_N_BITS[copyCode])

            for (i in 0 until insertLength) {
                literalSwitch.next(reader)
                val p1 = if (output.size > 0) output[output.size - 1] else 0
                val p2 = if (output.size > 1) output[output.size - 2] else 0
                val mode = contextModes[literalSwitch.type]
                val context = CONTEXT_LOOKUP[CONTEXT_LOOKUP_OFFSETS[2 * mode] + p1] or
                        CONTEXT_LOOKUP[CONTEXT_LOOKUP_OFFSETS[2 * mode + 1] + p2]
                val tree = literalMap[64 * literalSwitch.type + context]
                output.add(literalCodes[tree].decode(reader))
            }
            if (output.size >= metaBlockEnd) break

            val distanceCode: Int
            val distance: Int
            if (implicitDistance) {
                distanceCode = 0
                distance = distances[(distanceIndex + 3) and 3]
            } else {
                distanceSwitch.next(reader)
                val context = if (copyLength > 4) 3 else copyLength - 2
                val tree = distanceMap[4 * distanceSwitch.type + context]
                distanceCode = distanceCodes[tree].decode(reader)
                distance = when {
                    distanceCode < 16 -> distances[(distanceIndex + DISTANCE_SHORT_CODE_INDEX_OFFSET[distanceCode]) and 3] +
                            DISTANCE_SHORT_CODE_VALUE_OFFSET[distanceCode]
                    distanceCode < 16 + directDistances -> distanceCode - 15
                    else -> {
                        val code = distanceCode - directDistances - 16
                        val bits = 1 + (code ushr (postfixBits + 1))
                        val high = code ushr postfixBits
                        val low = code and postfixMask
                        val offset = ((2 + (high and 1)) shl bits) - 4
                        ((offset + reader.read(bits)) shl postfixBits) + low + directDistances + 1
                    }
                }
                if (distance <= 0) throw BrotliException("invalid distance")
            }

            val maxDistance = minOf(maxBackwardDistance, output.size)
            if (distance > maxDistance) {
                // a word of the static dictionary
                if (copyLength < 4 || copyLength > 24) throw BrotliException("invalid dictionary word length")
                val word = dictionaryWord(copyLength, distance - maxDistance - 1)
                for (b in word) output.add(b.toInt())
            } else {
                if (distanceCode != 0) {
                    distances[distanceIndex and 3] = distance
                    distanceIndex++
                }
                val from = output.size - distance
                for (i in 0 until copyLength) output.add(output[from + i])
            }
        }
        if (output.size > metaBlockEnd) throw BrotliException("meta-block data beyond its length")
    }
    return output.toByteArray()
}


// -- tables of the reference decoder, org.brotli:dec (MIT license)

private val CONTEXT_LOOKUP = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 8, 12, 16, 12, 12, 20, 12, 16, 24, 28, 12, 12, 32, 12, 36, 12,
    44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 32, 32, 24, 40, 28, 12, 12, 48, 52, 52, 52, 48, 52, 52,
    52, 48, 52, 52, 52, 52, 52, 48, 52, 52, 52, 52, 52, 48, 52, 52, 52, 52, 52, 24, 12, 28, 12, 12,
    12, 56, 60, 60, 60, 56, 60, 60, 60, 56, 60, 60, 60, 60, 60, 56, 60, 60, 60, 60, 60, 56, 60, 60,
    60, 60, 60, 24, 12, 28, 12, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
    0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
    0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
    2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3,
    2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3,
    2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3,
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 2, 2, 2, 2, 2, 2, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4,
    4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
    5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
    5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7,
    0, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 16, 16, 16, 16, 16, 16, 16, 16,
    16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
    16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
    32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
    32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
    40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
    40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
    48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 56, 0, 1, 2, 3, 4, 5, 6, 7,
    8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
    56, 57, 58, 59, 60, 61, 62, 63, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
    40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
    24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 0, 1, 2, 3, 4, 5, 6, 7,
    8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
    56, 57, 58, 59, 60, 61, 62, 63, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3,
    4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9,
    10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15,
    16, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18, 19, 19, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21,
    22, 22, 22, 22, 23, 23, 23, 23, 24, 24, 24, 24, 25, 25, 25, 25, 26, 26, 26, 26, 27, 27, 27, 27,
    28, 28, 28, 28, 29, 29, 29, 29, 30, 30, 30, 30, 31, 31, 31, 31, 32, 32, 32, 32, 33, 33, 33, 33,
    34, 34, 34, 34, 35, 35, 35, 35, 36, 36, 36, 36, 37, 37, 37, 37, 38, 38, 38, 38, 39, 39, 39, 39,
    40, 40, 40, 40, 41, 41, 41, 41, 42, 42, 42, 42, 43, 43, 43, 43, 44, 44, 44, 44, 45, 45, 45, 45,
    46, 46, 46, 46, 47, 47, 47, 47, 48, 48, 48, 48, 49, 49, 49, 49, 50, 50, 50, 50, 51, 51, 51, 51,
    52, 52, 52, 52, 53, 53, 53, 53, 54, 54, 54, 54, 55, 55, 55, 55, 56, 56, 56, 56, 57, 57, 57, 57,
    58, 58, 58, 58, 59, 59, 59, 59, 60, 60, 60, 60, 61, 61, 61, 61, 62, 62, 62, 62, 63, 63, 63, 63,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
)

private val CONTEXT_LOOKUP_OFFSETS = intArrayOf(
    1024, 1536, 1280, 1536, 0, 256, 768, 512
)

private val DICTIONARY_OFFSETS_BY_LENGTH = intArrayOf(
    0, 0, 0, 0, 0, 4096, 9216, 21504, 35840, 44032, 53248, 63488, 74752, 87040, 93696, 100864, 104704, 106752, 108928, 113536, 115968, 118528, 119872, 121280,
    122016
)

private val DICTIONARY_SIZE_BITS_BY_LENGTH = intArrayOf(
    0, 0, 0, 0, 10, 10, 11, 11, 10, 10, 10, 10, 10, 9, 9, 8, 7, 7, 8, 7, 7, 6, 6, 5,
    5
)

private val BLOCK_LENGTH_OFFSET = intArrayOf(
    1, 5, 9, 13, 17, 25, 33, 41, 49, 65, 81, 97, 113, 145, 177, 209, 241, 305, 369, 497, 753, 1265, 2289, 4337,
    8433, 16625
)

private val BLOCK_LENGTH_N_BITS = intArrayOf(
    2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 7, 8, 9, 10, 11, 12,
    13, 24
)

private val INSERT_LENGTH_OFFSET = intArrayOf(
    0, 1, 2, 3, 4, 5, 6, 8, 10, 14, 18, 26, 34, 50, 66, 98, 130, 194, 322, 578, 1090, 2114, 6210, 22594
)

private val INSERT_LENGTH_N_BITS = intArrayOf(
    0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 12, 14, 24
)

private val COPY_LENGTH_OFFSET = intArrayOf(
    2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 18, 22, 30, 38, 54, 70, 102, 134, 198, 326, 582, 1094, 2118
)

private val COPY_LENGTH_N_BITS = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 24
)

private val INSERT_RANGE_LUT = intArrayOf(
    0, 0, 8, 8, 0, 16, 8, 16, 16
)

private val COPY_RANGE_LUT = intArrayOf(
    0, 8, 0, 8, 16, 0, 16, 8, 16
)

private val CODE_LENGTH_CODE_ORDER = intArrayOf(
    1, 2, 3, 4, 0, 5, 17, 6, 16, 7, 8, 9, 10, 11, 12, 13, 14, 15
)

private val DISTANCE_SHORT_CODE_INDEX_OFFSET = intArrayOf(
    3, 2, 1, 0, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2
)

private val DISTANCE_SHORT_CODE_VALUE_OFFSET = intArrayOf(
    0, 0, 0, 0, -1, 1, -2, 2, -3, 3, -1, 1, -2, 2, -3, 3
)

private val FIXED_TABLE = intArrayOf(
    131072, 131076, 131075, 196610, 131072, 131076, 131075, 262145, 131072, 131076, 131075, 196610, 131072, 131076, 131075, 262149
)

private val TRANSFORMS = arrayOf(
    Transform(byteArrayOf(), 0, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(32)),
    Transform(byteArrayOf(32), 0, byteArrayOf(32)),
    Transform(byteArrayOf(), 12, byteArrayOf()),
    Transform(byteArrayOf(), 10, byteArrayOf(32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 116, 104, 101, 32)),
    Transform(byteArrayOf(32), 0, byteArrayOf()),
    Transform(byteArrayOf(115, 32), 0, byteArrayOf(32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 111, 102, 32)),
    Transform(byteArrayOf(), 10, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 97, 110, 100, 32)),
    Transform(byteArrayOf(), 13, byteArrayOf()),
    Transform(byteArrayOf(), 1, byteArrayOf()),
    Transform(byteArrayOf(44, 32), 0, byteArrayOf(32)),
    Transform(byteArrayOf(), 0, byteArrayOf(44, 32)),
    Transform(byteArrayOf(32), 10, byteArrayOf(32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 105, 110, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 116, 111, 32)),
    Transform(byteArrayOf(101, 32), 0, byteArrayOf(32)),
    Transform(byteArrayOf(), 0, byteArrayOf(34)),
    Transform(byteArrayOf(), 0, byteArrayOf(46)),
    Transform(byteArrayOf(), 0, byteArrayOf(34, 62)),
    Transform(byteArrayOf(), 0, byteArrayOf(10)),
    Transform(byteArrayOf(), 3, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(93)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 102, 111, 114, 32)),
    Transform(byteArrayOf(), 14, byteArrayOf()),
    Transform(byteArrayOf(), 2, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 97, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 116, 104, 97, 116, 32)),
    Transform(byteArrayOf(32), 10, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(46, 32)),
    Transform(byteArrayOf(46), 0, byteArrayOf()),
    Transform(byteArrayOf(32), 0, byteArrayOf(44, 32)),
    Transform(byteArrayOf(), 15, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 119, 105, 116, 104, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(39)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 102, 114, 111, 109, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 98, 121, 32)),
    Transform(byteArrayOf(), 16, byteArrayOf()),
    Transform(byteArrayOf(), 17, byteArrayOf()),
    Transform(byteArrayOf(32, 116, 104, 101, 32), 0, byteArrayOf()),
    Transform(byteArrayOf(), 4, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(46, 32, 84, 104, 101, 32)),
    Transform(byteArrayOf(), 11, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 111, 110, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 97, 115, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 105, 115, 32)),
    Transform(byteArrayOf(), 7, byteArrayOf()),
    Transform(byteArrayOf(), 1, byteArrayOf(105, 110, 103, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(10, 9)),
    Transform(byteArrayOf(), 0, byteArrayOf(58)),
    Transform(byteArrayOf(32), 0, byteArrayOf(46, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(101, 100, 32)),
    Transform(byteArrayOf(), 20, byteArrayOf()),
    Transform(byteArrayOf(), 18, byteArrayOf()),
    Transform(byteArrayOf(), 6, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(40)),
    Transform(byteArrayOf(), 10, byteArrayOf(44, 32)),
    Transform(byteArrayOf(), 8, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 97, 116, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(108, 121, 32)),
    Transform(byteArrayOf(32, 116, 104, 101, 32), 0, byteArrayOf(32, 111, 102, 32)),
    Transform(byteArrayOf(), 5, byteArrayOf()),
    Transform(byteArrayOf(), 9, byteArrayOf()),
    Transform(byteArrayOf(32), 10, byteArrayOf(44, 32)),
    Transform(byteArrayOf(), 10, byteArrayOf(34)),
    Transform(byteArrayOf(46), 0, byteArrayOf(40)),
    Transform(byteArrayOf(), 11, byteArrayOf(32)),
    Transform(byteArrayOf(), 10, byteArrayOf(34, 62)),
    Transform(byteArrayOf(), 0, byteArrayOf(61, 34)),
    Transform(byteArrayOf(32), 0, byteArrayOf(46)),
    Transform(byteArrayOf(46, 99, 111, 109, 47), 0, byteArrayOf()),
    Transform(byteArrayOf(32, 116, 104, 101, 32), 0, byteArrayOf(32, 111, 102, 32, 116, 104, 101, 32)),
    Transform(byteArrayOf(), 10, byteArrayOf(39)),
    Transform(byteArrayOf(), 0, byteArrayOf(46, 32, 84, 104, 105, 115, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(44)),
    Transform(byteArrayOf(46), 0, byteArrayOf(32)),
    Transform(byteArrayOf(), 10, byteArrayOf(40)),
    Transform(byteArrayOf(), 10, byteArrayOf(46)),
    Transform(byteArrayOf(), 0, byteArrayOf(32, 110, 111, 116, 32)),
    Transform(byteArrayOf(32), 0, byteArrayOf(61, 34)),
    Transform(byteArrayOf(), 0, byteArrayOf(101, 114, 32)),
    Transform(byteArrayOf(32), 11, byteArrayOf(32)),
    Transform(byteArrayOf(), 0, byteArrayOf(97, 108, 32)),
    Transform(byteArrayOf(32), 11, byteArrayOf()),
    Transform(byteArrayOf(), 0, byteArrayOf(61, 39)),
    Transform(byteArrayOf(), 11, byteArrayOf(34)),
    Transform(byteArrayOf(), 10, byteArrayOf(46, 32)),
    Transform(byteArrayOf(32), 0, byteArrayOf(40)),
    Transform(byteArrayOf(), 0, byteArrayOf(102, 117, 108, 32)),
    Transform(byteArrayOf(32), 10, byteArrayOf(46, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(105, 118, 101, 32)),
    Transform(byteArrayOf(), 0, byteArrayOf(108, 101, 115, 115, 32)),
    Transform(byteArrayOf(), 11, byteArrayOf(39)),
    Transform(byteArrayOf(), 0, byteArrayOf(101, 115, 116, 32)),
    Transform(byteArrayOf(32), 10, byteArrayOf(46)),
    Transform(byteArrayOf(), 11, byteArrayOf(34, 62)),
    Transform(byteArrayOf(32), 0, byteArrayOf(61, 39)),
    Transform(byteArrayOf(), 10, byteArrayOf(44)),
    Transform(byteArrayOf(), 0, byteArrayOf(105, 122, 101, 32)),
    Transform(byteArrayOf(), 11, byteArrayOf(46)),
    Transform(byteArrayOf(-62, -96), 0, byteArrayOf()),
    Transform(byteArrayOf(32), 0, byteArrayOf(44)),
    Transform(byteArrayOf(), 10, byteArrayOf(61, 34)),
    Transform(byteArrayOf(), 11, byteArrayOf(61, 34)),
    Transform(byteArrayOf(), 0, byteArrayOf(111, 117, 115, 32)),
    Transform(byteArrayOf(), 11, byteArrayOf(44, 32)),
    Transform(byteArrayOf(), 10, byteArrayOf(61, 39)),
    Transform(byteArrayOf(32), 10, byteArrayOf(44)),
    Transform(byteArrayOf(32), 11, byteArrayOf(61, 34)),
    Transform(byteArrayOf(32), 11, byteArrayOf(44, 32)),
    Transform(byteArrayOf(), 11, byteArrayOf(44)),
    Transform(byteArrayOf(), 11, byteArrayOf(40)),
    Transform(byteArrayOf(), 11, byteArrayOf(46, 32)),
    Transform(byteArrayOf(32), 11, byteArrayOf(46)),
    Transform(byteArrayOf(), 11, byteArrayOf(61, 39)),
    Transform(byteArrayOf(32), 11, byteArrayOf(46, 32)),
    Transform(byteArrayOf(32), 10, byteArrayOf(61, 34)),
    Transform(byteArrayOf(32), 11, byteArrayOf(61, 39)),
    Transform(byteArrayOf(32), 10, byteArrayOf(61, 39)),
)

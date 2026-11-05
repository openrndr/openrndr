package org.openrndr.draw.font.webfont

/**
 * An error in the data of a web font
 */
class WebFontException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** is [bytes] a WOFF 1.0 font? */
fun isWoff(bytes: ByteArray): Boolean = bytes.size >= 4 && bytes.tag(0) == "wOFF"

/** is [bytes] a WOFF 2.0 font? */
fun isWoff2(bytes: ByteArray): Boolean = bytes.size >= 4 && bytes.tag(0) == "wOF2"

/**
 * The TrueType or OpenType font of the font in [bytes]: WOFF and WOFF2 fonts are decoded, other fonts are returned as
 * they are. Font collections in WOFF2 are not supported.
 * @throws WebFontException when the web font is invalid
 */
fun sfntFontBytes(bytes: ByteArray): ByteArray = try {
    when {
        isWoff2(bytes) -> decodeWoff2(bytes)
        isWoff(bytes) -> decodeWoff(bytes)
        else -> bytes
    }
} catch (e: WebFontException) {
    throw e
} catch (e: RuntimeException) {
    throw WebFontException("invalid web font: ${e.message}", e)
}

// -- reading and writing big-endian data

internal fun ByteArray.u8(offset: Int): Int = this[offset].toInt() and 0xff
internal fun ByteArray.u16(offset: Int): Int = (u8(offset) shl 8) or u8(offset + 1)
internal fun ByteArray.s16(offset: Int): Int = u16(offset).toShort().toInt()
internal fun ByteArray.u32(offset: Int): Long = (u16(offset).toLong() shl 16) or u16(offset + 2).toLong()
internal fun ByteArray.tag(offset: Int): String = (0 until 4).map { (this[offset + it].toInt() and 0xff).toChar() }.joinToString("")

/** a growable big-endian byte buffer */
internal class ByteWriter(capacity: Int = 1024) {
    var data = ByteArray(maxOf(capacity, 16))
    var size = 0

    private fun ensure(extra: Int) {
        if (size + extra > data.size) data = data.copyOf(maxOf(data.size * 2, size + extra))
    }

    fun u8(value: Int) {
        ensure(1)
        data[size++] = value.toByte()
    }

    fun u16(value: Int) {
        u8(value ushr 8); u8(value)
    }

    fun u32(value: Long) {
        u16((value ushr 16).toInt()); u16(value.toInt())
    }

    fun bytes(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        ensure(length)
        bytes.copyInto(data, size, offset, offset + length)
        size += length
    }

    fun pad(alignment: Int) {
        while (size % alignment != 0) u8(0)
    }

    fun toByteArray(): ByteArray = data.copyOf(size)
}

/** a table of a font */
internal class SfntTable(val tag: String, val data: ByteArray)

private fun checksum(data: ByteArray, offset: Int, length: Int): Long {
    var sum = 0L
    var i = 0
    while (i < length) {
        var word = 0L
        for (j in 0 until 4) {
            word = (word shl 8) or (if (i + j < length) data.u8(offset + i + j).toLong() else 0L)
        }
        sum = (sum + word) and 0xffffffffL
        i += 4
    }
    return sum
}

/** assembles the font of [tables] with the sfnt version [flavor], the tables are sorted by tag */
internal fun writeSfnt(flavor: Long, tables: List<SfntTable>): ByteArray {
    val sorted = tables.sortedBy { it.tag }
    val count = sorted.size
    var entrySelector = 0
    while ((1 shl (entrySelector + 1)) <= count) entrySelector++
    val searchRange = (1 shl entrySelector) * 16
    val writer = ByteWriter(12 + 16 * count + sorted.sumOf { it.data.size + 3 })
    writer.u32(flavor)
    writer.u16(count)
    writer.u16(searchRange)
    writer.u16(entrySelector)
    writer.u16(count * 16 - searchRange)
    var offset = 12 + 16 * count
    val offsets = IntArray(count)
    for ((i, table) in sorted.withIndex()) {
        offsets[i] = offset
        offset += (table.data.size + 3) and 3.inv()
    }
    for ((i, table) in sorted.withIndex()) {
        for (c in table.tag) writer.u8(c.code)
        val data = if (table.tag == "head" && table.data.size >= 12) {
            table.data.copyOf().also { for (j in 8 until 12) it[j] = 0 }
        } else table.data
        writer.u32(checksum(data, 0, data.size))
        writer.u32(offsets[i].toLong())
        writer.u32(table.data.size.toLong())
    }
    var headOffset = -1
    for ((i, table) in sorted.withIndex()) {
        if (table.tag == "head") headOffset = offsets[i]
        writer.bytes(table.data)
        writer.pad(4)
    }
    val font = writer.toByteArray()
    if (headOffset >= 0 && font.size >= headOffset + 12) {
        // checkSumAdjustment makes the checksum of the font 0xB1B0AFBA
        for (j in 8 until 12) font[headOffset + j] = 0
        val adjustment = (0xB1B0AFBAL - checksum(font, 0, font.size)) and 0xffffffffL
        for (j in 0 until 4) font[headOffset + 8 + j] = (adjustment ushr (24 - 8 * j)).toByte()
    }
    return font
}

// -- WOFF 1.0

/** decodes a WOFF 1.0 font, https://www.w3.org/TR/WOFF/ */
internal fun decodeWoff(bytes: ByteArray): ByteArray {
    if (bytes.size < 44) throw WebFontException("WOFF header too short")
    val flavor = bytes.u32(4)
    val tableCount = bytes.u16(12)
    val tables = (0 until tableCount).map { i ->
        val entry = 44 + i * 20
        if (entry + 20 > bytes.size) throw WebFontException("WOFF table directory beyond the data")
        val tag = bytes.tag(entry)
        val offset = bytes.u32(entry + 4).toInt()
        val compressedLength = bytes.u32(entry + 8).toInt()
        val originalLength = bytes.u32(entry + 12).toInt()
        if (offset < 0 || compressedLength < 0 || offset + compressedLength > bytes.size) {
            throw WebFontException("WOFF table '$tag' beyond the data")
        }
        val data = if (compressedLength < originalLength) {
            zlibDecompress(bytes, offset, offset + compressedLength, originalLength).also {
                if (it.size != originalLength) throw WebFontException("WOFF table '$tag' has the wrong size")
            }
        } else {
            bytes.copyOfRange(offset, offset + originalLength)
        }
        SfntTable(tag, data)
    }
    return writeSfnt(flavor, tables)
}

package org.openrndr.draw.font.webfont

/**
 * Decodes WOFF 2.0 fonts, https://www.w3.org/TR/WOFF2/: the tables are compressed with Brotli, the glyf and loca
 * tables and the hmtx table can be transformed.
 */

private val KNOWN_TAGS = arrayOf(
    "cmap", "head", "hhea", "hmtx", "maxp", "name", "OS/2", "post", "cvt ", "fpgm", "glyf", "loca", "prep", "CFF ",
    "VORG", "EBDT", "EBLC", "gasp", "hdmx", "kern", "LTSH", "PCLT", "VDMX", "vhea", "vmtx", "BASE", "GDEF", "GPOS",
    "GSUB", "EBSC", "JSTF", "MATH", "CBDT", "CBLC", "COLR", "CPAL", "SVG ", "sbix", "acnt", "avar", "bdat", "bloc",
    "bsln", "cvar", "fdsc", "feat", "fmtx", "fvar", "gvar", "hsty", "just", "lcar", "mort", "morx", "opbd", "prop",
    "trak", "Zapf", "Silf", "Glat", "Gloc", "Feat", "Sill"
)

/** a reader of the data of WOFF2 fonts */
private class Woff2Reader(val data: ByteArray, var position: Int = 0, val end: Int = data.size) {
    private fun check(count: Int) {
        if (position + count > end) throw WebFontException("WOFF2 data ends unexpectedly")
    }

    fun u8(): Int { check(1); return data.u8(position++) }
    fun u16(): Int { check(2); return data.u16(position).also { position += 2 } }
    fun s16(): Int { check(2); return data.s16(position).also { position += 2 } }
    fun u32(): Long { check(4); return data.u32(position).also { position += 4 } }

    fun bytes(count: Int): ByteArray {
        check(count)
        return data.copyOfRange(position, position + count).also { position += count }
    }

    /** UIntBase128 */
    fun base128(): Long {
        var value = 0L
        for (i in 0 until 5) {
            val b = u8()
            if (i == 0 && b == 0x80) throw WebFontException("UIntBase128 with a leading zero")
            if (value and 0xfe000000L != 0L) throw WebFontException("UIntBase128 overflow")
            value = (value shl 7) or (b and 0x7f).toLong()
            if (b and 0x80 == 0) return value
        }
        throw WebFontException("UIntBase128 longer than 5 bytes")
    }

    /** 255UInt16 */
    fun u255(): Int = when (val code = u8()) {
        253 -> u16()
        255 -> u8() + 253
        254 -> u8() + 506
        else -> code
    }

    /** a reader of the next [length] bytes, which are skipped */
    fun sub(length: Int): Woff2Reader {
        check(length)
        return Woff2Reader(data, position, position + length).also { position += length }
    }
}

private class Woff2Entry(val tag: String, val originalLength: Int, val transformLength: Int?, val transformed: Boolean)

internal fun decodeWoff2(bytes: ByteArray): ByteArray {
    val header = Woff2Reader(bytes)
    if (bytes.size < 48) throw WebFontException("WOFF2 header too short")
    header.u32() // signature
    val flavor = header.u32()
    if (flavor == 0x74746366L) throw WebFontException("WOFF2 font collections are not supported")
    header.u32() // length
    val tableCount = header.u16()
    header.u16() // reserved
    header.u32() // totalSfntSize
    val compressedSize = header.u32().toInt()
    header.position = 48

    val entries = (0 until tableCount).map {
        val flags = header.u8()
        val tagIndex = flags and 0x3f
        val tag = if (tagIndex == 63) header.bytes(4).let { b -> (0 until 4).joinToString("") { (b[it].toInt() and 0xff).toChar().toString() } }
        else KNOWN_TAGS[tagIndex]
        val version = (flags ushr 6) and 3
        val originalLength = header.base128().toInt()
        // glyf and loca are transformed with version 0, hmtx with version 1, other tables are not transformed
        val transformed = if (tag == "glyf" || tag == "loca") version == 0 else version != 0
        val transformLength = if (transformed) header.base128().toInt() else null
        Woff2Entry(tag, originalLength, transformLength, transformed)
    }
    if (header.position + compressedSize > bytes.size) throw WebFontException("WOFF2 compressed data beyond the data")
    val totalSize = entries.sumOf { (it.transformLength ?: it.originalLength).toLong() }
    val stream = brotliDecompress(bytes, header.position, header.position + compressedSize, totalSize.toInt())
    if (stream.size < totalSize) throw WebFontException("WOFF2 table data shorter than the tables")

    val tableData = mutableMapOf<String, ByteArray>()
    var offset = 0
    for (entry in entries) {
        val length = entry.transformLength ?: entry.originalLength
        tableData[entry.tag] = stream.copyOfRange(offset, offset + length)
        offset += length
    }

    val glyfEntry = entries.find { it.tag == "glyf" }
    var glyphXMins: IntArray? = null
    if (glyfEntry != null && glyfEntry.transformed) {
        val (glyf, loca, xMins) = reconstructGlyf(tableData.getValue("glyf"))
        tableData["glyf"] = glyf
        tableData["loca"] = loca
        glyphXMins = xMins
    }
    val hmtxEntry = entries.find { it.tag == "hmtx" }
    if (hmtxEntry != null && hmtxEntry.transformed) {
        val xMins = glyphXMins ?: glyphXMinsOf(tableData)
        tableData["hmtx"] = reconstructHmtx(tableData.getValue("hmtx"), tableData, xMins)
    }
    return writeSfnt(flavor, entries.map { SfntTable(it.tag, tableData.getValue(it.tag)) })
}

/** the xMin of the glyphs of an untransformed glyf table */
private fun glyphXMinsOf(tables: Map<String, ByteArray>): IntArray {
    val glyf = tables["glyf"] ?: throw WebFontException("transformed hmtx without a glyf table")
    val loca = tables["loca"] ?: throw WebFontException("transformed hmtx without a loca table")
    val head = tables["head"] ?: throw WebFontException("transformed hmtx without a head table")
    val glyphs = (tables["maxp"] ?: throw WebFontException("transformed hmtx without a maxp table")).u16(4)
    val long = head.s16(50) != 0
    return IntArray(glyphs) { i ->
        val start = if (long) loca.u32(i * 4).toInt() else loca.u16(i * 2) * 2
        val end = if (long) loca.u32(i * 4 + 4).toInt() else loca.u16(i * 2 + 2) * 2
        if (end > start) glyf.s16(start + 2) else 0
    }
}

/** the hmtx table of a transformed hmtx table, the absent left side bearings are the xMin of the glyphs */
private fun reconstructHmtx(data: ByteArray, tables: Map<String, ByteArray>, xMins: IntArray): ByteArray {
    val hhea = tables["hhea"] ?: throw WebFontException("transformed hmtx without a hhea table")
    val metrics = hhea.u16(34)
    val glyphs = xMins.size
    val reader = Woff2Reader(data)
    val flags = reader.u8()
    val advances = IntArray(metrics) { reader.u16() }
    val proportional = IntArray(metrics) { if (flags and 1 == 0) reader.s16() else xMins[it] }
    val monospaced = IntArray(maxOf(0, glyphs - metrics)) { if (flags and 2 == 0) reader.s16() else xMins[metrics + it] }
    val writer = ByteWriter(metrics * 4 + monospaced.size * 2)
    for (i in 0 until metrics) {
        writer.u16(advances[i])
        writer.u16(proportional[i])
    }
    for (lsb in monospaced) writer.u16(lsb)
    return writer.toByteArray()
}

private const val ON_CURVE = 0x01
private const val X_SHORT = 0x02
private const val Y_SHORT = 0x04
private const val X_SAME_OR_POSITIVE = 0x10
private const val Y_SAME_OR_POSITIVE = 0x20
private const val OVERLAP_SIMPLE = 0x40

private const val ARG_1_AND_2_ARE_WORDS = 0x0001
private const val WE_HAVE_A_SCALE = 0x0008
private const val MORE_COMPONENTS = 0x0020
private const val WE_HAVE_AN_X_AND_Y_SCALE = 0x0040
private const val WE_HAVE_A_TWO_BY_TWO = 0x0080
private const val WE_HAVE_INSTRUCTIONS = 0x0100

private fun withSign(flag: Int, value: Int): Int = if (flag and 1 != 0) value else -value

/** the glyf table, the loca table and the xMin of the glyphs of a transformed glyf table */
private fun reconstructGlyf(data: ByteArray): Triple<ByteArray, ByteArray, IntArray> {
    val header = Woff2Reader(data)
    header.u16() // reserved
    val options = header.u16()
    val glyphCount = header.u16()
    val indexFormat = header.u16()
    val sizes = IntArray(7) { header.u32().toInt() }
    val contours = header.sub(sizes[0])
    val points = header.sub(sizes[1])
    val flagStream = header.sub(sizes[2])
    val glyphStream = header.sub(sizes[3])
    val composites = header.sub(sizes[4])
    val bboxes = header.sub(sizes[5])
    val instructions = header.sub(sizes[6])
    val overlaps = if (options and 1 != 0) header.bytes((glyphCount + 7) shr 3) else null

    val bboxBitmapSize = ((glyphCount + 31) shr 5) * 4
    val bboxBitmap = bboxes.bytes(bboxBitmapSize)
    fun hasBbox(glyph: Int) = (bboxBitmap[glyph shr 3].toInt() and (0x80 shr (glyph and 7))) != 0

    val glyf = ByteWriter(data.size * 2)
    val offsets = IntArray(glyphCount + 1)
    val xMins = IntArray(glyphCount)

    for (glyph in 0 until glyphCount) {
        offsets[glyph] = glyf.size
        val contourCount = contours.s16()
        when {
            contourCount == 0 -> {
                if (hasBbox(glyph)) throw WebFontException("empty glyph $glyph with a bounding box")
            }

            contourCount < 0 -> {
                if (!hasBbox(glyph)) throw WebFontException("composite glyph $glyph without a bounding box")
                val bbox = IntArray(4) { bboxes.s16() }
                // the component records
                val start = composites.position
                var hasInstructions = false
                do {
                    val flags = composites.u16()
                    composites.u16() // glyph index
                    var skip = if (flags and ARG_1_AND_2_ARE_WORDS != 0) 4 else 2
                    skip += when {
                        flags and WE_HAVE_A_SCALE != 0 -> 2
                        flags and WE_HAVE_AN_X_AND_Y_SCALE != 0 -> 4
                        flags and WE_HAVE_A_TWO_BY_TWO != 0 -> 8
                        else -> 0
                    }
                    composites.bytes(skip)
                    if (flags and WE_HAVE_INSTRUCTIONS != 0) hasInstructions = true
                } while (flags and MORE_COMPONENTS != 0)
                val record = data.copyOfRange(start, composites.position)
                glyf.u16(-1)
                for (v in bbox) glyf.u16(v)
                glyf.bytes(record)
                if (hasInstructions) {
                    val length = glyphStream.u255()
                    glyf.u16(length)
                    glyf.bytes(instructions.bytes(length))
                }
                xMins[glyph] = bbox[0]
            }

            else -> {
                val endPoints = IntArray(contourCount)
                var total = 0
                for (c in 0 until contourCount) {
                    total += points.u255()
                    endPoints[c] = total - 1
                }
                val xs = IntArray(total)
                val ys = IntArray(total)
                val onCurve = BooleanArray(total)
                var x = 0
                var y = 0
                for (p in 0 until total) {
                    val flag = flagStream.u8()
                    onCurve[p] = flag and 0x80 == 0
                    val f = flag and 0x7f
                    val dx: Int
                    val dy: Int
                    when {
                        f < 10 -> {
                            dx = 0
                            dy = withSign(f, ((f and 14) shl 7) + glyphStream.u8())
                        }
                        f < 20 -> {
                            dx = withSign(f, (((f - 10) and 14) shl 7) + glyphStream.u8())
                            dy = 0
                        }
                        f < 84 -> {
                            val b0 = f - 20
                            val b1 = glyphStream.u8()
                            dx = withSign(f, 1 + (b0 and 0x30) + (b1 shr 4))
                            dy = withSign(f shr 1, 1 + ((b0 and 0x0c) shl 2) + (b1 and 0x0f))
                        }
                        f < 120 -> {
                            val b0 = f - 84
                            dx = withSign(f, 1 + ((b0 / 12) shl 8) + glyphStream.u8())
                            dy = withSign(f shr 1, 1 + (((b0 % 12) shr 2) shl 8) + glyphStream.u8())
                        }
                        f < 124 -> {
                            val b1 = glyphStream.u8()
                            val b2 = glyphStream.u8()
                            val b3 = glyphStream.u8()
                            dx = withSign(f, (b1 shl 4) + (b2 shr 4))
                            dy = withSign(f shr 1, ((b2 and 0x0f) shl 8) + b3)
                        }
                        else -> {
                            val b1 = glyphStream.u8()
                            val b2 = glyphStream.u8()
                            val b3 = glyphStream.u8()
                            val b4 = glyphStream.u8()
                            dx = withSign(f, (b1 shl 8) + b2)
                            dy = withSign(f shr 1, (b3 shl 8) + b4)
                        }
                    }
                    x += dx
                    y += dy
                    xs[p] = x
                    ys[p] = y
                }
                val instructionLength = glyphStream.u255()
                val bbox = if (hasBbox(glyph)) IntArray(4) { bboxes.s16() } else intArrayOf(
                    xs.minOrNull() ?: 0, ys.minOrNull() ?: 0, xs.maxOrNull() ?: 0, ys.maxOrNull() ?: 0
                )
                glyf.u16(contourCount)
                for (v in bbox) glyf.u16(v)
                for (e in endPoints) glyf.u16(e)
                glyf.u16(instructionLength)
                glyf.bytes(instructions.bytes(instructionLength))
                val overlap = overlaps != null && (overlaps[glyph shr 3].toInt() and (0x80 shr (glyph and 7))) != 0
                // the flags, then the coordinates as deltas
                val coordinates = ByteWriter(total * 4)
                val ycoordinates = ByteWriter(total * 2)
                var px = 0
                var py = 0
                for (p in 0 until total) {
                    var flag = if (onCurve[p]) ON_CURVE else 0
                    if (p == 0 && overlap) flag = flag or OVERLAP_SIMPLE
                    val dx = xs[p] - px
                    val dy = ys[p] - py
                    when {
                        dx == 0 -> flag = flag or X_SAME_OR_POSITIVE
                        dx in -255..255 -> {
                            flag = flag or X_SHORT or (if (dx > 0) X_SAME_OR_POSITIVE else 0)
                            coordinates.u8(if (dx > 0) dx else -dx)
                        }
                        else -> coordinates.u16(dx)
                    }
                    when {
                        dy == 0 -> flag = flag or Y_SAME_OR_POSITIVE
                        dy in -255..255 -> {
                            flag = flag or Y_SHORT or (if (dy > 0) Y_SAME_OR_POSITIVE else 0)
                            ycoordinates.u8(if (dy > 0) dy else -dy)
                        }
                        else -> ycoordinates.u16(dy)
                    }
                    glyf.u8(flag)
                    px = xs[p]
                    py = ys[p]
                }
                glyf.bytes(coordinates.toByteArray())
                glyf.bytes(ycoordinates.toByteArray())
                xMins[glyph] = bbox[0]
            }
        }
        glyf.pad(if (indexFormat == 0) 2 else 4)
    }
    offsets[glyphCount] = glyf.size

    val loca = ByteWriter((glyphCount + 1) * 4)
    for (o in offsets) {
        if (indexFormat == 0) loca.u16(o / 2) else loca.u32(o.toLong())
    }
    return Triple(glyf.toByteArray(), loca.toByteArray(), xMins)
}

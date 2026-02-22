package org.openrndr.fontdriver.android

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

internal class TtfReader(private val bytes: ByteArray) {
    private val bb: ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    data class Table(val tag: String, val offset: Int, val length: Int)
    data class Head(val unitsPerEm: Int, val xMin: Int, val yMin: Int, val xMax: Int, val yMax: Int)
    data class Hhea(val ascent: Int, val descent: Int, val lineGap: Int)

    private val tables: Map<String, Table> = readTableDirectory()

    fun readHead(): Head {
        val t = tables["head"] ?: error("TTF table 'head' not found")
        val b = sliceAt(t.offset, t.length)

        // head: unitsPerEm at offset 18 (uint16)
        val unitsPerEm = b.u16(18)

        // xMin,yMin,xMax,yMax at offsets 36..43 (int16)
        val xMin = b.s16(36)
        val yMin = b.s16(38)
        val xMax = b.s16(40)
        val yMax = b.s16(42)

        return Head(unitsPerEm, xMin, yMin, xMax, yMax)
    }

    fun readHhea(): Hhea {
        val t = tables["hhea"] ?: error("TTF table 'hhea' not found")
        val b = sliceAt(t.offset, t.length)

        // hhea: ascender, descender, lineGap at offsets 4,6,8 (int16)
        val ascent = b.s16(4)
        val descent = b.s16(6)
        val lineGap = b.s16(8)
        return Hhea(ascent, descent, lineGap)
    }

    fun readCmapCodePoints(): IntArray {
        val t = tables["cmap"] ?: error("TTF table 'cmap' not found")
        val b = sliceAt(t.offset, t.length)

        // cmap header
        val numTables = b.u16(2)
        // Choose best subtable: prefer format 12 (platform 3, encoding 10) or (0,4)
        var chosenOffset: Int? = null
        var chosenFormat = -1

        for (i in 0 until numTables) {
            val rec = 4 + i * 8
            val platformID = b.u16(rec)
            val encodingID = b.u16(rec + 2)
            val subOffset = b.u32(rec + 4)

            val sub = sliceAt(t.offset + subOffset, t.length - subOffset)
            val format = sub.u16(0)

            val prefer =
                (format == 12 && platformID == 3 && encodingID == 10) ||
                        (format == 12 && platformID == 0) ||
                        (format == 4 && platformID == 3 && encodingID == 1) ||
                        (format == 4 && platformID == 0)

            if (prefer) {
                // Prefer format 12 over 4
                if (chosenOffset == null || (format == 12 && chosenFormat != 12)) {
                    chosenOffset = subOffset
                    chosenFormat = format
                }
            }
        }

        // Fallback: first subtable
        if (chosenOffset == null) {
            chosenOffset = b.u32(4 + 0 * 8)
            chosenFormat = sliceAt(t.offset + chosenOffset, t.length - chosenOffset).u16(0)
        }

        val sub = sliceAt(t.offset + chosenOffset, t.length - chosenOffset)
        return when (chosenFormat) {
            4 -> readCmapFormat4(sub)
            12 -> readCmapFormat12(sub)
            else -> {
                // As a safe fallback (not ideal), scan BMP and keep those paint says exist would require Android APIs.
                // Here we just return empty if unsupported format.
                IntArray(0)
            }
        }
    }

    private fun readCmapFormat4(sub: ByteBuffer): IntArray {
        // Format 4: segment mapping to delta values (BMP only)
        val segCountX2 = sub.u16(6)
        val segCount = segCountX2 / 2

        val endCodeOff = 14
        val startCodeOff = endCodeOff + 2 * segCount + 2
        val idDeltaOff = startCodeOff + 2 * segCount
        val idRangeOffOff = idDeltaOff + 2 * segCount
        val glyphArrayOff = idRangeOffOff + 2 * segCount

        val out = ArrayList<Int>(4096)

        for (s in 0 until segCount) {
            val endCode = sub.u16(endCodeOff + 2 * s)
            val startCode = sub.u16(startCodeOff + 2 * s)
            val idDelta = sub.s16(idDeltaOff + 2 * s)
            val idRangeOffset = sub.u16(idRangeOffOff + 2 * s)

            // endCode == 0xFFFF sentinel segment often present
            if (startCode == 0xFFFF && endCode == 0xFFFF) continue

            for (cp in startCode..endCode) {
                val glyphIndex = if (idRangeOffset == 0) {
                    // (cp + idDelta) % 65536
                    ((cp + idDelta) and 0xFFFF)
                } else {
                    val roPos = idRangeOffOff + 2 * s
                    val glyphIndexAddr = roPos + idRangeOffset + 2 * (cp - startCode)
                    if (glyphIndexAddr + 2 > sub.capacity()) 0 else sub.u16(glyphIndexAddr)
                }

                if (glyphIndex != 0) out.add(cp)
            }
        }
        return out.toIntArray()
    }

    private fun readCmapFormat12(sub: ByteBuffer): IntArray {
        // Format 12: groups of (startCharCode, endCharCode, startGlyphId)
        val nGroups = sub.u32(12)
        val out = ArrayList<Int>(8192)
        var off = 16
        for (i in 0 until nGroups) {
            val startChar = sub.u32(off)
            val endChar = sub.u32(off + 4)
            // val startGlyph = sub.u32(off + 8) // not needed for codepoints set
            for (cp in startChar..endChar) out.add(cp)
            off += 12
        }
        return out.toIntArray()
    }

    private fun readTableDirectory(): Map<String, Table> {

        require(bb.capacity() >= 12) { "Font data too small (${bb.capacity()} bytes)" }

        val sig = bb.u32(0)
        val ok = (sig == 0x00010000) || (sig == 0x4F54544F) // 'OTTO'
        if (!ok) {
            val t = bb.tag(0)
            error("Unsupported font signature 0x${sig.toUInt().toString(16)} ('$t'). " +
                    "Need sfnt TTF (0x00010000) or OTF 'OTTO'.")
        }

        val numTables = bb.u16(4)
        val dirSize = 12 + numTables * 16
        require(dirSize <= bb.capacity()) {
            "Invalid table directory: numTables=$numTables dirSize=$dirSize cap=${bb.capacity()}"
        }

        // Offset table
//        val numTables = bb.u16(4)
        val map = HashMap<String, Table>(numTables)

        var off = 12
        repeat(numTables) {
            val tag = bb.tag(off)
            // val checksum = bb.u32(off + 4)
            val offset = bb.u32(off + 8)
            val length = bb.u32(off + 12)
            map[tag] = Table(tag, offset, length)
            off += 16
        }
        return map
    }

    private fun sliceAt(offset: Int, length: Int): ByteBuffer {
        val dup = bb.duplicate().order(ByteOrder.BIG_ENDIAN)
        dup.position(offset)
        dup.limit(min(offset + length, dup.capacity()))
        return dup.slice().order(ByteOrder.BIG_ENDIAN)
    }

    // ---- ByteBuffer helpers (big endian) ----
    private fun ByteBuffer.u16(off: Int): Int =
        (get(off).toInt() and 0xFF shl 8) or (get(off + 1).toInt() and 0xFF)

    private fun ByteBuffer.s16(off: Int): Int = getShort(off).toInt()
    private fun ByteBuffer.u32(off: Int): Int {
        val b0 = get(off).toInt() and 0xFF
        val b1 = get(off + 1).toInt() and 0xFF
        val b2 = get(off + 2).toInt() and 0xFF
        val b3 = get(off + 3).toInt() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    private fun ByteBuffer.tag(off: Int): String {
        val c0 = (get(off + 0).toInt() and 0xFF).toChar()
        val c1 = (get(off + 1).toInt() and 0xFF).toChar()
        val c2 = (get(off + 2).toInt() and 0xFF).toChar()
        val c3 = (get(off + 3).toInt() and 0xFF).toChar()
        return "" + c0 + c1 + c2 + c3
    }
}
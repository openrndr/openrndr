package org.openrndr.draw.font.internal

import org.openrndr.draw.font.*

/**
 * A [MathTable] read from the bytes of an OpenType MATH [table], for drivers whose libraries do not read MATH tables
 * themselves, such as harfbuzzjs. Values in font units are multiplied by [scale], the pixels per font unit of the
 * face, the percent constants are given as stored.
 *
 * Device and variation tables of values are not applied: values are those of the default instance of variable fonts,
 * without adjustments for sizes in pixels.
 *
 * @see <a href="https://learn.microsoft.com/en-us/typography/opentype/spec/math">The OpenType MATH table</a>
 */
class OpenTypeMathTable(private val table: ByteArray, private val scale: Double) : MathTable {
    private fun uint16(offset: Int): Int =
        ((table[offset].toInt() and 0xff) shl 8) or (table[offset + 1].toInt() and 0xff)

    private fun int16(offset: Int): Int = uint16(offset).toShort().toInt()

    /** the offset of the table at the 16-bit offset stored at [offset] relative to [base], null when it is 0 */
    private fun subtable(base: Int, offset: Int): Int? = uint16(offset).let { if (it == 0) null else base + it }

    private val constants = subtable(0, 4)
    private val glyphInfo = subtable(0, 6)
    private val variants = subtable(0, 8)

    private val italicsCorrection = glyphInfo?.let { subtable(it, it) }
    private val topAccentAttachment = glyphInfo?.let { subtable(it, it + 2) }
    private val extendedShapeCoverage = glyphInfo?.let { subtable(it, it + 4) }
    private val kernInfo = glyphInfo?.let { subtable(it, it + 6) }

    /** the value of the MathValueRecord at [offset], in font units */
    private fun mathValue(offset: Int): Int = int16(offset)

    /** the index of [glyph] in the Coverage table at [coverage], null when it is not covered */
    private fun coverageIndex(coverage: Int, glyph: Int): Int? {
        when (uint16(coverage)) {
            1 -> {
                // sorted glyph ids
                var low = 0
                var high = uint16(coverage + 2) - 1
                while (low <= high) {
                    val middle = (low + high) ushr 1
                    val id = uint16(coverage + 4 + middle * 2)
                    when {
                        id < glyph -> low = middle + 1
                        id > glyph -> high = middle - 1
                        else -> return middle
                    }
                }
            }
            2 -> {
                // sorted ranges of glyph ids
                var low = 0
                var high = uint16(coverage + 2) - 1
                while (low <= high) {
                    val middle = (low + high) ushr 1
                    val record = coverage + 4 + middle * 6
                    when {
                        uint16(record + 2) < glyph -> low = middle + 1
                        uint16(record) > glyph -> high = middle - 1
                        else -> return uint16(record + 4) + glyph - uint16(record)
                    }
                }
            }
        }
        return null
    }

    /** the MathValueRecord of [glyph] in a table of a coverage offset, a count and records, null when it has none */
    private fun glyphValue(subtable: Int?, glyph: Int): Int? {
        if (subtable == null) return null
        val coverage = subtable(subtable, subtable) ?: return null
        val index = coverageIndex(coverage, glyph) ?: return null
        if (index >= uint16(subtable + 2)) return null
        return mathValue(subtable + 4 + index * 4)
    }

    override fun constant(constant: MathConstant): Double {
        val base = constants ?: return 0.0
        return when (val i = constant.ordinal) {
            // scriptPercentScaleDown and scriptScriptPercentScaleDown, int16
            0, 1 -> int16(base + i * 2).toDouble()
            // delimitedSubFormulaMinHeight and displayOperatorMinHeight, UFWORD
            2, 3 -> uint16(base + i * 2) * scale
            // radicalDegreeBottomRaisePercent, int16 after the 51 MathValueRecords
            MathConstant.RADICAL_DEGREE_BOTTOM_RAISE_PERCENT.ordinal -> int16(base + 8 + 51 * 4).toDouble()
            else -> mathValue(base + 8 + (i - 4) * 4) * scale
        }
    }

    override fun italicCorrection(glyphIndex: Int): Double =
        (glyphValue(italicsCorrection, glyphIndex) ?: 0) * scale

    override fun topAccentAttachment(glyphIndex: Int): Double? =
        glyphValue(topAccentAttachment, glyphIndex)?.let { it * scale }

    override fun isExtendedShape(glyphIndex: Int): Boolean =
        extendedShapeCoverage != null && coverageIndex(extendedShapeCoverage, glyphIndex) != null

    override fun kern(glyphIndex: Int, corner: MathKernCorner, correctionHeight: Double): Double {
        val info = kernInfo ?: return 0.0
        val coverage = subtable(info, info) ?: return 0.0
        val index = coverageIndex(coverage, glyphIndex) ?: return 0.0
        if (index >= uint16(info + 2)) return 0.0
        val kern = subtable(info, info + 4 + index * 8 + corner.ordinal * 2) ?: return 0.0
        // the kern values are between the correction heights, the first index whose height is not below the
        // correction height is the one that applies, like std::upper_bound in HarfBuzz
        val heightCount = uint16(kern)
        val heights = kern + 2
        val values = heights + heightCount * 4
        var i = 0
        var count = heightCount
        while (count > 0) {
            val half = count / 2
            if (mathValue(heights + (i + half) * 4) * scale < correctionHeight) {
                i += half + 1
                count -= half + 1
            } else {
                count = half
            }
        }
        return mathValue(values + i * 4) * scale
    }

    /** the MathGlyphConstruction of [glyph] in [direction], null when it has none */
    private fun construction(glyph: Int, direction: MathDirection): Int? {
        val base = variants ?: return null
        val vertical = direction == MathDirection.VERTICAL
        val coverage = subtable(base, base + if (vertical) 2 else 4) ?: return null
        val index = coverageIndex(coverage, glyph) ?: return null
        val vertCount = uint16(base + 6)
        val count = if (vertical) vertCount else uint16(base + 8)
        if (index >= count) return null
        val offsets = base + 10 + if (vertical) 0 else vertCount * 2
        return subtable(base, offsets + index * 2)
    }

    override fun variants(glyphIndex: Int, direction: MathDirection): List<MathGlyphVariant> {
        val construction = construction(glyphIndex, direction) ?: return emptyList()
        return List(uint16(construction + 2)) {
            val record = construction + 4 + it * 4
            MathGlyphVariant(uint16(record), uint16(record + 2) * scale)
        }
    }

    override fun assembly(glyphIndex: Int, direction: MathDirection): MathGlyphAssembly? {
        val construction = construction(glyphIndex, direction) ?: return null
        val assembly = subtable(construction, construction) ?: return null
        val parts = List(uint16(assembly + 4)) {
            val record = assembly + 6 + it * 10
            MathGlyphPart(
                uint16(record),
                uint16(record + 2) * scale,
                uint16(record + 4) * scale,
                uint16(record + 6) * scale,
                (uint16(record + 8) and 1) != 0
            )
        }
        return MathGlyphAssembly(parts, mathValue(assembly) * scale)
    }

    override fun minConnectorOverlap(direction: MathDirection): Double =
        variants?.let { uint16(it) * scale } ?: 0.0
}

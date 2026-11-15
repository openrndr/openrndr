import org.openrndr.draw.font.MathConstant
import org.openrndr.draw.font.MathDirection
import org.openrndr.draw.font.MathKernCorner
import org.openrndr.draw.font.MathTable
import org.openrndr.draw.font.internal.OpenTypeMathTable
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Compares the MATH table read by [OpenTypeMathTable], which the harfbuzzjs driver uses, with the one HarfBuzz reads,
 * for every glyph of STIX Two Math. Skipped when the font is not found, see [TestMathTableHarfBuzz].
 */
class TestOpenTypeMathTable {
    private val fontFile = File(
        System.getProperty("org.openrndr.test.mathFont") ?: "/System/Library/Fonts/Supplemental/STIXTwoMath.otf"
    )
    private val size = 32.0

    /** the bytes of the table [tag] of the font in [bytes] */
    private fun table(bytes: ByteArray, tag: String): ByteArray {
        fun u16(o: Int) = ((bytes[o].toInt() and 0xff) shl 8) or (bytes[o + 1].toInt() and 0xff)
        fun u32(o: Int) = (u16(o).toLong() shl 16) or u16(o + 2).toLong()
        for (i in 0 until u16(4)) {
            val record = 12 + i * 16
            if (String(bytes, record, 4, Charsets.ISO_8859_1) == tag) {
                val offset = u32(record + 8).toInt()
                return bytes.copyOfRange(offset, offset + u32(record + 12).toInt())
            }
        }
        fail("no $tag table")
    }

    /** HarfBuzz rounds to 1/64 pixel */
    private fun assertClose(expected: Double, actual: Double, what: String) {
        assertTrue(abs(expected - actual) <= 1.0 / 64 + 1E-4 * abs(expected), "$what: HarfBuzz $expected, read $actual")
    }

    @Test
    fun `the MATH table read from its bytes is the one HarfBuzz reads`() {
        if (!fontFile.exists()) return
        val bytes = fontFile.readBytes()
        val head = table(bytes, "head")
        val unitsPerEm = ((head[18].toInt() and 0xff) shl 8) or (head[19].toInt() and 0xff)
        val maxp = table(bytes, "maxp")
        val glyphCount = ((maxp[4].toInt() and 0xff) shl 8) or (maxp[5].toInt() and 0xff)

        val harfBuzz: MathTable = TextShapingDriverHarfBuzz().mathTable(FontDriverFreetype().loadFace(fontFile.path).withSize(size))!!
        val read: MathTable = OpenTypeMathTable(table(bytes, "MATH"), size / unitsPerEm)

        for (constant in MathConstant.entries) {
            assertClose(harfBuzz.constant(constant), read.constant(constant), "$constant")
        }
        for (direction in MathDirection.entries) {
            assertClose(harfBuzz.minConnectorOverlap(direction), read.minConnectorOverlap(direction), "overlap $direction")
        }
        var variants = 0
        var assemblies = 0
        var kerns = 0
        for (glyph in 0 until glyphCount) {
            assertClose(harfBuzz.italicCorrection(glyph), read.italicCorrection(glyph), "italic correction of $glyph")
            val attachment = read.topAccentAttachment(glyph)
            val hbAttachment = harfBuzz.topAccentAttachment(glyph)
            // HarfBuzz gives no attachment when the font stores half the advance, see MathTableHarfBuzz
            if (hbAttachment != null || attachment == null) {
                assertEquals(hbAttachment == null, attachment == null, "top accent attachment of $glyph")
                if (attachment != null) assertClose(hbAttachment!!, attachment, "top accent attachment of $glyph")
            }
            assertEquals(harfBuzz.isExtendedShape(glyph), read.isExtendedShape(glyph), "extended shape $glyph")
            for (corner in MathKernCorner.entries) {
                for (height in listOf(-17.3, 0.3, 4.7, 11.1, 26.9)) {
                    val k = read.kern(glyph, corner, height)
                    if (k != 0.0) kerns++
                    assertClose(harfBuzz.kern(glyph, corner, height), k, "kern of $glyph $corner at $height")
                }
            }
            for (direction in MathDirection.entries) {
                val expected = harfBuzz.variants(glyph, direction)
                val actual = read.variants(glyph, direction)
                assertEquals(expected.map { it.glyphIndex }, actual.map { it.glyphIndex }, "variants of $glyph $direction")
                expected.zip(actual).forEach { (e, a) -> assertClose(e.advance, a.advance, "variant advance of $glyph") }
                if (actual.isNotEmpty()) variants++

                val expectedAssembly = harfBuzz.assembly(glyph, direction)
                val actualAssembly = read.assembly(glyph, direction)
                assertEquals(expectedAssembly == null, actualAssembly == null, "assembly of $glyph $direction")
                if (expectedAssembly != null && actualAssembly != null) {
                    assemblies++
                    assertClose(expectedAssembly.italicCorrection, actualAssembly.italicCorrection, "assembly italics of $glyph")
                    assertEquals(expectedAssembly.parts.map { it.glyphIndex to it.isExtender },
                        actualAssembly.parts.map { it.glyphIndex to it.isExtender }, "parts of $glyph")
                    expectedAssembly.parts.zip(actualAssembly.parts).forEach { (e, a) ->
                        assertClose(e.fullAdvance, a.fullAdvance, "part advance of $glyph")
                        assertClose(e.startConnectorLength, a.startConnectorLength, "part start of $glyph")
                        assertClose(e.endConnectorLength, a.endConnectorLength, "part end of $glyph")
                    }
                }
            }
        }
        // the comparison covers the parts of the table
        assertTrue(variants > 100 && assemblies > 20 && kerns > 100, "variants $variants, assemblies $assemblies, kerns $kerns")
    }
}

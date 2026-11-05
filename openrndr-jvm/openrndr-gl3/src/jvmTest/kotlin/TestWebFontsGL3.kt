import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.bytesOfDataUrl
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.font.webfont.WebFontException
import org.openrndr.draw.font.webfont.sfntFontBytes
import org.openrndr.resourceUrl
import java.io.File
import java.net.URI
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * WOFF and WOFF2 fonts are decoded to TrueType fonts, fonts can be loaded from data urls
 */
class TestWebFontsGL3 : AbstractApplicationTestFixture() {
    private fun resourceBytes(name: String) = URI(resourceUrl("/fonts/$name")).toURL().readBytes()
    private val woff by lazy { resourceBytes("DejaVuLGCSansMono-BoldOblique.woff") }
    private val woff2 by lazy { resourceBytes("DejaVuLGCSansMono-BoldOblique.woff2") }

    /** the tables of a TrueType font by tag */
    private fun tables(font: ByteArray): Map<String, ByteArray> {
        fun u16(o: Int) = ((font[o].toInt() and 0xff) shl 8) or (font[o + 1].toInt() and 0xff)
        fun u32(o: Int) = (u16(o).toLong() shl 16) or u16(o + 2).toLong()
        return (0 until u16(4)).associate { i ->
            val entry = 12 + 16 * i
            val offset = u32(entry + 8).toInt()
            String(font.copyOfRange(entry, entry + 4), Charsets.ISO_8859_1) to font.copyOfRange(offset, offset + u32(entry + 12).toInt())
        }
    }

    @Test
    fun woffAndWoff2DecodeToTheSameTables() {
        // the WOFF and WOFF2 fonts are made of the same TrueType font, the tables that WOFF2 does not transform are
        // the same. glyf and loca are encoded anew, the checksum adjustment in head differs with them, and WOFF2
        // encoders set bit 11 of the flags in head
        val fromWoff = tables(sfntFontBytes(woff))
        val fromWoff2 = tables(sfntFontBytes(woff2))
        assertEquals(fromWoff.keys, fromWoff2.keys)
        for (tag in fromWoff.keys - setOf("glyf", "loca", "head")) {
            assertContentEquals(fromWoff.getValue(tag), fromWoff2.getValue(tag), "table '$tag'")
        }
        val head = fromWoff.getValue("head")
        val head2 = fromWoff2.getValue("head")
        assertContentEquals(head.copyOfRange(12, 16), head2.copyOfRange(12, 16), "magic number in 'head'")
        assertEquals(head[16].toInt() or 0x08, head2[16].toInt(), "flags in 'head'")
        assertContentEquals(head.copyOfRange(17, 54), head2.copyOfRange(17, 54), "table 'head'")
    }

    private fun assertSameGlyphs(expected: Face, actual: Face, glyphs: Int) {
        for (glyph in 0 until glyphs) {
            val e = expected.glyphForIndex(glyph)
            val a = actual.glyphForIndex(glyph)
            assertEquals(e.advanceWidth(), a.advanceWidth(), "advance of glyph $glyph")
            assertEquals(e.leftSideBearing(), a.leftSideBearing(), "left side bearing of glyph $glyph")
            assertEquals(e.shape().contours.map { it.segments }, a.shape().contours.map { it.segments }, "outline of glyph $glyph")
        }
    }

    @Test
    fun woffAndWoff2FacesHaveTheSameGlyphs() {
        val face = loadFace(resourceUrl("/fonts/DejaVuLGCSansMono-BoldOblique.woff")).withSize(64.0)
        val face2 = loadFace(resourceUrl("/fonts/DejaVuLGCSansMono-BoldOblique.woff2")).withSize(64.0)
        assertEquals(face.unitsPerEm(), face2.unitsPerEm())
        assertEquals(face.ascent, face2.ascent)
        assertEquals(face.descent, face2.descent)
        assertTrue(face2.glyphForCharacter('g').shape().contours.isNotEmpty())
        val glyphs = tables(sfntFontBytes(woff)).getValue("maxp").let { ((it[4].toInt() and 0xff) shl 8) or (it[5].toInt() and 0xff) }
        assertSameGlyphs(face, face2, glyphs)
    }

    @Test
    fun fontsLoadFromDataUrls() {
        val base64 = "data:font/woff2;base64," + Base64.getEncoder().encodeToString(woff2)
        val face = loadFace(resourceUrl("/fonts/DejaVuLGCSansMono-BoldOblique.woff2")).withSize(64.0)
        assertSameGlyphs(face, loadFace(base64).withSize(64.0), 64)

        // percent-encoded, and base64 with line breaks
        val percent = "data:font/woff2," + woff2.joinToString("") { "%%%02X".format(it.toInt() and 0xff) }
        assertContentEquals(woff2, bytesOfDataUrl(percent))
        val wrapped = "data:application/font-woff2;charset=utf-8;base64," + Base64.getMimeEncoder().encodeToString(woff2)
        assertContentEquals(woff2, bytesOfDataUrl(wrapped))
        assertContentEquals("a,b c".toByteArray(), bytesOfDataUrl("data:,a,b%20c"))
    }

    @Test
    fun otherFontsAreNotChanged() {
        val ttf = resourceBytes("Roboto-Medium.ttf")
        assertTrue(ttf === sfntFontBytes(ttf))
    }

    @Test
    fun invalidWebFontsFail() {
        assertFailsWith<WebFontException> { sfntFontBytes(woff2.copyOf(woff2.size / 2)) }
        assertFailsWith<WebFontException> { sfntFontBytes(woff.copyOf(200)) }
    }
}

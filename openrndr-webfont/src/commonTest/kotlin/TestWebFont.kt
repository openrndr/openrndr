import org.openrndr.webfont.WebFontException
import org.openrndr.webfont.isWoff
import org.openrndr.webfont.isWoff2
import org.openrndr.webfont.sfntFontBytes
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestWebFont {
    @Test
    fun webFontsAreRecognizedBySignature() {
        assertTrue(isWoff("wOFF\u0000".encodeToByteArray()))
        assertTrue(isWoff2("wOF2\u0000".encodeToByteArray()))
        assertFalse(isWoff("wOF".encodeToByteArray()))
    }

    @Test
    fun otherDataIsNotChanged() {
        val ttf = byteArrayOf(0, 1, 0, 0, 0, 0)
        assertSame(ttf, sfntFontBytes(ttf))
    }

    @Test
    fun truncatedWebFontsFail() {
        assertFailsWith<WebFontException> { sfntFontBytes("wOF2".encodeToByteArray() + ByteArray(20)) }
        assertFailsWith<WebFontException> { sfntFontBytes("wOFF".encodeToByteArray() + ByteArray(20)) }
    }
}

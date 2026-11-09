import org.openrndr.draw.font.loadFace
import org.openrndr.resourceUrl
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Glyph outlines and metrics of FreeType faces are unhinted, they scale with the face size
 */
class TestUnhintedOutlinesGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `outlines and metrics scale with the face size`() {
        val face = loadFace(resourceUrl("/fonts/Roboto-Medium.ttf"))
        val small = face.withSize(16.0)
        val large = face.withSize(72.0)
        val scale = 72.0 / 16.0
        // outline points and metrics are in 26.6 fixed point, rounded to 1/64 pixel at both sizes
        val tolerance = (scale + 1.0) / 128.0 + 1E-6
        fun assertClose(expected: Double, actual: Double, message: String) =
            assertTrue(abs(expected - actual) <= tolerance, "$message: expected $expected, was $actual")
        for (c in "sixeAgW") {
            val s = small.glyphForCharacter(c)
            val l = large.glyphForCharacter(c)
            val sb = s.shape().bounds
            val lb = l.shape().bounds
            assertClose(lb.x, sb.x * scale, "'$c' outline x")
            assertClose(lb.y, sb.y * scale, "'$c' outline y")
            assertClose(lb.width, sb.width * scale, "'$c' outline width")
            assertClose(lb.height, sb.height * scale, "'$c' outline height")
            assertClose(l.advanceWidth(), s.advanceWidth() * scale, "'$c' advance")
            assertClose(l.leftSideBearing(), s.leftSideBearing() * scale, "'$c' left side bearing")
            assertClose(l.bounds().width, s.bounds().width * scale, "'$c' bounds width")
        }
    }
}

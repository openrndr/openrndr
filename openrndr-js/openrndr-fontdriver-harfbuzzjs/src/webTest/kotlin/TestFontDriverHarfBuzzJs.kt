import kotlinx.coroutines.test.runTest
import org.openrndr.draw.font.Face
import org.openrndr.fontdriver.harfbuzzjs.FontDriverHarfBuzzJs
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compares the harfbuzzjs font driver with values of the FreeType font driver on the JVM. FreeType hints outlines and
 * bounds to the pixel grid, harfbuzzjs does not hint: the outline of an 'o' overshoots the baseline and x-height by
 * about 0.3 pixels at 32 pixels per em, which hinting removes. Outlines and bounds are compared within that.
 */
class TestFontDriverHarfBuzzJs {
    private val roboto = "openrndr-jvm/openrndr-gl3/src/jvmTest/resources/fonts/Roboto-Medium.ttf"
    private val platypi = "data/fonts/Platypi-Variable.ttf"

    private fun assertBounds(expected: Rectangle, actual: Rectangle, tolerance: Double, message: String) {
        assertTrue(
            abs(expected.x - actual.x) <= tolerance && abs(expected.y - actual.y) <= tolerance &&
                    abs(expected.width - actual.width) <= tolerance && abs(expected.height - actual.height) <= tolerance,
            "$message: expected $expected but was $actual"
        )
    }

    private suspend fun face(file: String): Face? {
        val root = repositoryRoot() ?: return null
        return FontDriverHarfBuzzJs.create().loadFace("$root/$file").withSize(32.0)
    }

    @Test
    fun metrics() = runTest {
        val face = face(roboto) ?: return@runTest
        assertEquals(2048, face.unitsPerEm())
        assertEquals(1900, face.ascentMetrics())
        assertEquals(-500, face.descentMetrics())
        assertEquals(29.6875, face.ascent, 1E-9)
        assertEquals(-7.8125, face.descent, 1E-9)
        assertEquals(37.5, face.height, 1E-9)
        assertEquals(0.015625, face.emWidth, 1E-12)
        assertEquals(17.0, face.xHeight, 1.0)
        assertEquals(23.0, face.capHeight, 1.0)
        assertEquals(Rectangle(-1500.0, -555.0, 3896.0, 2718.0), face.bounds)
        assertTrue(face.allCodePoints().contains('A'.code))
    }

    @Test
    fun glyphs() = runTest {
        val face = face(roboto) ?: return@runTest
        val expected = mapOf(
            'H' to Triple(44, 22.734375, Rectangle(2.3125, -23.0, 18.0625, 23.0)),
            'o' to Triple(83, 18.21875, Rectangle(1.234375, -17.0, 15.71875, 17.0)),
            'g' to Triple(75, 18.140625, Rectangle(1.28125, -17.0, 14.90625, 24.0)),
            'V' to Triple(58, 20.703125, Rectangle(0.28125, -23.0, 20.171875, 23.0))
        )
        for ((c, values) in expected) {
            val (index, advance, shapeBounds) = values
            val glyph = face.glyphForCharacter(c)
            assertEquals(index, glyph.index, "index of $c")
            assertEquals(advance, glyph.advanceWidth(), 1E-9, "advance of $c")
            assertBounds(shapeBounds, glyph.shape().bounds, 0.6, "outline of $c")
            assertBounds(shapeBounds, glyph.bounds(), 0.6, "bounds of $c")
        }
    }

    @Test
    fun kerning() = runTest {
        val face = face(roboto) ?: return@runTest
        assertEquals(-1.203125, face.kernAdvance('A', 'V'), 1E-9)
        assertEquals(-3.25, face.kernAdvance('T', 'o'), 1E-9)
        assertEquals(0.0, face.kernAdvance('H', 'H'), 1E-9)
    }

    @Test
    fun rasterization() = runTest {
        val face = face(roboto) ?: return@runTest
        val glyph = face.glyphForCharacter('o')
        val bounds = glyph.bitmapBounds(1.0)
        // FreeType's bitmap is 16 x 17 at (1, -17), the overshoots add a row above and below
        assertTrue(abs(bounds.x - 1) <= 1 && abs(bounds.y + 17) <= 1 && abs(bounds.width - 16) <= 1 && abs(bounds.height - 17) <= 2, "bitmap bounds $bounds")
        val bitmap = MPPBuffer.allocate(bounds.width * bounds.height)
        glyph.rasterize(1.0, bitmap, bounds.width, true)
        val coverage = IntArray(bounds.width * bounds.height) { bitmap.get().toInt() and 0xff }
        // the ink of the bitmap is the area of the glyph, FreeType's bitmap of 'o' holds 36317
        assertEquals(36317.0, coverage.sum().toDouble(), 36317.0 * 0.03)
        // the counter of the 'o' is empty, its stroke covered
        assertEquals(0, coverage[(bounds.height / 2) * bounds.width + bounds.width / 2], "center of the counter")
        assertEquals(255, coverage[(bounds.height / 2) * bounds.width + 2], "left stroke")

        // at a content scale of 2 the bitmap has four times the ink
        val bounds2 = glyph.bitmapBounds(2.0)
        val bitmap2 = MPPBuffer.allocate(bounds2.width * bounds2.height)
        glyph.rasterize(2.0, bitmap2, bounds2.width, true)
        val ink2 = (0 until bounds2.width * bounds2.height).sumOf { bitmap2.get().toInt() and 0xff }
        assertEquals(4.0 * coverage.sum(), ink2.toDouble(), 4.0 * coverage.sum() * 0.02)
    }

    @Test
    fun variableFaces() = runTest {
        val face = face(platypi) ?: return@runTest
        assertTrue(face.isVariable)
        assertEquals(mapOf("Weight" to 300.0), face.master.axes)
        assertEquals(300.0..800.0, face.master.ranges["Weight"])
        assertEquals(24.192047119140625, face.glyphForCharacter('H').advanceWidth(), 1E-3)
        val heavy = face.withMaster(face.master.apply { put("Weight", 800.0) })
        assertEquals(26.624053955078125, heavy.glyphForCharacter('H').advanceWidth(), 1E-3)
        assertTrue(heavy.glyphForCharacter('H').shape().bounds.width > face.glyphForCharacter('H').shape().bounds.width)
    }

    @Test
    fun loadingWithoutBlocking() = runTest {
        val root = repositoryRoot() ?: return@runTest
        val face = FontDriverHarfBuzzJs.create().loadFaceSuspend("$root/$roboto").withSize(32.0)
        assertEquals(22.734375, face.glyphForCharacter('H').advanceWidth(), 1E-9)
    }
}

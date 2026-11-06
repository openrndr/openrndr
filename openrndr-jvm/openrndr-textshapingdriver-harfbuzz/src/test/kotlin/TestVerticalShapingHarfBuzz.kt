import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Text is shaped top to bottom with the vertical metrics of the font, results are in pixels with y pointing down.
 * The harfbuzzjs driver is tested against the same values.
 */
class TestVerticalShapingHarfBuzz {
    private val driver = TextShapingDriverHarfBuzz()
    private val fonts = FontDriverFreetype()
    private val mongolianText = "ᠮᠣᠩᠭᠣᠯ"

    private fun face(path: String): Face = fonts.loadFace(path).withSize(64.0)
    private val mongolian by lazy { face("../../data/fonts/NotoSansMongolian-Regular.ttf") }
    private val roboto by lazy { face("../openrndr-gl3/src/jvmTest/resources/fonts/Roboto-Medium.ttf") }

    private fun near(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) <= 1.0 / 32.0, "$message: expected $expected, was $actual")

    @Test
    fun verticalTextUsesVerticalMetrics() {
        val horizontal = driver.shape(mongolian, mongolianText, direction = Direction.LEFT_TO_RIGHT, script = Script.MONGOLIAN)
        val vertical = driver.shape(mongolian, mongolianText, direction = Direction.TOP_TO_BOTTOM, script = Script.MONGOLIAN)
        // vertical forms are substituted
        assertNotEquals(horizontal.map { it.glyphIndex }, vertical.map { it.glyphIndex })
        for (result in vertical) {
            // the advances of vmtx, one em down
            near(0.0, result.advance.x, "x advance")
            near(64.0, result.advance.y, "y advance")
            // the offsets are those of the horizontal origin from the vertical origin: centered, the origin of the font
            // lies 880 units of 1000 above the baseline
            near(-mongolian.glyphForIndex(result.glyphIndex).advanceWidth() / 2.0, result.offset.x, "x offset")
            near(56.3125, result.offset.y, "y offset")
        }
        // like right to left text, bottom to top text is in visual order: the glyphs are reversed, the advances point
        // down. The middle forms are the same, the initial and final forms are those of the other ends
        val upwards = driver.shape(mongolian, mongolianText, direction = Direction.BOTTOM_TO_TOP, script = Script.MONGOLIAN)
        assertEquals(vertical.size, upwards.size)
        assertEquals(vertical.map { it.glyphIndex }.subList(1, 5).reversed(), upwards.map { it.glyphIndex }.subList(1, 5))
        for (result in upwards) near(64.0, result.advance.y, "y advance bottom to top")
    }

    @Test
    fun fontsWithoutVerticalMetricsAreCenteredOnTheirLineHeight() {
        val vertical = driver.shape(roboto, "Ab", direction = Direction.TOP_TO_BOTTOM, script = Script.LATIN)
        // the advance is the ascender minus the descender, the glyph is centered on it
        for (result in vertical) near(roboto.ascent - roboto.descent, result.advance.y, "y advance")
        near(-21.296875, vertical[0].offset.x, "x offset of A")
        near(60.25, vertical[0].offset.y, "y offset of A")
        near(61.1875, vertical[1].offset.y, "y offset of b")
    }

    @Test
    fun offsetsPointDown() {
        val platypi = face("../../data/fonts/Platypi-Regular.ttf")
        // a second acute is stacked above the first, a second dot below under the first
        val above = driver.shape(platypi, "x́́", direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN)
        near(-15.6875, above[2].offset.y, "stacked acute")
        val below = driver.shape(platypi, "x̣̣", direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN)
        near(13.0625, below[2].offset.y, "stacked dot below")
        assertEquals(0.0, above[0].advance.y)
    }
}

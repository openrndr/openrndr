@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlinx.coroutines.test.runTest
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.fontdriver.harfbuzzjs.FontDriverHarfBuzzJs
import org.openrndr.textshapingdriver.harfbuzzjs.TextShapingDriverHarfBuzzJs
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun rootVariable(): JsString? =
    js("(globalThis.process && globalThis.process.env && globalThis.process.env.OPENRNDR_ROOT) || null")

/**
 * Vertical text and y-down results, with the values of the HarfBuzz text shaping driver on the JVM
 * (TestVerticalShapingHarfBuzz)
 */
class TestVerticalShapingHarfBuzzJs {
    private val driver = TextShapingDriverHarfBuzzJs()
    private val mongolianText = "ᠮᠣᠩᠭᠣᠯ"

    private suspend fun face(path: String): Face? {
        val root = rootVariable()?.toString() ?: return null
        return FontDriverHarfBuzzJs.create().loadFace("$root/$path").withSize(64.0)
    }

    /**
     * harfbuzzjs positions in whole font units, a unit of Noto Sans Mongolian is 0.064 pixels at 64 pixels per em, the
     * driver on the JVM in 1/64 pixels
     */
    private fun near(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) <= 0.04, "$message: expected $expected, was $actual")

    @Test
    fun verticalTextUsesVerticalMetrics() = runTest {
        val mongolian = face("data/fonts/NotoSansMongolian-Regular.ttf") ?: return@runTest
        val horizontal = driver.shape(mongolian, mongolianText, direction = Direction.LEFT_TO_RIGHT, script = Script.MONGOLIAN)
        val vertical = driver.shape(mongolian, mongolianText, direction = Direction.TOP_TO_BOTTOM, script = Script.MONGOLIAN)
        assertNotEquals(horizontal.map { it.glyphIndex }, vertical.map { it.glyphIndex })
        assertEquals(listOf(121, 32, 80, 115, 32, 129), vertical.map { it.glyphIndex })
        for (result in vertical) {
            near(0.0, result.advance.x, "x advance")
            near(64.0, result.advance.y, "y advance")
            near(-mongolian.glyphForIndex(result.glyphIndex).advanceWidth() / 2.0, result.offset.x, "x offset")
            near(56.3125, result.offset.y, "y offset")
        }
        val upwards = driver.shape(mongolian, mongolianText, direction = Direction.BOTTOM_TO_TOP, script = Script.MONGOLIAN)
        assertEquals(vertical.size, upwards.size)
        for (result in upwards) near(64.0, result.advance.y, "y advance bottom to top")
    }

    @Test
    fun fontsWithoutVerticalMetricsAreCenteredOnTheirLineHeight() = runTest {
        val roboto = face("openrndr-jvm/openrndr-gl3/src/jvmTest/resources/fonts/Roboto-Medium.ttf") ?: return@runTest
        val vertical = driver.shape(roboto, "Ab", direction = Direction.TOP_TO_BOTTOM, script = Script.LATIN)
        for (result in vertical) near(75.0, result.advance.y, "y advance")
        near(-21.296875, vertical[0].offset.x, "x offset of A")
        near(60.25, vertical[0].offset.y, "y offset of A")
        near(61.1875, vertical[1].offset.y, "y offset of b")
    }

    @Test
    fun offsetsPointDown() = runTest {
        val platypi = face("data/fonts/Platypi-Regular.ttf") ?: return@runTest
        val above = driver.shape(platypi, "x́́", direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN)
        near(-15.6875, above[2].offset.y, "stacked acute")
        val below = driver.shape(platypi, "x̣̣", direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN)
        near(13.0625, below[2].offset.y, "stacked dot below")
    }

    /** without vertical positioning in the font, a glyph is drawn at the pen minus its vertical origin */
    private fun assertVerticalMetrics(face: Face, text: String, script: Script) {
        for (result in driver.shape(face, text, direction = Direction.TOP_TO_BOTTOM, script = script)) {
            val glyph = face.glyphForIndex(result.glyphIndex)
            near(glyph.verticalAdvance, result.advance.y, "vertical advance of glyph ${result.glyphIndex}")
            near(-glyph.verticalOrigin.x, result.offset.x, "vertical origin x of glyph ${result.glyphIndex}")
            near(-glyph.verticalOrigin.y, result.offset.y, "vertical origin y of glyph ${result.glyphIndex}")
        }
    }

    @Test
    fun glyphsHaveTheVerticalMetricsOfTheShaper() = runTest {
        val mongolian = face("data/fonts/NotoSansMongolian-Regular.ttf") ?: return@runTest
        val roboto = face("openrndr-jvm/openrndr-gl3/src/jvmTest/resources/fonts/Roboto-Medium.ttf") ?: return@runTest
        assertVerticalMetrics(mongolian, mongolianText, Script.MONGOLIAN)
        assertVerticalMetrics(roboto, "Ab", Script.LATIN)
        val a = roboto.glyphForCharacter('A')
        near(75.0, a.verticalAdvance, "vertical advance of A")
        near(21.296875, a.verticalOrigin.x, "vertical origin x of A")
        near(-60.25, a.verticalOrigin.y, "vertical origin y of A")
    }
}

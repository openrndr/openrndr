import org.openrndr.draw.font.MathConstant
import org.openrndr.draw.font.MathDirection
import org.openrndr.draw.font.MathKernCorner
import org.openrndr.draw.font.internal.CachingFontDriver
import org.openrndr.draw.font.internal.CachingTextShapingDriver
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import java.io.File
import kotlin.math.abs
import kotlin.test.*

/**
 * The MATH table of STIX Two Math, a font that comes with macOS. The tests are skipped when the font is not found,
 * -Dorg.openrndr.test.mathFont points to another copy of it.
 */
class TestMathTableHarfBuzz {
    private val fontFile = File(
        System.getProperty("org.openrndr.test.mathFont") ?: "/System/Library/Fonts/Supplemental/STIXTwoMath.otf"
    )
    private val driver = TextShapingDriverHarfBuzz()

    private fun face(size: Double = 32.0) = FontDriverFreetype().loadFace(fontFile.path).withSize(size)

    /** the glyph of [text] as HarfBuzz shapes it */
    private fun glyph(face: org.openrndr.draw.font.Face, text: String) =
        driver.shape(face, text, direction = Direction.LEFT_TO_RIGHT).single().glyphIndex

    @Test
    fun `the caching driver of applications gives the MATH table of the driver it wraps`() {
        if (!fontFile.exists()) return
        // applications install a CachingTextShapingDriver and a CachingFontDriver
        val caching = CachingTextShapingDriver(driver)
        val face = CachingFontDriver(FontDriverFreetype()).loadFace(fontFile.path).withSize(32.0)
        val math = caching.mathTable(face)
        assertNotNull(math)
        assertTrue(math.constant(MathConstant.AXIS_HEIGHT) > 0.0)
        assertEquals(driver.querySupportedScripts(face), caching.querySupportedScripts(face))
    }

    @Test
    fun `the caching driver forwards every function of TextShapingDriver`() {
        // a function the caching driver does not override would give the interface's default, not the wrapped driver's
        val declared = CachingTextShapingDriver::class.java.declaredMethods.map { it.name to it.parameterTypes.toList() }.toSet()
        val missing = TextShapingDriver::class.java.methods
            .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .filter { (it.name to it.parameterTypes.toList()) !in declared }
        assertTrue(missing.isEmpty(), "not forwarded: ${missing.map { it.name }}")
    }

    @Test
    fun `faces without a MATH table have none`() {
        val face = FontDriverFreetype().loadFace("../../data/fonts/Platypi-Regular.ttf")
        assertNull(driver.mathTable(face))
    }

    @Test
    fun `constants are lengths at the size of the face, percentages as stored`() {
        if (!fontFile.exists()) return
        val small = driver.mathTable(face(32.0))!!
        val large = driver.mathTable(face(64.0))!!
        val axis = small.constant(MathConstant.AXIS_HEIGHT)
        assertTrue(axis > 0.0, "axis height $axis")
        // lengths scale with the size of the face, like shaping results
        assertTrue(abs(large.constant(MathConstant.AXIS_HEIGHT) - 2 * axis) < 0.1)
        assertTrue(small.constant(MathConstant.FRACTION_RULE_THICKNESS) > 0.0)
        val scaleDown = small.constant(MathConstant.SCRIPT_PERCENT_SCALE_DOWN)
        assertEquals(scaleDown, large.constant(MathConstant.SCRIPT_PERCENT_SCALE_DOWN))
        assertTrue(scaleDown in 50.0..90.0, "script percent scale down $scaleDown")
    }

    @Test
    fun `the variants of a parenthesis grow and it has an assembly with an extender`() {
        if (!fontFile.exists()) return
        val face = face()
        val math = driver.mathTable(face)!!
        val paren = glyph(face, "(")
        val variants = math.variants(paren, MathDirection.VERTICAL)
        assertTrue(variants.size > 2, "variants $variants")
        assertEquals(paren, variants.first().glyphIndex)
        assertTrue(variants.zipWithNext().all { (a, b) -> b.advance > a.advance }, "variants $variants")
        assertTrue(math.variants(paren, MathDirection.HORIZONTAL).isEmpty())

        val assembly = math.assembly(paren, MathDirection.VERTICAL)
        assertNotNull(assembly)
        assertTrue(assembly.parts.size >= 2)
        assertTrue(assembly.parts.any { it.isExtender })
        assertTrue(assembly.parts.all { it.fullAdvance > 0.0 })
        assertNull(math.assembly(glyph(face, "x"), MathDirection.VERTICAL))
        assertTrue(math.minConnectorOverlap(MathDirection.VERTICAL) >= 0.0)
    }

    @Test
    fun `italic glyphs have an italic correction`() {
        if (!fontFile.exists()) return
        val face = face()
        val math = driver.mathTable(face)!!
        // U+1D453 MATHEMATICAL ITALIC SMALL F
        assertTrue(math.italicCorrection(glyph(face, "𝑓")) > 0.0)
        assertEquals(0.0, math.italicCorrection(glyph(face, "+")))
    }

    @Test
    fun `accents, extended shapes and kerning`() {
        if (!fontFile.exists()) return
        val face = face()
        val math = driver.mathTable(face)!!
        val parenVariants = math.variants(glyph(face, "("), MathDirection.VERTICAL)
        assertTrue(math.isExtendedShape(parenVariants.last().glyphIndex))
        assertFalse(math.isExtendedShape(glyph(face, "x")))
        // italic capitals have top accent attachments, U+1D434 MATHEMATICAL ITALIC CAPITAL A
        val a = glyph(face, "𝐴")
        val attachment = math.topAccentAttachment(a)
        assertNotNull(attachment)
        assertTrue(attachment > 0.0)
        // kerning gives finite values at any height, also for glyphs without math kerning
        for (corner in MathKernCorner.entries) {
            assertTrue(math.kern(a, corner, 10.0).isFinite())
        }
    }
}

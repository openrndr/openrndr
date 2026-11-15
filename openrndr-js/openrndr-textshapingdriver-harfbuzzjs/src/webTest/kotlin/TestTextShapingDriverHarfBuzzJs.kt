@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlinx.coroutines.test.runTest
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Script
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.fontdriver.harfbuzzjs.FontDriverHarfBuzzJs
import org.openrndr.textshapingdriver.harfbuzzjs.TextShapingDriverHarfBuzzJs
import kotlin.test.Test
import kotlin.test.assertEquals

private fun repositoryRootVariable(): JsString? =
    js("(globalThis.process && globalThis.process.env && globalThis.process.env.OPENRNDR_ROOT) || null")

/**
 * Compares the harfbuzzjs text shaping driver with values of the HarfBuzz text shaping driver on the JVM
 */
class TestTextShapingDriverHarfBuzzJs {
    private suspend fun roboto(): Face? {
        val root = repositoryRootVariable()?.toString() ?: return null
        return FontDriverHarfBuzzJs.create()
            .loadFace("$root/openrndr-jvm/openrndr-gl3/src/jvmTest/resources/fonts/Roboto-Medium.ttf").withSize(32.0)
    }

    @Test
    fun shaping() = runTest {
        val face = roboto() ?: return@runTest
        val driver = TextShapingDriverHarfBuzzJs()
        // the 'ffi' ligature is glyph 420, the advance of 'A' is kerned against 'V'
        val shaped = driver.shape(face, "office AV")
        assertEquals(listOf(83, 74, 420, 71, 73, 4, 37, 58), shaped.map { it.glyphIndex })
        val advances = listOf(18.21875, 11.34375, 18.625, 16.75, 17.171875, 7.96875, 20.09375, 20.703125)
        for ((expected, result) in advances.zip(shaped)) {
            assertEquals(expected, result.advance.x, 1E-9)
            assertEquals(0.0, result.offset.x, 1E-9)
        }

        val noLigatures = driver.shape(face, "office", listOf(ShapeFeature("liga", 0)))
        assertEquals(listOf(83, 74, 74, 77, 71, 73), noLigatures.map { it.glyphIndex })
    }

    @Test
    fun queries() = runTest {
        val face = roboto() ?: return@runTest
        val driver = TextShapingDriverHarfBuzzJs()
        assertEquals(listOf(Script.CYRILLIC, Script.GREEK, Script.LATIN), driver.querySupportedScripts(face))
        assertEquals(
            listOf("c2sc", "ccmp", "dlig", "dnom", "frac", "liga", "lnum", "numr", "onum", "pnum", "smcp", "ss01", "ss02", "ss03", "ss04", "ss05", "ss06", "ss07", "tnum"),
            driver.querySubstitutionFeatures(face, Script.LATIN)
        )
        assertEquals(listOf("cpsp", "kern"), driver.queryPositionFeatures(face, Script.LATIN))
        assertEquals(emptyList(), driver.querySubstitutionFeatures(face, Script.ARABIC))
    }
}

private fun fileExists(path: String): Boolean =
    js("(() => { const p = globalThis.process; const fs = p && p.getBuiltinModule ? p.getBuiltinModule('fs') : undefined; return !!(fs && fs.existsSync(path)); })()")

/**
 * The MATH table on the web, read from the bytes of the table. The values are compared with HarfBuzz on the JVM by
 * TestOpenTypeMathTable, here the driver is checked to give it in pixels at the size of the face.
 */
class TestMathTableHarfBuzzJs {
    @Test
    fun mathTables() = runTest {
        val root = repositoryRootVariable()?.toString() ?: return@runTest
        val fonts = FontDriverHarfBuzzJs.create()
        val driver = TextShapingDriverHarfBuzzJs()
        val roboto = fonts.loadFace("$root/openrndr-jvm/openrndr-gl3/src/jvmTest/resources/fonts/Roboto-Medium.ttf")
        assertEquals(null, driver.mathTable(roboto))

        val stix = "/System/Library/Fonts/Supplemental/STIXTwoMath.otf"
        if (!fileExists(stix)) return@runTest
        val face = fonts.loadFace(stix).withSize(32.0)
        val math = driver.mathTable(face) ?: error("no MATH table")
        // 258 of 1000 units per em
        assertEquals(32.0 * 258 / 1000, math.constant(org.openrndr.draw.font.MathConstant.AXIS_HEIGHT), 1E-9)
        val paren = driver.shape(face, "(").single().glyphIndex
        val variants = math.variants(paren, org.openrndr.draw.font.MathDirection.VERTICAL)
        assertEquals(paren, variants.first().glyphIndex)
        assertEquals(true, variants.zipWithNext().all { (a, b) -> b.advance > a.advance })
        assertEquals(true, math.assembly(paren, org.openrndr.draw.font.MathDirection.VERTICAL)!!.parts.any { it.isExtender })
    }
}

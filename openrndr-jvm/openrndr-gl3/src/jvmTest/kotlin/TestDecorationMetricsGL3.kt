import org.openrndr.draw.font.DecorationMetrics
import org.openrndr.draw.font.loadFace
import org.openrndr.resourceUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Faces report the positions and thicknesses of underlines and strikeouts of their fonts
 */
class TestDecorationMetricsGL3 : AbstractApplicationTestFixture() {
    @Test
    fun facesReportTheDecorationMetricsOfTheirFonts() {
        val face = loadFace(resourceUrl("/fonts/Roboto-Medium.ttf")).withSize(32.0)
        assertEquals("FaceFreetype", face::class.simpleName)
        // post: underlinePosition -150, underlineThickness 100; OS/2: yStrikeoutPosition 512, yStrikeoutSize 102, of
        // 2048 units per em. The positions are those of the centers of the lines
        val metrics = face.decorationMetrics
        assertEquals(-200.0 / 2048.0 * 32.0, metrics.underlinePosition, 1E-9)
        assertEquals(100.0 / 2048.0 * 32.0, metrics.underlineThickness, 1E-9)
        assertEquals(461.0 / 2048.0 * 32.0, metrics.strikeoutPosition, 1E-9)
        assertEquals(102.0 / 2048.0 * 32.0, metrics.strikeoutThickness, 1E-9)
        assertTrue(metrics.underlineFromFont && metrics.strikeoutFromFont)
    }

    @Test
    fun linesWithoutMetricsFallBack() {
        val face = loadFace(resourceUrl("/fonts/Roboto-Medium.ttf")).withSize(32.0)
        val metrics = DecorationMetrics.of(face, underlinePosition = -3.0, underlineThickness = 0.0)
        assertFalse(metrics.underlineFromFont)
        assertFalse(metrics.strikeoutFromFont)
        assertEquals(-0.075 * 32.0, metrics.underlinePosition, 1E-9)
        assertEquals(0.05 * 32.0, metrics.underlineThickness, 1E-9)
        assertEquals(face.xHeight / 2.0, metrics.strikeoutPosition, 1E-9)
    }
}

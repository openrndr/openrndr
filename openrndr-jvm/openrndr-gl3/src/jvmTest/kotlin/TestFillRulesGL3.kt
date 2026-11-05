import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Shapes and contours are filled following the fill rule of the drawer, like the `fill-rule` of SVG
 */
class TestFillRulesGL3 : AbstractApplicationTestFixture() {
    private val width = 200
    private val height = 100
    private lateinit var target: RenderTarget

    @BeforeTest
    fun createTarget() {
        target = renderTarget(width, height, 1.0) { colorBuffer(type = ColorType.FLOAT32); depthBuffer() }
    }

    @AfterTest
    fun destroyTarget() {
        target.destroy()
    }

    /** fills [shape] with [fillRule], returns the alpha at pixels */
    private fun fill(shape: Shape, fillRule: FillRule? = null): (Int, Int) -> Float {
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            fill = ColorRGBa.WHITE
            stroke = null
            if (fillRule != null) this.fillRule = fillRule
            shape(shape)
        }
        val data = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data)
        return { x, y -> data.getFloat(((height - 1 - y) * width + x) * 16 + 12) }
    }

    private val outer = Rectangle(10.0, 10.0, 60.0, 60.0).contour
    private val overlapping = Shape(listOf(outer, Rectangle(40.0, 30.0, 60.0, 60.0).contour))
    private val hole = Rectangle(30.5, 30.5, 20.0, 20.0).contour
    private val pentagram = Shape(listOf(ShapeContour.fromPoints((0 until 5).map { i ->
        val a = Math.PI * 2.0 * i * 2 / 5 - Math.PI / 2
        Vector2(150.0, 50.0) + Vector2(cos(a), sin(a)) * 45.0
    }, closed = true)))

    @Test
    fun theDefaultIsNonZero() {
        assertEquals(FillRule.NON_ZERO, program.drawer.fillRule)
        assertEquals(1.0f, fill(overlapping)(55, 50), 1E-3f)
    }

    @Test
    fun nonZeroFillsWhereContoursWindAround() {
        // overlapping contours that wind the same way, a contour inside one that winds the same way
        assertEquals(1.0f, fill(overlapping, FillRule.NON_ZERO)(55, 50), 1E-3f)
        assertEquals(1.0f, fill(Shape(listOf(outer, hole)), FillRule.NON_ZERO)(40, 40), 1E-3f)
        // a hole that winds the other way
        assertEquals(0.0f, fill(Shape(listOf(outer, hole.reversed)), FillRule.NON_ZERO)(40, 40), 1E-3f)
        // the center of a pentagram is surrounded twice
        assertEquals(1.0f, fill(pentagram, FillRule.NON_ZERO)(150, 50), 1E-3f)
    }

    @Test
    fun evenOddFillsWhereContoursSurroundAnOddNumberOfTimes() {
        assertEquals(0.0f, fill(overlapping, FillRule.EVEN_ODD)(55, 50), 1E-3f)
        assertEquals(1.0f, fill(overlapping, FillRule.EVEN_ODD)(20, 20), 1E-3f)
        assertEquals(0.0f, fill(Shape(listOf(outer, hole)), FillRule.EVEN_ODD)(40, 40), 1E-3f)
        assertEquals(0.0f, fill(Shape(listOf(outer, hole.reversed)), FillRule.EVEN_ODD)(40, 40), 1E-3f)
        assertEquals(0.0f, fill(pentagram, FillRule.EVEN_ODD)(150, 50), 1E-3f)
        assertEquals(1.0f, fill(pentagram, FillRule.EVEN_ODD)(150, 10), 1E-3f)
        val three = Shape((0 until 3).map { Rectangle(110.0 + it * 5.0, 10.0, 60.0, 60.0).contour })
        assertEquals(1.0f, fill(three, FillRule.EVEN_ODD)(150, 40), 1E-3f)
    }

    /** asserts that the pixels at [points] are half covered: the outline crosses their centers */
    private fun assertHalfCovered(alpha: (Int, Int) -> Float, points: List<Pair<Int, Int>>, name: String) {
        for ((x, y) in points) {
            assertTrue(alpha(x, y) in 0.35f..0.65f, "$name pixel ($x, $y): ${alpha(x, y)}")
        }
    }

    @Test
    fun theEdgesOfHolesAreAntiAliased() {
        // the edges of the hole lie in the middle of pixels 30 and 50
        val edges = listOf(30 to 40, 50 to 40, 40 to 30, 40 to 50)
        for (fillRule in FillRule.entries) {
            assertHalfCovered(fill(Shape(listOf(outer, hole.reversed)), fillRule), edges, "$fillRule, opposite winding")
        }
        // a hole that winds the same way as the outline is a hole by the even-odd rule only
        assertHalfCovered(fill(Shape(listOf(outer, hole)), FillRule.EVEN_ODD), edges, "EVEN_ODD, same winding")
    }

    @Test
    fun theEdgesOfOverlapsAreAntiAliased() {
        // the second rectangle starts in the middle of pixels 40 and ends in the middle of pixels 100
        val overlapping = Shape(listOf(outer, Rectangle(40.5, 30.5, 60.0, 60.0).contour))
        for (fillRule in FillRule.entries) {
            val alpha = fill(overlapping, fillRule)
            // the outlines outside of the overlap
            assertHalfCovered(alpha, listOf(100 to 60, 80 to 90, 85 to 30), "$fillRule outline")
            // the outlines within the overlap are not edges of the non-zero fill, there is no seam
            val within = listOf(40 to 50, 60 to 30)
            if (fillRule == FillRule.NON_ZERO) {
                for ((x, y) in within) {
                    assertEquals(1.0f, alpha(x, y), 1E-3f, "NON_ZERO pixel ($x, $y)")
                }
            } else {
                assertHalfCovered(alpha, within, "EVEN_ODD overlap")
            }
        }
    }

    @Test
    fun edgesAreCoveredByTheirDistanceToTheOutline() {
        // outlines a quarter of a pixel from the centers of pixels: the pixels on the inside are covered for three
        // quarters, those on the outside for a quarter, on both sides of holes that wind either way
        val outer = Rectangle(10.25, 10.25, 60.0, 60.0).contour
        val hole = Rectangle(30.25, 30.25, 20.0, 20.0).contour
        val outline = listOf(Triple(10, 40, 0.75f), Triple(9, 40, 0.0f), Triple(70, 40, 0.25f), Triple(40, 10, 0.75f), Triple(40, 70, 0.25f))
        val holeEdges = listOf(Triple(30, 40, 0.25f), Triple(29, 40, 1.0f), Triple(50, 40, 0.75f), Triple(49, 40, 0.0f))
        val filledHole = listOf(Triple(30, 40, 1.0f), Triple(50, 40, 1.0f), Triple(49, 40, 1.0f))

        fun assertCoverage(shape: Shape, fillRule: FillRule, expected: List<Triple<Int, Int, Float>>, name: String) {
            val alpha = fill(shape, fillRule)
            for ((x, y, coverage) in expected) {
                assertEquals(coverage, alpha(x, y), 0.02f, "$name $fillRule pixel ($x, $y)")
            }
        }
        for (fillRule in FillRule.entries) {
            assertCoverage(Shape(listOf(outer, hole.reversed)), fillRule, outline + holeEdges, "opposite winding hole")
        }
        assertCoverage(Shape(listOf(outer, hole)), FillRule.EVEN_ODD, outline + holeEdges, "same winding hole")
        // the inner contour does not make a hole by the non-zero rule, its outline is not anti-aliased
        assertCoverage(Shape(listOf(outer, hole)), FillRule.NON_ZERO, outline + filledHole, "same winding hole")

        // overlapping contours: the outlines within the overlap are edges of the even-odd fill only
        val overlapping = Shape(listOf(outer, Rectangle(40.25, 30.25, 60.0, 60.0).contour))
        val outside = listOf(Triple(100, 60, 0.25f), Triple(85, 30, 0.75f), Triple(85, 29, 0.0f))
        assertCoverage(overlapping, FillRule.NON_ZERO, outside + listOf(Triple(40, 50, 1.0f), Triple(60, 30, 1.0f)), "overlap")
        assertCoverage(overlapping, FillRule.EVEN_ODD, outside + listOf(Triple(40, 50, 0.25f), Triple(60, 30, 0.25f), Triple(39, 50, 1.0f)), "overlap")
    }
}

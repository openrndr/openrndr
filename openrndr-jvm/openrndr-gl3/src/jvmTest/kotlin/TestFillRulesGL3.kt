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

    @Test
    fun theEdgesOfHolesAreAntiAliased() {
        // the edges of the hole lie in the middle of pixels 30 and 50, the hole winds the other way than the outline
        for (fillRule in FillRule.entries) {
            val alpha = fill(Shape(listOf(outer, hole.reversed)), fillRule)
            for ((x, y) in listOf(30 to 40, 50 to 40, 40 to 30, 40 to 50)) {
                assertTrue(alpha(x, y) in 0.3f..0.7f, "$fillRule pixel ($x, $y): ${alpha(x, y)}")
            }
        }
    }
}

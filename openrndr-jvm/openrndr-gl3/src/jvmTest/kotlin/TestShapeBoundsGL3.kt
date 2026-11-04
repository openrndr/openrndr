import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Shapes, contours and line strips have the bounds of their geometry as `c_boundsPosition`, over all contours, strokes
 * have the bounds of the geometry they stroke
 */
class TestShapeBoundsGL3 : AbstractApplicationTestFixture() {
    private val width = 400
    private val height = 200
    private lateinit var target: RenderTarget

    @BeforeTest
    fun createTarget() {
        target = renderTarget(width, height, 1.0) { colorBuffer(type = ColorType.FLOAT32); depthBuffer() }
    }

    @AfterTest
    fun destroyTarget() {
        target.destroy()
    }

    private val boundsStyle = shadeStyle {
        fragmentTransform = "x_fill = vec4(c_boundsPosition.xy, 0.0, 1.0); x_stroke = x_fill;"
    }

    /** draws with the bounds style, returns the bounds position at pixels */
    private fun draw(block: Drawer.() -> Unit): (Int, Int) -> Vector2 {
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            fill = ColorRGBa.WHITE
            stroke = null
            shadeStyle = boundsStyle
            block()
        }
        val data = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data)
        return { x, y ->
            val i = ((height - 1 - y) * width + x) * 16
            Vector2(data.getFloat(i).toDouble(), data.getFloat(i + 4).toDouble())
        }
    }

    /** asserts the bounds position at the center of pixel ([x], [y]) in [bounds] */
    private fun assertBounds(pixels: (Int, Int) -> Vector2, x: Int, y: Int, bounds: Rectangle) {
        val expected = Vector2((x + 0.5 - bounds.x) / bounds.width, (y + 0.5 - bounds.y) / bounds.height)
        val actual = pixels(x, y)
        assertTrue(abs(expected.x - actual.x) < 1E-3 && abs(expected.y - actual.y) < 1E-3,
            "pixel ($x, $y): expected $expected, was $actual")
    }

    private val square = Rectangle(10.0, 10.0, 180.0, 180.0)

    @Test
    fun convexShapes() {
        val pixels = draw { shape(square.shape) }
        assertBounds(pixels, 40, 100, square)
        assertBounds(pixels, 100, 100, square)
        assertBounds(pixels, 180, 180, square)
    }

    @Test
    fun shapesWithSeveralContours() {
        val shape = Shape(listOf(square.contour, Rectangle(210.0, 10.0, 180.0, 180.0).contour))
        val bounds = Rectangle(10.0, 10.0, 380.0, 180.0)
        val pixels = draw { shape(shape) }
        assertBounds(pixels, 40, 100, bounds)
        assertBounds(pixels, 300, 100, bounds)
        assertBounds(pixels, 380, 20, bounds)
    }

    @Test
    fun boundsAreNotLeftFromOtherDraws() {
        val pixels = draw {
            shape(Shape(listOf(Rectangle(0.0, 0.0, 50.0, 50.0).contour, Rectangle(300.0, 100.0, 50.0, 50.0).contour)))
            shape(square.shape)
            contour(Rectangle(200.0, 20.0, 100.0, 50.0).contour)
        }
        assertBounds(pixels, 100, 100, square)
        assertBounds(pixels, 250, 40, Rectangle(200.0, 20.0, 100.0, 50.0))
    }

    @Test
    fun strokesHaveTheBoundsOfTheirGeometry() {
        val pixels = draw {
            fill = null
            stroke = ColorRGBa.WHITE
            strokeWeight = 20.0
            shape(square.shape)
        }
        // on the outline and outside of it, the stroke weight is not part of the bounds
        assertBounds(pixels, 10, 100, square)
        assertBounds(pixels, 2, 100, square)
        assertBounds(pixels, 195, 100, square)
        assertBounds(pixels, 100, 15, square)
    }

    @Test
    fun lineStripsHaveTheBoundsOfTheirPoints() {
        val pixels = draw {
            stroke = ColorRGBa.WHITE
            strokeWeight = 10.0
            lineStrip(listOf(Vector2(50.0, 50.0), Vector2(350.0, 50.0), Vector2(350.0, 150.0)))
        }
        val bounds = Rectangle(50.0, 50.0, 300.0, 100.0)
        assertBounds(pixels, 200, 50, bounds)
        assertBounds(pixels, 350, 100, bounds)
    }
}

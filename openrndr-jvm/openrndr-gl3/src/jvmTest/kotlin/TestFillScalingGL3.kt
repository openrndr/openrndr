import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The anti-aliased edges of fills are a device pixel wide however the model matrix scales, rotates or skews them
 */
class TestFillScalingGL3 : AbstractApplicationTestFixture() {
    private val size = 100
    private lateinit var target: RenderTarget

    @BeforeTest
    fun createTarget() {
        target = renderTarget(size, size, 1.0) { colorBuffer(type = ColorType.FLOAT32); depthBuffer() }
    }

    @AfterTest
    fun destroyTarget() {
        target.destroy()
    }

    /** fills [shape] in device pixel coordinates, drawn with the model [transform] and the shape mapped back */
    private fun fill(transform: Drawer.() -> Unit, shape: (Rectangle) -> Shape, rectangle: Rectangle, inverse: (Rectangle) -> Rectangle): (Int, Int) -> Float {
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            fill = ColorRGBa.WHITE
            stroke = null
            transform()
            shape(shape(inverse(rectangle)))
        }
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data)
        return { x, y -> data.getFloat(((size - 1 - y) * size + x) * 16 + 12) }
    }

    private val scales = listOf(1.0 to 1.0, 200.0 to 200.0, 200.0 to 120.0, 120.0 to 200.0, 4.0 to 1.0, 0.25 to 3.0)

    /** a rectangle and, for the stenciled fill, a rectangle with a hole that winds the other way */
    private val shapes = listOf<Pair<String, (Rectangle) -> Shape>>(
        "convex" to { r -> r.shape },
        "stenciled" to { r ->
            Shape(listOf(r.contour, Rectangle(r.x + r.width * 0.25, r.y + r.height * 0.25, r.width * 0.5, r.height * 0.5).contour.reversed))
        }
    )

    private fun assertCoverage(alpha: (Int, Int) -> Float, expected: List<Triple<Int, Int, Float>>, name: String) {
        for ((x, y, coverage) in expected) {
            assertEquals(coverage, alpha(x, y), 0.02f, "$name pixel ($x, $y)")
        }
    }

    @Test
    fun pixelAlignedEdgesAreSharpWhenScaled() {
        for ((sx, sy) in scales) for ((name, shape) in shapes) {
            val alpha = fill({ scale(sx, sy) }, shape, Rectangle(10.0, 10.0, 80.0, 80.0)) {
                Rectangle(it.x / sx, it.y / sy, it.width / sx, it.height / sy)
            }
            val expected = listOf(Triple(10, 15, 1.0f), Triple(89, 15, 1.0f), Triple(15, 10, 1.0f), Triple(15, 89, 1.0f),
                Triple(9, 15, 0.0f), Triple(90, 15, 0.0f), Triple(15, 9, 0.0f), Triple(15, 90, 0.0f))
            assertCoverage(alpha, expected, "$name scale($sx, $sy)")
        }
    }

    @Test
    fun edgesAreCoveredByTheirDistanceInDevicePixelsWhenScaled() {
        // edges a quarter of a pixel from the centers of the pixels, the hole spans pixels 30.25 to 70.25
        for ((sx, sy) in scales) for ((name, shape) in shapes) {
            val alpha = fill({ scale(sx, sy) }, shape, Rectangle(10.25, 10.25, 80.0, 80.0)) {
                Rectangle(it.x / sx, it.y / sy, it.width / sx, it.height / sy)
            }
            val expected = mutableListOf(Triple(10, 15, 0.75f), Triple(90, 15, 0.25f), Triple(15, 10, 0.75f), Triple(15, 90, 0.25f))
            if (name == "stenciled") {
                expected += listOf(Triple(30, 50, 0.25f), Triple(70, 50, 0.75f), Triple(50, 30, 0.25f), Triple(50, 70, 0.75f))
            }
            assertCoverage(alpha, expected, "$name scale($sx, $sy)")
        }
    }

    @Test
    fun edgesAreCoveredByTheirDistanceInDevicePixelsWhenRotatedAndScaled() {
        // rotated by a quarter turn around the center and scaled non-uniformly, the edges are aligned with the pixels
        for ((name, shape) in shapes) {
            val alpha = fill({ translate(50.0, 50.0); rotate(90.0); scale(5.0, 0.5) }, shape, Rectangle(10.25, 10.25, 80.0, 80.0)) {
                // device x = -0.5 * y', device y = 5 * x' relative to the center
                Rectangle((it.y - 50.0) / 5.0, -(it.x + it.width - 50.0) / 0.5, it.height / 5.0, it.width / 0.5)
            }
            assertCoverage(alpha, listOf(Triple(10, 15, 0.75f), Triple(90, 15, 0.25f), Triple(15, 10, 0.75f), Triple(15, 90, 0.25f)), "$name rotated")
        }
    }
}

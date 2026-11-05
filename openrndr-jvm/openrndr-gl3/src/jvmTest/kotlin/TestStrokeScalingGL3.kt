import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.StrokeAlignment
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Strokes are scaled with their geometry, their anti-aliased edges are a device pixel wide however they are scaled
 */
class TestStrokeScalingGL3 : AbstractApplicationTestFixture() {
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

    private fun stroke(draw: Drawer.() -> Unit): (Int, Int) -> Float {
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            fill = null
            stroke = ColorRGBa.WHITE
            draw()
        }
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data)
        return { x, y -> data.getFloat(((size - 1 - y) * size + x) * 16 + 12) }
    }

    private fun assertCoverage(alpha: (Int, Int) -> Float, expected: List<Triple<Int, Int, Float>>, name: String) {
        for ((x, y, coverage) in expected) {
            assertEquals(coverage, alpha(x, y), 0.02f, "$name pixel ($x, $y)")
        }
    }

    @Test
    fun nonUniformlyScaledStrokesAreCoveredByTheirDistanceInDevicePixels() {
        // under scale(4, 1) a stroke weight of 1 is 4 pixels wide across vertical lines, 1 pixel across horizontal lines
        val vertical = stroke {
            scale(4.0, 1.0); strokeWeight = 1.0
            lineSegment(Vector2(32.25 / 4.0, 10.0), Vector2(32.25 / 4.0, 90.0))
        }
        // the stroke spans pixels 30.25 to 34.25
        assertCoverage(vertical, listOf(Triple(29, 50, 0.0f), Triple(30, 50, 0.75f), Triple(32, 50, 1.0f), Triple(34, 50, 0.25f), Triple(35, 50, 0.0f)), "vertical")

        val horizontal = stroke {
            scale(4.0, 1.0); strokeWeight = 1.0
            lineSegment(Vector2(10.0 / 4.0, 60.25), Vector2(90.0 / 4.0, 60.25))
        }
        // the stroke spans pixels 59.75 to 60.75
        assertCoverage(horizontal, listOf(Triple(50, 58, 0.0f), Triple(50, 59, 0.25f), Triple(50, 60, 0.75f), Triple(50, 61, 0.0f)), "horizontal")
    }

    @Test
    fun uniformlyScaledStrokesAreCoveredByTheirDistanceInDevicePixels() {
        val alpha = stroke {
            scale(200.0, 200.0); strokeWeight = 0.01
            lineSegment(Vector2(50.25 / 200.0, 0.05), Vector2(50.25 / 200.0, 0.45))
        }
        // the stroke spans pixels 49.25 to 51.25
        assertCoverage(alpha, listOf(Triple(48, 50, 0.0f), Triple(49, 50, 0.75f), Triple(50, 50, 1.0f), Triple(51, 50, 0.25f), Triple(52, 50, 0.0f)), "scale(200, 200)")
    }

    @Test
    fun alignedStrokesAreClippedAPixelWideWhenScaled() {
        // a rectangle from pixel 20.25 to 80.25, stroked inside, 4 pixels wide across vertical edges, 1 across horizontal
        val alpha = stroke {
            scale(4.0, 1.0); strokeWeight = 1.0; strokeAlignment = StrokeAlignment.INSIDE
            contour(Rectangle(20.25 / 4.0, 20.25, 60.0 / 4.0, 60.0).contour)
        }
        assertCoverage(alpha, listOf(Triple(19, 50, 0.0f), Triple(20, 50, 0.75f), Triple(22, 50, 1.0f), Triple(24, 50, 0.25f), Triple(25, 50, 0.0f)), "left edge")
        assertCoverage(alpha, listOf(Triple(50, 19, 0.0f), Triple(50, 20, 0.75f), Triple(50, 21, 0.25f), Triple(50, 22, 0.0f)), "top edge")
    }

    @Test
    fun strokesThinnerThanAPixelCoverTheirWidth() {
        val alpha = stroke {
            strokeWeight = 0.5
            lineSegment(Vector2(40.5, 10.0), Vector2(40.5, 90.0))
        }
        assertCoverage(alpha, listOf(Triple(39, 50, 0.0f), Triple(40, 50, 0.5f), Triple(41, 50, 0.0f)), "half a pixel wide")
    }
}

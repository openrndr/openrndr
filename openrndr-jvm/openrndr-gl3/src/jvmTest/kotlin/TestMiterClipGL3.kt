import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineJoin
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Miters longer than the miter limit are clipped instead of beveled, like the miter-clip join of SVG 2
 */
class TestMiterClipGL3 : AbstractApplicationTestFixture() {
    private val size = 100

    private fun stroke(join: LineJoin, miterLimit: Double, weight: Double, draw: Drawer.() -> Unit): FloatArray {
        val rt = renderTarget(size, size, 1.0) { colorBuffer(type = ColorType.FLOAT32); depthBuffer(DepthFormat.DEPTH24_STENCIL8) }
        program.drawer.isolatedWithTarget(rt) {
            ortho(rt)
            clear(ColorRGBa.TRANSPARENT)
            fill = null
            stroke = ColorRGBa.WHITE
            strokeWeight = weight
            lineJoin = join
            this.miterLimit = miterLimit
            draw()
        }
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        rt.colorBuffer(0).read(data)
        rt.colorBuffer(0).destroy(); rt.depthBuffer?.destroy(); rt.destroy()
        return FloatArray(size * size) { data.getFloat(((size - 1 - it / size) * size + it % size) * 16 + 12) }
    }

    private fun FloatArray.at(x: Int, y: Int) = this[y * this@TestMiterClipGL3.size + x]

    private fun assertNear(expected: Double, actual: Double, tolerance: Double, message: String) =
        assertTrue(abs(expected - actual) <= tolerance, "$message: expected $expected, was $actual")

    // an apex at (50, 20.25) with a half angle of atan(1/2): its miter is sqrt(5) times half the stroke weight long
    private val apex = ShapeContour.fromPoints(listOf(Vector2(20.0, 80.0), Vector2(50.0, 20.25), Vector2(80.0, 80.0)), closed = false)

    @Test
    fun longMitersAreClippedAtTheMiterLimit() {
        // half of the stroke weight is 5, the miter is clipped at 1.5 * 5 = 7.5 above the apex, at y = 12.75
        val clipped = stroke(LineJoin.MITER_CLIP, 1.5, 10.0) { contour(apex) }
        assertNear(0.0, clipped.at(50, 11).toDouble(), 0.05, "above the clip line")
        assertNear(0.25, clipped.at(50, 12).toDouble(), 0.1, "on the clip line")
        assertNear(1.0, clipped.at(50, 14).toDouble(), 0.05, "below the clip line")
        // a miter is not clipped within the limit, a bevel cuts it off 5 * sin(atan(1/2)) = 2.24 above the apex
        val mitered = stroke(LineJoin.MITER, 3.0, 10.0) { contour(apex) }
        assertNear(1.0, mitered.at(50, 12).toDouble(), 0.05, "a miter within the limit")
        val beveled = stroke(LineJoin.MITER, 1.5, 10.0) { contour(apex) }
        assertNear(0.0, beveled.at(50, 14).toDouble(), 0.05, "a miter beyond the limit is beveled")
        // within the limit miter-clip is a miter
        val within = stroke(LineJoin.MITER_CLIP, 3.0, 10.0) { contour(apex) }
        for (i in mitered.indices) assertNear(mitered[i].toDouble(), within[i].toDouble(), 1E-4, "pixel $i within the limit")
    }

    @Test
    fun rectangleCornersAreClippedLikeContours() {
        // right angled corners have a miter ratio of sqrt(2), with a miter limit of 1.2 each corner loses a triangle
        // of (sqrt(2) - 1.2)^2 times the square of half the stroke weight
        val weight = 10.0
        val r = Rectangle(20.0, 20.0, 60.0, 60.0)
        val o = weight / 2.0
        val band = (r.width + 2 * o) * (r.height + 2 * o) - (r.width - 2 * o) * (r.height - 2 * o)
        val expected = band - 4 * (sqrt(2.0) * o - 1.2 * o) * (sqrt(2.0) * o - 1.2 * o)
        val rectangle = stroke(LineJoin.MITER_CLIP, 1.2, weight) { rectangle(r) }.sum().toDouble()
        val contour = stroke(LineJoin.MITER_CLIP, 1.2, weight) { contour(r.contour) }.sum().toDouble()
        assertNear(expected, rectangle, 1.0, "rectangle")
        assertNear(expected, contour, expected * 2E-3, "contour")
    }
}

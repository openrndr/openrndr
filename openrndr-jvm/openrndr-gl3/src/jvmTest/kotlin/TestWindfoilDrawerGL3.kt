import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.windfoil.WindfoilDrawer
import org.openrndr.draw.windfoil.WindfoilMap
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class TestWindfoilDrawerGL3 : AbstractApplicationTestFixture() {

    private fun map() = WindfoilMap(
        colorBuffer(4096, 4, type = ColorType.FLOAT32, format = ColorFormat.RG),
        colorBuffer(4096, 4, type = ColorType.UINT32_INT, format = ColorFormat.RGBa)
    )

    /** draws [commands] into a [width] x [height] FLOAT32 target and returns its RGBA pixels */
    private fun draw(map: WindfoilMap, commands: List<SlugCommand>, width: Int, height: Int): FloatArray {
        val drawer = WindfoilDrawer().apply { contrast = 0.0 }
        val target = renderTarget(width, height, contentScale = 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            drawer.draw(this, map, drawer.prepare(map, commands))
        }
        val buffer = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(buffer, ColorFormat.RGBa, ColorType.FLOAT32)
        val colorBuffer = target.colorBuffer(0)
        target.detachColorAttachments()
        target.destroy()
        colorBuffer.destroy()
        drawer.close()
        return FloatArray(width * height * 4) { buffer.getFloat(it * 4) }
    }

    @Test
    fun `partially covered pixels get their exact coverage`() {
        val map = map()
        // spans x 2.25..6.75 and y 1.5..3.5, so pixel columns 2 and 6 are 75% covered and rows 1 and 3 50%
        val index = map.addShape(Rectangle(2.25, 1.5, 4.5, 2.0).shape)
        val pixels = draw(map, listOf(SlugCommand(index, Matrix44.IDENTITY, ColorRGBa.WHITE, null, 0.0)), 9, 5)
        fun coverage(x: Int, y: Int) = pixels[(y * 9 + x) * 4 + 3]
        assertEquals(0.0f, coverage(1, 2), 1E-4f)
        assertEquals(0.75f, coverage(2, 2), 1E-4f)
        assertEquals(1.0f, coverage(4, 2), 1E-4f)
        assertEquals(0.75f, coverage(6, 2), 1E-4f)
        assertEquals(0.5f, coverage(4, 1), 1E-4f)
        assertEquals(0.375f, coverage(2, 1), 1E-4f)
        assertEquals(0.0f, coverage(7, 2), 1E-4f)
    }

    @Test
    fun `even-odd shapes leave overlaps empty`() {
        val map = map()
        val star = Shape(listOf(ShapeContour.fromPoints((0 until 5).map { i ->
            val a = Math.PI * 2.0 * i * 2 / 5 - Math.PI / 2
            Vector2(16.0, 16.0) + Vector2(cos(a), sin(a)) * 15.0
        }, closed = true)))
        val evenOdd = map.addShape(star, FillRule.EVEN_ODD)
        val nonZero = map.addShape(star, FillRule.NON_ZERO)
        val evenOddPixels = draw(map, listOf(SlugCommand(evenOdd, Matrix44.IDENTITY, ColorRGBa.WHITE, null, 0.0)), 32, 32)
        val nonZeroPixels = draw(map, listOf(SlugCommand(nonZero, Matrix44.IDENTITY, ColorRGBa.WHITE, null, 0.0)), 32, 32)
        val center = (16 * 32 + 16) * 4 + 3
        assertEquals(0.0f, evenOddPixels[center], 1E-4f)
        assertEquals(1.0f, nonZeroPixels[center], 1E-4f)
    }

    @Test
    fun `strokes are composited premultiplied`() {
        val map = map()
        val index = map.addShape(Shape(listOf(Circle(16.0, 16.0, 10.0).contour)))
        val pixels = draw(map, listOf(SlugCommand(index, Matrix44.IDENTITY, null, ColorRGBa.WHITE, 1.5)), 32, 32)
        // a white stroke stores its coverage in all channels
        for (i in 0 until 32 * 32) {
            assertEquals(pixels[i * 4 + 3], pixels[i * 4], 1E-4f)
        }
        // the ink of the stroke is close to its area
        val ink = (0 until 32 * 32).sumOf { pixels[it * 4 + 3].toDouble() }
        assertEquals(2.0 * Math.PI * 10.0 * 1.5, ink, 2.0 * Math.PI * 10.0 * 1.5 * 0.03)
    }
}

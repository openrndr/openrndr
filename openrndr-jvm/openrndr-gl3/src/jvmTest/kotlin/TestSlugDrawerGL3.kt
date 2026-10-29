import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugMap
import org.openrndr.math.Matrix44
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSlugDrawerGL3 : AbstractApplicationTestFixture() {

    @Test
    fun `strokes are composited premultiplied`() {
        val map = SlugMap(
            colorBuffer(4096, 4, type = ColorType.FLOAT32, format = ColorFormat.RG),
            colorBuffer(4096, 4, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
        )
        val index = map.addShape(Shape(listOf(Circle(16.0, 16.0, 10.0).contour)))
        val drawer = SlugDrawer().apply { contrast = 0.0 }
        val target = renderTarget(32, 32, contentScale = 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            drawer.draw(this, map, drawer.prepare(map, listOf(SlugCommand(index, Matrix44.IDENTITY, null, ColorRGBa.WHITE, 1.5))))
        }
        val buffer = ByteBuffer.allocateDirect(32 * 32 * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(buffer, ColorFormat.RGBa, ColorType.FLOAT32)
        val pixels = FloatArray(32 * 32 * 4) { buffer.getFloat(it * 4) }
        val colorBuffer = target.colorBuffer(0)
        target.detachColorAttachments()
        target.destroy()
        colorBuffer.destroy()
        drawer.close()

        // a white stroke stores its coverage in all channels
        for (i in 0 until 32 * 32) {
            assertEquals(pixels[i * 4 + 3], pixels[i * 4], 1E-4f)
        }
        // the ink of the stroke is close to its area
        val ink = (0 until 32 * 32).sumOf { pixels[it * 4].toDouble() }
        assertEquals(2.0 * Math.PI * 10.0 * 1.5, ink, 2.0 * Math.PI * 10.0 * 1.5 * 0.05)
    }
}

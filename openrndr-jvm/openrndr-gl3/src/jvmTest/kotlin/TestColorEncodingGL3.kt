import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Colors are linear in shaders and the window holds sRGB colors. TestColorEncodingWebGL checks the same values.
 */
class TestColorEncodingGL3 : AbstractApplicationTestFixture() {
    /** 0.5 as a linear color, encoded to sRGB it is 188 */
    private val linearHalf = ColorRGBa(0.5, 0.5, 0.5, 1.0)

    /** 128 as an sRGB color */
    private val srgbHalf = ColorRGBa.fromHex("#808080")

    /** the red component of the bottom left pixel of the bound framebuffer */
    private fun readRed(): Int {
        val buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        if (Driver.glType == DriverTypeGL.GL) {
            org.lwjgl.opengl.GL33C.glReadPixels(0, 0, 1, 1, 0x1908, 0x1401, buffer)
        } else {
            org.lwjgl.opengles.GLES30.glReadPixels(0, 0, 1, 1, 0x1908, 0x1401, buffer)
        }
        return buffer.get(0).toInt() and 0xff
    }

    private fun measure(target: RenderTarget?): Map<String, Int> {
        val drawer = program.drawer
        val results = mutableMapOf<String, Int>()
        fun draw() {
            drawer.ortho(RenderTarget.active)
            // past the edges, which are anti-aliased
            val cover = Rectangle(-1.0, -1.0, RenderTarget.active.width + 2.0, RenderTarget.active.height + 2.0)
            for ((name, color) in listOf("linear" to linearHalf, "srgb" to srgbHalf)) {
                drawer.clear(color)
                results["clear $name"] = readRed()

                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = color
                drawer.stroke = null
                drawer.rectangle(cover)
                results["fill $name"] = readRed()

                drawer.clear(ColorRGBa.BLACK)
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = "x_fill = p_color;"
                    parameter("color", color)
                }
                drawer.rectangle(cover)
                drawer.shadeStyle = null
                results["uniform $name"] = readRed()

                if (target != null) {
                    target.clearColor(0, color)
                    results["clearColor $name"] = readRed()
                }
            }
            val image = colorBuffer(4, 4, 1.0, type = ColorType.UINT8_SRGB)
            val pixels = ByteBuffer.allocateDirect(4 * 4 * 4)
            repeat(4 * 4 * 4) { pixels.put(it, 128.toByte()) }
            image.writeBuffer(pixels)
            drawer.clear(ColorRGBa.BLACK)
            drawer.image(image, 0.0, 0.0, RenderTarget.active.width.toDouble(), RenderTarget.active.height.toDouble())
            results["image"] = readRed()
            image.destroy()
        }
        if (target == null) {
            RenderTarget.active.bind()
            draw()
            RenderTarget.active.unbind()
        } else {
            drawer.isolatedWithTarget(target) { draw() }
        }
        return results
    }

    @Test
    fun `the window holds sRGB colors`() {
        assertEquals(windowExpectations, measure(null))
    }

    @Test
    fun `sRGB render targets hold sRGB colors`() {
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.UINT8_SRGB) }
        assertEquals(srgbTargetExpectations, measure(target))
        target.destroy()
    }

    @Test
    fun `linear render targets hold linear colors`() {
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.UINT8) }
        assertEquals(linearTargetExpectations, measure(target))
        target.destroy()
    }

    companion object {
        val windowExpectations = mapOf(
            "clear linear" to 188, "fill linear" to 188, "uniform linear" to 188,
            "clear srgb" to 128, "fill srgb" to 128, "uniform srgb" to 128,
            "image" to 128
        )
        val srgbTargetExpectations = windowExpectations + mapOf("clearColor linear" to 188, "clearColor srgb" to 128)
        val linearTargetExpectations = mapOf(
            "clear linear" to 128, "fill linear" to 128, "uniform linear" to 128, "clearColor linear" to 128,
            "clear srgb" to 55, "fill srgb" to 55, "uniform srgb" to 55, "clearColor srgb" to 55,
            "image" to 55
        )
    }
}

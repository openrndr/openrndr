import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Shade styles and filters keep working with the caches of their structures, shaders and render targets
 */
class TestShadeStylePipelineGL3 : AbstractApplicationTestFixture() {
    private fun pixels(buffer: ColorBuffer): ByteBuffer {
        val data = ByteBuffer.allocateDirect(buffer.effectiveWidth * buffer.effectiveHeight * 16).order(ByteOrder.nativeOrder())
        buffer.read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        return data
    }

    private fun ByteBuffer.color(width: Int, x: Int, y: Int): Vector4 {
        val i = (y * width + x) * 16
        return Vector4(getFloat(i).toDouble(), getFloat(i + 4).toDouble(), getFloat(i + 8).toDouble(), getFloat(i + 12).toDouble())
    }

    private fun assertColor(expected: Vector4, actual: Vector4, name: String) {
        assertTrue((expected - actual).length < 1E-3, "$name: expected $expected, was $actual")
    }

    @Test
    fun `a style drawn with two vertex formats uses its changed code with both`() {
        val target = renderTarget(32, 16, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        val style = shadeStyle { fragmentTransform = "x_fill = vec4(1.0, 0.0, 0.0, 1.0);" }
        fun draw() {
            program.drawer.isolatedWithTarget(target) {
                ortho(target)
                clear(ColorRGBa.TRANSPARENT)
                stroke = null
                shadeStyle = style
                circle(8.0, 8.0, 6.0)
                rectangle(18.0, 2.0, 12.0, 12.0)
            }
        }
        draw()
        // the circle is drawn first with the changed code, the rectangle must not keep the old structure
        style.fragmentTransform = "x_fill = vec4(0.0, 1.0, 0.0, 1.0);"
        draw()
        val data = pixels(target.colorBuffer(0))
        assertColor(Vector4(0.0, 1.0, 0.0, 1.0), data.color(32, 8, 8), "circle")
        assertColor(Vector4(0.0, 1.0, 0.0, 1.0), data.color(32, 24, 8), "rectangle")
        target.destroy()
    }

    @Test
    fun `int values of float parameters are uploaded as floats`() {
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        val style = shadeStyle { fragmentTransform = "x_fill = vec4(p_amount, 0.0, 0.0, 1.0);" }
        style.parameterValues["amount"] = 1
        style.parameterTypes["amount"] = "float"
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            stroke = null
            shadeStyle = style
            rectangle(0.0, 0.0, 4.0, 4.0)
        }
        assertColor(Vector4(1.0, 0.0, 0.0, 1.0), pixels(target.colorBuffer(0)).color(4, 1, 1), "rectangle")
        target.destroy()
    }

    @Test
    fun `filters bind buffer texture parameters to their shader`() {
        val data = bufferTexture(1, ColorFormat.RGBa, ColorType.FLOAT32)
        data.shadow.writer().apply { write(Vector4(0.25, 0.5, 0.75, 1.0)) }
        data.shadow.upload(0, 16)
        val shader = filterShaderFromCode(
            """
            #ifdef GL_ES
            precision highp float;
            precision highp samplerBuffer;
            #endif
            in vec2 v_texCoord0;
            uniform sampler2D tex0;
            uniform samplerBuffer data;
            out vec4 o_color;
            void main() { o_color = texelFetch(data, 0); }
            """.trimIndent(), "buffer-texture-filter"
        )
        val filter = Filter(shader)
        filter.parameters["data"] = data
        val input = colorBuffer(4, 4, type = ColorType.FLOAT32)
        val target = colorBuffer(4, 4, type = ColorType.FLOAT32)
        filter.apply(input, target)
        assertColor(Vector4(0.25, 0.5, 0.75, 1.0), pixels(target).color(4, 1, 1), "filtered")
        filter.destroy()
        listOf(input, target).forEach { it.destroy() }
        data.destroy()
    }

    @Test
    fun `shade style filters keep their padding and reuse their render target`() {
        val input = colorBuffer(8, 8, type = ColorType.FLOAT32)
        val target = colorBuffer(8, 8, type = ColorType.FLOAT32)
        target.fill(ColorRGBa.RED)
        val filter = ShadeStyleFilter1to1(shadeStyle { fragmentTransform = "x_fill = vec4(0.0, 1.0, 0.0, 1.0);" })
        filter.padding = 2
        repeat(3) { filter.apply(input, target) }
        val data = pixels(target)
        assertColor(Vector4(1.0, 0.0, 0.0, 1.0), data.color(8, 0, 0), "padding")
        assertColor(Vector4(0.0, 1.0, 0.0, 1.0), data.color(8, 4, 4), "inside")

        // a target of another size gets a render target of its size
        val small = colorBuffer(4, 4, type = ColorType.FLOAT32)
        filter.padding = 0
        filter.apply(input, small)
        assertColor(Vector4(0.0, 1.0, 0.0, 1.0), pixels(small).color(4, 0, 0), "small")
        filter.destroy()
        listOf(input, target, small).forEach { it.destroy() }
    }

    @Test
    fun `dynamic parameters are read after their values change`() {
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        val function = glslFunction<Double>("float x", "x * p_scale") { parameter("scale", 0.5) }
        val style = shadeStyle {
            fragmentTransform = "x_fill = vec4(p_f(1.0), 0.0, 0.0, 1.0);"
            parameter("f", function)
        }
        fun red(): Double {
            program.drawer.isolatedWithTarget(target) {
                ortho(target)
                clear(ColorRGBa.TRANSPARENT)
                stroke = null
                shadeStyle = style
                rectangle(0.0, 0.0, 4.0, 4.0)
            }
            return pixels(target.colorBuffer(0)).color(4, 1, 1).x
        }
        assertTrue(abs(red() - 0.5) < 1E-3)
        function.parameter("scale", 0.25)
        assertTrue(abs(red() - 0.25) < 1E-3)
        // another function with the same structure keeps the resolution, its value is read
        style.parameter("f", glslFunction<Double>("float x", "x * p_scale") { parameter("scale", 0.75) })
        assertTrue(abs(red() - 0.75) < 1E-3)
        target.destroy()
    }
}

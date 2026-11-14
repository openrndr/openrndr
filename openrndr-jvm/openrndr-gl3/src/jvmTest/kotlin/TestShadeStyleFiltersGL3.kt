import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ShadeStyleFilter2to1
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.shadeStyle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Shade style filters apply shade styles to their inputs
 */
class TestShadeStyleFiltersGL3 : AbstractApplicationTestFixture() {
    @Test
    fun twoToOneFiltersReadBothInputs() {
        fun buffer(color: ColorRGBa) = colorBuffer(4, 4, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).apply { fill(color) }
        val first = buffer(ColorRGBa(0.25, 0.0, 0.0, 1.0))
        val second = buffer(ColorRGBa(0.0, 0.5, 0.0, 1.0))
        val target = buffer(ColorRGBa.TRANSPARENT)

        val filter = ShadeStyleFilter2to1(shadeStyle {
            // the output of filters is premultiplied by its alpha
            fragmentTransform = "x_fill = vec4(texture(p_input0, c_textureCoordinate).rgb + texture(p_input1, c_textureCoordinate).rgb, 1.0);"
        })
        filter.apply(first, second, target)

        val data = ByteBuffer.allocateDirect(4 * 4 * 16).order(ByteOrder.nativeOrder())
        target.read(data)
        for (pixel in 0 until 16) {
            val (r, g, a) = listOf(0, 4, 12).map { data.getFloat(pixel * 16 + it).toDouble() }
            assertTrue(abs(r - 0.25) < 1E-4 && abs(g - 0.5) < 1E-4 && abs(a - 1.0) < 1E-4, "pixel $pixel: was ($r, $g, $a)")
        }
        listOf(first, second, target).forEach { it.destroy() }
    }
}

/**
 * Filters with generateInputMipmaps sample copies of their inputs with mipmaps
 */
class TestFilterInputMipmapsGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `inputs are sampled with mipmaps and are not changed`() {
        // the left half is red, the right half black, the 1x1 level of its mipmaps is half red
        val input = colorBuffer(8, 8, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        val pixels = ByteBuffer.allocateDirect(8 * 8 * 16).order(ByteOrder.nativeOrder())
        for (y in 0 until 8) for (x in 0 until 8) {
            pixels.putFloat(if (x < 4) 1.0f else 0.0f); pixels.putFloat(0.0f); pixels.putFloat(0.0f); pixels.putFloat(1.0f)
        }
        pixels.rewind()
        input.write(org.openrndr.utils.buffer.MPPBuffer(pixels))
        val target = colorBuffer(4, 4, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)

        val filter = org.openrndr.draw.ShadeStyleFilter1to1(shadeStyle {
            fragmentTransform = "x_fill = vec4(textureLod(p_input0, c_textureCoordinate, 3.0).rgb, 1.0);"
        })
        filter.generateInputMipmaps = true
        filter.apply(input, target)

        val data = ByteBuffer.allocateDirect(4 * 4 * 16).order(ByteOrder.nativeOrder())
        target.read(data)
        for (pixel in 0 until 16) {
            val r = data.getFloat(pixel * 16).toDouble()
            assertTrue(abs(r - 0.5) < 1E-3, "pixel $pixel: red was $r")
        }
        kotlin.test.assertEquals(1, input.levels)
        input.destroy()
        target.destroy()
    }
}

/**
 * A shade style filter that leaves x_fill alone passes its premultiplied input through unchanged
 */
class TestShadeStyleFilterIdentityGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `empty shade style filters do not change half transparent pixels`() {
        val size = 16
        // a disc with a soft edge, in premultiplied values
        val pixels = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        val expected = FloatArray(size * size * 4)
        for (y in 0 until size) for (x in 0 until size) {
            val d = kotlin.math.hypot(x - 7.5, y - 7.5) / 8.0
            val alpha = (1.0 - d).coerceIn(0.0, 1.0).toFloat()
            val rgba = floatArrayOf(0.8f * alpha, 0.4f * alpha, 0.2f * alpha, alpha)
            for (c in 0 until 4) {
                pixels.putFloat(rgba[c]); expected[(y * size + x) * 4 + c] = rgba[c]
            }
        }
        pixels.rewind()
        val input = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        input.write(org.openrndr.utils.buffer.MPPBuffer(pixels))
        val target = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)

        for (transform in listOf("", "x_fill.rgb *= 1.0;")) {
            val filter = org.openrndr.draw.ShadeStyleFilter1to1(shadeStyle { fragmentTransform = transform })
            filter.apply(input, target)
            val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
            target.read(data)
            for (i in 0 until size * size * 4) {
                val actual = data.getFloat(i * 4)
                assertTrue(abs(actual - expected[i]) < 1E-5, "'$transform' component $i: expected ${expected[i]}, was $actual")
            }
        }
        input.destroy()
        target.destroy()
    }
}

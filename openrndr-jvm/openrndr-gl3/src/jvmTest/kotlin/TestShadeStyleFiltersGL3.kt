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

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ImageAccess
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.glslFunction
import org.openrndr.draw.glslParameter
import org.openrndr.draw.imageBinding
import org.openrndr.draw.textureParameter
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.*
import org.openrndr.math.IntVector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compute styles resolve dynamic parameters like shade styles, resolvers can read built-in values of compute shaders
 */
class TestComputeStyleDynamicParametersGL3 : AbstractApplicationTestFixture() {
    private val size = 4

    private fun floats(buffer: ColorBuffer): FloatArray {
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        buffer.read(data)
        return FloatArray(size * size * 4) { data.getFloat(it * 4) }
    }

    private fun assertValues(expected: List<Double>, actual: List<Double>) {
        for ((index, pair) in expected.zip(actual).withIndex()) {
            assertTrue(abs(pair.first - pair.second) < 1E-4, "value $index: expected ${pair.first}, was ${pair.second}")
        }
    }

    @Test
    fun computeStylesResolveDynamicParameters() {
        if (!Driver.capabilities.compute) return

        // pixel (x, y) of the data is (x / 4, y / 4, 0.5, 1)
        val gradient = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        for (y in 0 until size) for (x in 0 until size) {
            data.putFloat(x / 4f); data.putFloat(y / 4f); data.putFloat(0.5f); data.putFloat(1f)
        }
        data.rewind()
        gradient.writeBuffer(data)
        val target = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)

        val amount = glslParameter<Double>("p_a * 2.0") { parameter("a", 0.25) }
        val style = computeStyle {
            computeTransform = """
                ivec2 p = ivec2(gl_GlobalInvocationID.xy);
                imageStore(p_target, p, vec4(p_amount, p_f(0.25), p_color.g, 1.0));
            """.trimIndent()
            image("target", target.imageBinding(0, ImageAccess.WRITE))
            workGroupSize = IntVector3(size, size, 1)
            parameter("amount", amount)
            // a function that reads a built-in value of compute shaders
            parameter("f", glslFunction<Double>("float x", "x + float(gl_GlobalInvocationID.x) * 0.125"))
            // compute shaders have no texture coordinate, the coordinate is given
            parameter("color", textureParameter<ColorRGBa>(gradient, "(vec2(gl_GlobalInvocationID.xy) + 0.5) / 4.0"))
        }
        style.execute(1, 1, 1)

        val pixels = floats(target)
        val indices = (0 until size * size)
        assertValues(indices.map { 0.5 }, indices.map { pixels[it * 4].toDouble() })
        assertValues(indices.map { 0.25 + (it % size) * 0.125 }, indices.map { pixels[it * 4 + 1].toDouble() })
        assertValues(indices.map { (it / size) / 4.0 }, indices.map { pixels[it * 4 + 2].toDouble() })

        // values change without new shaders
        amount.parameter("a", 0.375)
        style.execute(1, 1, 1)
        assertValues(indices.map { 0.75 }, floats(target).let { p -> indices.map { p[it * 4].toDouble() } })

        gradient.destroy()
        target.destroy()
    }
}

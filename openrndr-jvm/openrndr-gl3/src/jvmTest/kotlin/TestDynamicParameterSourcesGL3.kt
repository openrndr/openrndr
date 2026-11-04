import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DynamicParameter
import org.openrndr.draw.DynamicParameterSource
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.ShadeStyleFilter
import org.openrndr.draw.ShadeStyleManager
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.glslFunction
import org.openrndr.draw.glslLibrary
import org.openrndr.draw.glslParameter
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.Driver
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Dynamic parameter sources provide the dynamic parameters of styles, the parameters delegate holds dynamic parameters
 */
class TestDynamicParameterSourcesGL3 : AbstractApplicationTestFixture() {
    private val size = 4
    private val buffers = mutableListOf<ColorBuffer>()

    @AfterTest
    fun destroyBuffers() {
        buffers.forEach { it.destroy() }
        buffers.clear()
    }

    private fun target() = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).also { buffers += it }

    /** the red channel of the result of filtering with [style] */
    private fun filter(style: ShadeStyle): List<Double> {
        val target = target()
        ShadeStyleFilter(style).apply(target(), target)
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        target.read(data)
        return List(size * size) { data.getFloat(it * 16).toDouble() }
    }

    private fun assertValues(expected: Double, actual: List<Double>) {
        for ((index, value) in actual.withIndex()) {
            assertTrue(abs(expected - value) < 1E-4, "value $index: expected $expected, was $value")
        }
    }

    /** a preset of a function of a float, with a parameter of its own */
    private class Scale(factor: Double) : DynamicParameterSource {
        override var dynamicParameter: GlslFunction = glslFunction<Double>("float x", "x * p_factor") {
            parameter("factor", factor)
        }
        var factor: Double by dynamicParameter.Parameter("factor")
    }

    /** a preset that takes another preset */
    private class Twice(inner: DynamicParameterSource) : DynamicParameterSource {
        override val dynamicParameter = glslFunction<Double>("float x", "p_inner(p_inner(x))") {
            parameter("inner", inner)
        }
    }

    private fun style() = shadeStyle { fragmentTransform = "x_fill = vec4(p_f(0.25), 0.0, 0.0, 1.0);" }

    @Test
    fun sourcesProvideDynamicParameters() {
        val scale = Scale(2.0)
        val style = style().apply { parameter("f", scale) }
        assertValues(0.5, filter(style))
        // values are read from the dynamic parameter of the source
        scale.factor = 3.0
        assertValues(0.75, filter(style))
        // the source replaces its dynamic parameter, the style uses the new one
        scale.dynamicParameter = glslFunction<Double>("float x", "x + p_offset") { parameter("offset", 0.5) }
        assertValues(0.75, filter(style))
    }

    @Test
    fun sourcesCanBeNested() {
        val scale = Scale(2.0)
        val twice = Twice(scale)
        val style = style().apply { parameter("f", twice) }
        assertValues(1.0, filter(style))
        scale.factor = 0.5
        assertValues(0.0625, filter(style))
    }

    @Test
    fun sourcesWithTheSameStructureShareShaders() {
        val manager = ShadeStyleManager.fromGenerators(
            "test-sources",
            vsGenerator = Driver.instance.shaderGenerators::filterVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::filterFragmentShader
        )
        val format = vertexFormat { position(2); textureCoordinate(2) }
        val style = style()
        fun shader() = manager.shader(style, format).also { it.end() }

        style.parameter("f", Scale(2.0))
        val first = shader()
        style.parameter("f", Scale(4.0))
        assertSame(first, shader())
        style.parameter("f", Twice(Scale(2.0)))
        assertNotSame(first, shader())
    }

    @Test
    fun sourcesUseLibraries() {
        val half = glslLibrary("half", "float half_f(float x) { return x * 0.5; }")
        val source = object : DynamicParameterSource {
            override val dynamicParameter = glslFunction<Double>("float x", "half_f(x)", listOf(half))
        }
        assertValues(0.125, filter(style().apply { parameter("f", source) }))
    }

    /** a style with properties that hold a dynamic parameter and a source */
    private class Properties : ShadeStyle() {
        var amount: DynamicParameter by Parameter("amount", glslParameter<Double>("0.25"))
        var f: DynamicParameterSource by Parameter("f", Scale(2.0))

        init {
            fragmentTransform = "x_fill = vec4(p_f(p_amount), 0.0, 0.0, 1.0);"
        }
    }

    @Test
    fun parameterPropertiesHoldDynamicParameters() {
        val style = Properties()
        assertValues(0.5, filter(style))
        style.amount = glslParameter<Double>("0.125 + 0.0 * c_boundsPosition.x")
        assertValues(0.25, filter(style))
        style.f = Scale(4.0)
        assertValues(0.5, filter(style))
    }
}

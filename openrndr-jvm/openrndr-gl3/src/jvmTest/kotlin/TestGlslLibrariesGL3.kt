import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ImageAccess
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.ShadeStyleFilter
import org.openrndr.draw.ShadeStyleManager
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.glslFunction
import org.openrndr.draw.glslLibrary
import org.openrndr.draw.glslParameter
import org.openrndr.draw.imageBinding
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.resolveDynamicParameters
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.*
import org.openrndr.math.IntVector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * GLSL libraries are shared by shade styles, compute styles and dynamic parameters
 */
class TestGlslLibrariesGL3 : AbstractApplicationTestFixture() {
    private val size = 4
    private val buffers = mutableListOf<ColorBuffer>()

    @AfterTest
    fun destroyBuffers() {
        buffers.forEach { it.destroy() }
        buffers.clear()
    }

    private val double = glslLibrary("double", "float double_f(float x) { return x * 2.0; }")
    private val quadruple = glslLibrary("quadruple", "float quadruple_f(float x) { return double_f(double_f(x)); }", double)

    private fun target() = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).also { buffers += it }

    private fun reds(buffer: ColorBuffer): List<Double> {
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        buffer.read(data)
        return List(size * size) { data.getFloat(it * 16).toDouble() }
    }

    private fun assertValues(expected: Double, actual: List<Double>) {
        for ((index, value) in actual.withIndex()) {
            assertTrue(abs(expected - value) < 1E-4, "value $index: expected $expected, was $value")
        }
    }

    /** the red channel of the result of filtering with [style] */
    private fun filter(style: ShadeStyle): List<Double> {
        val source = target()
        val target = target()
        ShadeStyleFilter(style).apply(source, target)
        return reds(target)
    }

    private fun libraryCount(code: String?, name: String) = code?.split("// -- library $name\n")?.size?.minus(1) ?: 0

    @Test
    fun stylesUseLibraries() {
        val style = shadeStyle {
            libraries += quadruple
            fragmentTransform = "x_fill = vec4(quadruple_f(0.125), 0.0, 0.0, 1.0);"
        }
        assertValues(0.5, filter(style))
    }

    @Test
    fun functionsUseLibraries() {
        val style = shadeStyle { fragmentTransform = "x_fill = vec4(p_f(0.125), 0.0, 0.0, 1.0);" }
        style.parameter("f", glslFunction<Double>("float x", "quadruple_f(x) + p_offset", listOf(quadruple)) {
            parameter("offset", 0.25)
        })
        assertValues(0.75, filter(style))

        val parameter = shadeStyle { fragmentTransform = "x_fill = vec4(p_amount, 0.0, 0.0, 1.0);" }
        parameter.parameter("amount", glslParameter<Double>("double_f(0.25)", listOf(double)))
        assertValues(0.5, filter(parameter))
    }

    @Test
    fun sharedLibrariesAreEmittedOnce() {
        // the style, two functions and a nested function use the library, it is a dependency of another library
        val style = shadeStyle {
            libraries += double
            fragmentTransform = "x_fill = vec4(p_f(0.0625) + p_g(0.0625) + double_f(0.0), 0.0, 0.0, 1.0);"
        }
        style.parameter("f", glslFunction<Double>("float x", "double_f(x)", listOf(double)))
        style.parameter("g", glslFunction<Double>("float x", "p_inner(x)", listOf(quadruple)) {
            parameter("inner", glslFunction<Double>("float x", "quadruple_f(x)", listOf(quadruple, double)))
        })
        val preamble = style.resolveDynamicParameters().fragmentPreamble
        assertEquals(1, libraryCount(preamble, "double"))
        assertEquals(1, libraryCount(preamble, "quadruple"))
        // dependencies go first
        assertTrue(preamble!!.indexOf("library double") < preamble.indexOf("library quadruple"))
        assertValues(0.375, filter(style))
    }

    @Test
    fun librariesAreEmittedInTheStagesThatUseThem() {
        val style = shadeStyle {
            libraries += listOf(double, glslLibrary("unused", "float unused_f() { return 0.0; }"))
            fragmentTransform = "x_fill.r = double_f(0.25);"
        }
        val resolved = style.resolveDynamicParameters()
        assertNull(resolved.vertexPreamble)
        assertEquals(1, libraryCount(resolved.fragmentPreamble, "double"))
        assertEquals(0, libraryCount(resolved.fragmentPreamble, "unused"))
    }

    @Test
    fun librariesWithTheSameNameAndOtherCodeAreAnError() {
        val other = glslLibrary("double", "float double_f(float x) { return x + x; }")
        val style = shadeStyle {
            libraries += double
            fragmentTransform = "x_fill.r = p_f(double_f(0.25));"
        }
        style.parameter("f", glslFunction<Double>("float x", "double_f(x)", listOf(other)))
        assertFailsWith<IllegalArgumentException> { style.resolveDynamicParameters() }

        // the same library made twice is the same library
        style.parameter("f", glslFunction<Double>("float x", "double_f(x)", listOf(glslLibrary(double.name, double.code))))
        assertEquals(1, libraryCount(style.resolveDynamicParameters().fragmentPreamble, "double"))
    }

    @Test
    fun swappingLibrariesMakesOneShaderForEach() {
        val manager = ShadeStyleManager.fromGenerators(
            "test-libraries",
            vsGenerator = Driver.instance.shaderGenerators::filterVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::filterFragmentShader
        )
        val format = vertexFormat { position(2); textureCoordinate(2) }
        val triple = glslLibrary("scale", "float scale_f(float x) { return x * 3.0; }")
        val half = glslLibrary("scale", "float scale_f(float x) { return x * 0.5; }")
        val style = shadeStyle { fragmentTransform = "x_fill.r = p_f(0.25);" }
        fun f(library: org.openrndr.draw.GlslLibrary) = glslFunction<Double>("float x", "scale_f(x)", listOf(library))

        fun shader() = manager.shader(style, format).also { it.end() }
        style.parameter("f", f(triple))
        val first = shader()
        style.parameter("f", f(half))
        val second = shader()
        assertNotSame(first, second)
        // the shaders are kept, functions made anew with the same library share them
        style.parameter("f", f(triple))
        assertSame(first, shader())
        style.parameter("f", f(half))
        assertSame(second, shader())

        // libraries of styles too
        val styled = shadeStyle { fragmentTransform = "x_fill.r = scale_f(0.25);"; libraries = listOf(triple) }
        val third = manager.shader(styled, format).also { it.end() }
        styled.libraries = listOf(half)
        assertNotSame(third, manager.shader(styled, format).also { it.end() })
        styled.libraries = listOf(triple)
        assertSame(third, manager.shader(styled, format).also { it.end() })
    }

    @Test
    fun composedStylesShareLibraries() {
        val add = shadeStyle {
            libraries += double
            fragmentTransform = "x_fill = vec4(double_f(0.125), 0.0, 0.0, 1.0);"
        }
        val style = add + shadeStyle { libraries += double; fragmentTransform = "x_fill.r += double_f(0.0625);" }
        assertEquals(1, libraryCount(style.resolveDynamicParameters().fragmentPreamble, "double"))

        val target = renderTarget(size, size, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            stroke = null
            shadeStyle = style
            rectangle(0.0, 0.0, size.toDouble(), size.toDouble())
        }
        assertValues(0.375, reds(target.colorBuffer(0)))
        target.destroy()
    }

    @Test
    fun computeStylesUseLibraries() {
        if (!Driver.capabilities.compute) return
        val target = target()
        val style = computeStyle {
            libraries += quadruple
            computeTransform = "imageStore(p_target, ivec2(gl_GlobalInvocationID.xy), vec4(p_f(0.0625), 0.0, 0.0, 1.0));"
            image("target", target.imageBinding(0, ImageAccess.WRITE))
            workGroupSize = IntVector3(size, size, 1)
            parameter("f", glslFunction<Double>("float x", "quadruple_f(x) + double_f(x)", listOf(double)))
        }
        style.execute(1, 1, 1)
        assertValues(0.375, reds(target))
    }
}

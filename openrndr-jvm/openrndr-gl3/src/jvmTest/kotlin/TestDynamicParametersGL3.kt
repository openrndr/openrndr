import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.ShadeStyleFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.glslFunction
import org.openrndr.draw.glslParameter
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.textureParameter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Dynamic parameters are read from textures or computed by GLSL code, in drawers and in shade style filters
 */
class TestDynamicParametersGL3 : AbstractApplicationTestFixture() {
    private val size = 4
    private val buffers = mutableListOf<ColorBuffer>()

    @AfterTest
    fun destroyBuffers() {
        buffers.forEach { it.destroy() }
        buffers.clear()
    }

    /** a 4x4 float image, pixel (x, y) of the data (bottom row first) is (x / 4, y / 4, 0.5, 1) */
    private fun gradient(): ColorBuffer {
        val image = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        for (y in 0 until size) for (x in 0 until size) {
            data.putFloat(x / 4f); data.putFloat(y / 4f); data.putFloat(0.5f); data.putFloat(1f)
        }
        data.rewind()
        image.writeBuffer(data)
        return image.also { buffers += it }
    }

    private fun read(buffer: ColorBuffer): List<ColorRGBa> {
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        buffer.read(data)
        return List(size * size) {
            ColorRGBa(data.getFloat(it * 16).toDouble(), data.getFloat(it * 16 + 4).toDouble(),
                data.getFloat(it * 16 + 8).toDouble(), data.getFloat(it * 16 + 12).toDouble())
        }
    }

    /** draws a rectangle covering a 4x4 float target with [style], returns its pixels */
    private fun render(style: ShadeStyle?, draw: (org.openrndr.draw.Drawer.() -> Unit)? = null): List<ColorRGBa> {
        val target = renderTarget(size, size, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            if (draw != null) {
                draw()
            } else {
                fill = ColorRGBa.BLACK
                stroke = null
                shadeStyle = style
                rectangle(0.0, 0.0, size.toDouble(), size.toDouble())
            }
        }
        return read(target.colorBuffer(0)).also { target.destroy() }
    }

    private fun assertColors(expected: List<ColorRGBa>, actual: List<ColorRGBa>) {
        for ((index, pair) in expected.zip(actual).withIndex()) {
            val (e, a) = pair
            assertTrue(abs(e.r - a.r) < 1E-4 && abs(e.g - a.g) < 1E-4 && abs(e.b - a.b) < 1E-4 && abs(e.alpha - a.alpha) < 1E-4,
                "pixel $index: expected $e, was $a")
        }
    }

    private fun assertValues(expected: List<Double>, actual: List<Double>) {
        for ((index, pair) in expected.zip(actual).withIndex()) {
            assertTrue(abs(pair.first - pair.second) < 1E-4, "value $index: expected ${pair.first}, was ${pair.second}")
        }
    }

    private fun uniform(r: Double, g: Double, b: Double) = List(size * size) { ColorRGBa(r, g, b, 1.0) }

    /** fills with the value of a float parameter, opaque, the alpha is multiplied in */
    private fun fillWith(name: String) = shadeStyle { fragmentTransform = "x_fill = vec4(vec3(p_$name), 1.0);" }

    @Test
    fun constantsAreUnchanged() {
        val style = shadeStyle {
            fragmentTransform = "x_fill = p_color;"
            parameter("color", ColorRGBa(0.25, 0.5, 0.75, 1.0))
        }
        assertColors(uniform(0.25, 0.5, 0.75), render(style))
    }

    @Test
    fun texturesAreDrawnLikeImages() {
        val image = gradient()
        val expected = render(null) { image(image, 0.0, 0.0, size.toDouble(), size.toDouble()) }
        val style = shadeStyle {
            fragmentTransform = "x_fill = p_color;"
            parameter("color", textureParameter<ColorRGBa>(image))
        }
        assertColors(expected, render(style))
    }

    @Test
    fun floatTexturesReadTheRedChannel() {
        val image = gradient()
        val style = shadeStyle {
            fragmentTransform = "x_fill = vec4(p_amount, 0.0, 0.0, 1.0);"
            parameter("amount", textureParameter<Double>(image))
        }
        val reds = render(style).map { it.r }
        val expected = render(null) { image(image, 0.0, 0.0, size.toDouble(), size.toDouble()) }.map { it.r }
        assertValues(expected, reds)
    }

    @Test
    fun glslExpressionsAndFunctionBodies() {
        val expression = glslParameter<Double>("p_a * 2.0") { parameter("a", 0.25) }
        assertColors(uniform(0.5, 0.5, 0.5), render(fillWith("amount").apply { parameter("amount", expression) }))

        val body = glslParameter<Double>("float twice = p_a * 2.0;\nreturn twice + 0.25;") { parameter("a", 0.125) }
        assertColors(uniform(0.5, 0.5, 0.5), render(fillWith("amount").apply { parameter("amount", body) }))
    }

    @Test
    fun nestedParametersCanBeDynamic() {
        val image = gradient()
        // the red channel of the image, plus a constant
        val amount = glslParameter<Double>("p_image + p_offset") {
            parameter("image", textureParameter<Double>(image))
            parameter("offset", 0.5)
        }
        val reds = render(fillWith("amount").apply { parameter("amount", amount) }).map { it.r }
        val imageReds = render(null) { image(image, 0.0, 0.0, size.toDouble(), size.toDouble()) }.map { it.r }
        assertValues(imageReds.map { it + 0.5 }, reds)
    }

    @Test
    fun valuesChangeWithoutNewShaders() {
        val amount = glslParameter<Double>("p_a") { parameter("a", 0.25) }
        val style = fillWith("amount").apply { parameter("amount", amount) }
        assertColors(uniform(0.25, 0.25, 0.25), render(style))
        amount.parameter("a", 0.75)
        assertColors(uniform(0.75, 0.75, 0.75), render(style))

        val first = gradient()
        val second = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).also { buffers += it }
        second.fill(ColorRGBa(0.125, 0.25, 0.375, 1.0))
        val textured = shadeStyle {
            fragmentTransform = "x_fill = p_color;"
            parameter("color", textureParameter<ColorRGBa>(first))
        }
        render(textured)
        textured.parameter("color", textureParameter<ColorRGBa>(second))
        assertColors(uniform(0.125, 0.25, 0.375), render(textured))
    }

    @Test
    fun composedStylesKeepTheirDynamicParameters() {
        fun add(channel: String, amount: Double) = shadeStyle {
            fragmentTransform = "x_fill.$channel += p_amount;"
            parameter("amount", glslParameter<Double>("p_a") { parameter("a", amount) })
        }
        assertColors(uniform(0.25, 0.5, 0.0), render(add("r", 0.25) + add("g", 0.5)))
    }

    @Test
    fun shadeStyleFiltersUseDynamicParameters() {
        val image = gradient()
        val target = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).also { buffers += it }

        val filter = ShadeStyleFilter(shadeStyle { fragmentTransform = "x_fill = vec4(p_amount, p_color.g, 0.0, 1.0);" })
        filter.parameter("amount", glslParameter<Double>("p_a * 2.0") { parameter("a", 0.25) })
        // read at the texture coordinate of the filter, like its input
        filter.parameter("color", textureParameter<ColorRGBa>(image))
        filter.apply(image, target)

        val pixels = read(target)
        val imagePixels = read(image)
        assertValues(List(size * size) { 0.5 }, pixels.map { it.r })
        assertValues(imagePixels.map { it.g }, pixels.map { it.g })
    }

    private fun fillWithExpression(expression: String) =
        shadeStyle { fragmentTransform = "x_fill = vec4(vec3($expression), 1.0);" }

    @Test
    fun functionsWithoutContextAreCalled() {
        val style = fillWithExpression("p_f(0.25)")
        style.parameter("f", glslFunction<Double>("float x", "x * p_scale") { parameter("scale", 2.0) })
        assertColors(uniform(0.5, 0.5, 0.5), render(style))
    }

    @Test
    fun functionsReceiveTheContext() {
        // the position in the bounds, at the centers of the pixels of a row
        val style = fillWithExpression("p_f(1.0)")
        style.parameter("f", glslFunction<Double>("float k", "c_boundsPosition.x * k"))
        assertValues(List(size * size) { (it % size + 0.5) / size }, render(style).map { it.r })
    }

    @Test
    fun functionsWithSeveralArguments() {
        val style = fillWithExpression("p_sum(0.25, p_dot(vec2(0.25, 0.5)))")
        style.parameter("sum", glslFunction<Double>("float a, float b", "a + b + 0.0 * c_boundsPosition.x"))
        style.parameter("dot", glslFunction<Double>("vec2 v", "v.x + v.y"))
        assertColors(uniform(1.0, 1.0, 1.0), render(style))
    }

    @Test
    fun stylesCanTakeShapes() {
        // a style that fills the inside of a signed distance function
        val mask = shadeStyle { fragmentTransform = "x_fill = vec4(vec3(p_shape(c_boundsPosition.xy) < 0.0 ? 1.0 : 0.0), 1.0);" }
        mask.parameter("shape", glslFunction<Double>("vec2 p", "length(p - vec2(0.5)) - p_radius") { parameter("radius", 0.3) })
        val pixels = render(mask).map { it.r }
        // the four center pixels are inside, the corners are outside
        assertValues(listOf(1.0, 1.0, 1.0, 1.0), listOf(5, 6, 9, 10).map { pixels[it] })
        assertValues(listOf(0.0, 0.0, 0.0, 0.0), listOf(0, 3, 12, 15).map { pixels[it] })
    }

    @Test
    fun functionsCanBeNested() {
        val style = fillWithExpression("p_f(0.125)")
        style.parameter("f", glslFunction<Double>("float x", "p_inner(x) + p_offset") {
            parameter("inner", glslFunction<Double>("float y", "y * 2.0"))
            parameter("offset", 0.25)
        })
        assertColors(uniform(0.5, 0.5, 0.5), render(style))

        val expression = fillWith("amount")
        expression.parameter("amount", glslParameter<Double>("p_f(0.25)") {
            parameter("f", glslFunction<Double>("float x", "x * 2.0 + 0.0 * c_boundsPosition.x"))
        })
        assertColors(uniform(0.5, 0.5, 0.5), render(expression))
    }

    @Test
    fun shadeStyleFiltersCallFunctions() {
        val image = gradient()
        val target = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).also { buffers += it }
        val filter = ShadeStyleFilter(shadeStyle { fragmentTransform = "x_fill = vec4(p_f(x_fill.r), 0.0, 0.0, 1.0);" })
        filter.parameter("f", glslFunction<Double>("float x", "x + p_offset") { parameter("offset", 0.25) })
        filter.apply(image, target)
        assertValues(read(image).map { it.r + 0.25 }, read(target).map { it.r })
    }

    @Test
    fun composedStylesKeepTheirFunctions() {
        fun channel(channel: String, scale: Double) = shadeStyle {
            fragmentTransform = "x_fill.$channel = p_f(0.25);"
            parameter("f", glslFunction<Double>("float x", "x * p_scale + 0.0 * c_boundsPosition.x") { parameter("scale", scale) })
        }
        assertColors(uniform(0.5, 0.75, 0.0), render(channel("r", 2.0) + channel("g", 3.0)))
    }
}

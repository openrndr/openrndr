import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.composeShadeStyles
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Composed shade styles apply their members in order, each in its own namespace
 */
class TestComposedShadeStyleGL3 : AbstractApplicationTestFixture() {

    /** draws a rectangle with [style] in a 4x4 float target, returns the color of a pixel */
    private fun render(style: ShadeStyle): ColorRGBa {
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            fill = ColorRGBa.BLACK
            stroke = null
            shadeStyle = style
            rectangle(-1.0, -1.0, 6.0, 6.0)
        }
        val buffer = ByteBuffer.allocateDirect(4 * 4 * 4 * 4)
        target.colorBuffer(0).read(buffer)
        val color = ColorRGBa(buffer.getFloat(0).toDouble(), buffer.getFloat(4).toDouble(), buffer.getFloat(8).toDouble(), buffer.getFloat(12).toDouble())
        target.destroy()
        return color
    }

    private fun assertColor(r: Double, g: Double, b: Double, actual: ColorRGBa) {
        assertTrue(abs(actual.r - r) < 1E-4 && abs(actual.g - g) < 1E-4 && abs(actual.b - b) < 1E-4, "expected ($r, $g, $b), was $actual")
    }

    /** adds a constant to a channel, with a helper function and a parameter that are named the same in every style */
    private fun add(channel: String, amount: Double) = shadeStyle {
        fragmentPreamble = "float offset(float x) { return x + p_amount; }"
        fragmentTransform = "x_fill.$channel = offset(x_fill.$channel);"
        parameter("amount", amount)
    }

    @Test
    fun membersWithTheSameNamesDoNotCollide() {
        assertColor(0.25, 0.5, 0.0, render(add("r", 0.25) + add("g", 0.5)))
    }

    @Test
    fun theSameStyleCanBeAMemberTwice() {
        val style = add("b", 0.125)
        assertColor(0.0, 0.0, 0.25, render(composeShadeStyles(style, style)))
    }

    @Test
    fun membersAreAppliedInOrder() {
        val double = shadeStyle {
            fragmentTransform = "x_fill.r *= p_amount;"
            parameter("amount", 2.0)
        }
        assertColor(0.5, 0.0, 0.0, render(add("r", 0.25) + double))
        assertColor(0.25, 0.0, 0.0, render(double + add("r", 0.25)))
    }

    @Test
    fun valuesOfMembersAreUsed() {
        val red = add("r", 0.25)
        val composed = red + add("g", 0.5)
        assertColor(0.25, 0.5, 0.0, render(composed))
        red.parameter("amount", 0.75)
        assertColor(0.75, 0.5, 0.0, render(composed))
        // values can also be set on the composition by their namespaced names
        composed.parameter("s1_amount", 0.125)
        assertColor(0.75, 0.125, 0.0, render(composed))
    }

    @Test
    fun structureOfMembersIsUsed() {
        val red = add("r", 0.25)
        val composed = red + add("g", 0.5)
        assertColor(0.25, 0.5, 0.0, render(composed))
        red.fragmentTransform = "x_fill.b = offset(x_fill.b);"
        assertColor(0.0, 0.5, 0.25, render(composed))
    }

    @Test
    fun compositionsCanBeComposed() {
        val composed = (add("r", 0.25) + add("g", 0.5)) + add("b", 0.125)
        assertColor(0.25, 0.5, 0.125, render(composed))
    }

    @Test
    fun varyingsOfMembersDoNotCollide() {
        fun varying(channel: String, value: Double) = shadeStyle {
            vertexPreamble = "out float va_value;"
            fragmentPreamble = "in float va_value;"
            vertexTransform = "va_value = p_value;"
            fragmentTransform = "x_fill.$channel = va_value;"
            parameter("value", value)
        }
        assertColor(0.25, 0.75, 0.0, render(varying("r", 0.25) + varying("g", 0.75)))
    }
}

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import kotlin.math.sin

/**
 * Composes two shade styles with `+`. Both styles have a parameter called `amount` and a helper function called
 * `wave`; every style in a composition keeps its own names, so they do not collide. The styles are applied in order:
 * the stripes first, then the tint.
 *
 * The values of the parameters are set on the styles themselves, the composition uses them without new shaders.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        // dark stripes across the bounds, `amount` is the number of stripes
        val stripes = shadeStyle {
            fragmentPreamble = "float wave(float x) { return 0.5 + 0.5 * sin(x * 6.2831853); }"
            fragmentTransform = "x_fill.rgb *= mix(0.4, 1.0, step(0.5, wave(c_boundsPosition.x * p_amount)));"
            parameter("amount", 8.0)
        }

        // a tint that runs from top to bottom, `amount` is the strength of the tint
        val tint = shadeStyle {
            fragmentPreamble = "float wave(float x) { return 0.5 + 0.5 * cos(x * 3.1415927); }"
            fragmentTransform = "x_fill.rgb = mix(x_fill.rgb, p_color.rgb, p_amount * wave(c_boundsPosition.y));"
            parameter("amount", 0.5)
            parameter("color", ColorRGBa.fromHex("#2a9d8f"))
        }

        val composed = stripes + tint

        extend {
            drawer.clear(ColorRGBa.fromHex("#264653"))

            // the values of the members change every frame
            stripes.parameter("amount", 6.0 + 4.0 * sin(seconds))
            tint.parameter("amount", 0.5 + 0.5 * sin(seconds * 1.3))

            drawer.stroke = null
            drawer.fill = ColorRGBa.fromHex("#e9c46a")
            drawer.shadeStyle = composed
            drawer.circle(width * 0.3, height * 0.3, 160.0)
            drawer.rectangle(width * 0.45, height * 0.45, 300.0, 300.0)

            // the members can still be used on their own
            drawer.shadeStyle = stripes
            drawer.rectangle(40.0, height - 140.0, 240.0, 100.0)
            drawer.shadeStyle = tint
            drawer.rectangle(width - 280.0, 40.0, 240.0, 100.0)
        }
    }
}

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.glslParameter
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.textureParameter

/**
 * Dynamic shade style parameters: the shade style code only reads `p_color` and `p_brightness`, what they are is
 * decided by their values.
 *
 * - `color` is read from an image, which covers the bounds of every shape like `drawer.image` shows it
 * - `brightness` is computed by GLSL code with its own parameter, `time`
 *
 * Changing the time every frame does not make new shaders, it is a uniform.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("data/images/rgb-8_8_8.jpg")

        val brightness = glslParameter<Double>("0.65 + 0.35 * sin(p_time * 2.0 + c_boundsPosition.x * 6.2831853)") {
            parameter("time", 0.0)
        }

        val style = shadeStyle {
            fragmentTransform = "x_fill.rgb = p_color.rgb * p_brightness;"
            parameter("color", textureParameter<ColorRGBa>(image))
            parameter("brightness", brightness)
        }

        extend {
            drawer.clear(ColorRGBa.fromHex("#1d3557"))
            brightness.parameter("time", seconds)

            drawer.stroke = null
            drawer.shadeStyle = style
            // every shape shows the whole image over its bounds
            drawer.rectangle(40.0, 40.0, 400.0, 260.0)
            drawer.circle(560.0, 180.0, 120.0)
            drawer.rectangle(40.0, 360.0, 640.0, 320.0)
        }
    }
}

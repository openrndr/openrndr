import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyleFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.glslFunction
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2

/**
 * A shade style filter that takes a function: the filter reads its input at the position the `warp` function
 * returns. The warp has parameters of its own, the time changes every frame.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val image = loadImage("data/images/rgb-8_8_8.jpg")
        val warped = colorBuffer(image.width, image.height)

        val warp = glslFunction<Vector2>(
            "vec2 uv",
            "uv + p_strength * vec2(sin(uv.y * p_frequency + p_time), cos(uv.x * p_frequency + p_time))"
        ) {
            parameter("strength", 0.02)
            parameter("frequency", 20.0)
            parameter("time", 0.0)
        }

        val filter = ShadeStyleFilter(shadeStyle {
            fragmentTransform = "x_fill = texture(p_input0, p_warp(c_textureCoordinate));"
        })
        filter.parameter("warp", warp)

        extend {
            warp.parameter("time", seconds * 2.0)
            filter.apply(image, warped)

            drawer.clear(ColorRGBa.BLACK)
            drawer.image(warped, 0.0, 0.0, width.toDouble(), height.toDouble())
        }
    }
}

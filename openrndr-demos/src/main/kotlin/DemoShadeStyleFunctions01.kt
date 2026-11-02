import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.glslFunction
import org.openrndr.draw.shadeStyle
import kotlin.math.sin

/**
 * A shade style that takes a function: it draws the inside of a signed distance function, with lines at equal
 * distances outside of it. The style calls the function as `p_shape(position)`, which function that is, is a
 * parameter.
 *
 * Four copies of the style get four signed distance functions, each with parameters of its own that change every
 * frame.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        /** the shape style, positions are -1 to 1 over the bounds */
        fun shapeStyle(): ShadeStyle = shadeStyle {
            fragmentTransform = """
                vec2 p = c_boundsPosition.xy * 2.0 - 1.0;
                float d = p_shape(p);
                float lines = smoothstep(0.02, 0.0, abs(fract(d * 8.0) - 0.5) / 8.0);
                vec3 inside = vec3(0.95, 0.75, 0.3);
                vec3 outside = mix(vec3(0.1, 0.15, 0.25), vec3(0.6, 0.7, 0.8), lines);
                x_fill = vec4(mix(inside, outside, smoothstep(0.0, fwidth(d), d)), 1.0);
            """.trimIndent()
        }

        val circle = glslFunction<Double>("vec2 p", "length(p) - p_radius") {
            parameter("radius", 0.5)
        }
        val box = glslFunction<Double>(
            "vec2 p", """
            vec2 q = abs(p) - p_size;
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - p_rounding;
            """.trimIndent()
        ) {
            parameter("size", 0.4)
            parameter("rounding", 0.1)
        }
        val rings = glslFunction<Double>("vec2 p", "abs(length(p) - p_radius) - p_width") {
            parameter("radius", 0.5)
            parameter("width", 0.1)
        }
        // a function that uses another function: the union of two circles
        val pair = glslFunction<Double>("vec2 p", "min(p_circle(p - p_offset), p_circle(p + p_offset))") {
            parameter("circle", glslFunction<Double>("vec2 p", "length(p) - 0.3"))
            parameter("offset", 0.3)
        }

        val shapes = listOf(circle, box, rings, pair).map { shape ->
            shapeStyle().apply { parameter("shape", shape) }
        }

        extend {
            drawer.clear(ColorRGBa.BLACK)
            circle.parameter("radius", 0.5 + 0.2 * sin(seconds))
            box.parameter("rounding", 0.1 + 0.1 * sin(seconds * 1.5))
            rings.parameter("width", 0.08 + 0.05 * sin(seconds * 2.0))
            pair.parameter("offset", 0.3 + 0.2 * sin(seconds * 0.7))

            drawer.stroke = null
            for ((index, style) in shapes.withIndex()) {
                drawer.shadeStyle = style
                drawer.rectangle(20.0 + (index % 2) * 350.0, 20.0 + (index / 2) * 350.0, 330.0, 330.0)
            }
        }
    }
}

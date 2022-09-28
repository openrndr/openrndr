import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyleFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.shadeStyle

fun main() = application {
    program {
        val filtered = colorBuffer(width, height)
        val shadeFilter = ShadeStyleFilter(shadeStyle {
            fragmentTransform = """
               vec2 ar = c_boundsSize.y > c_boundsSize.x ? vec2(1.0, c_boundsSize.y / c_boundsSize.x) : vec2(c_boundsSize.x / c_boundsSize.y, 1.0);
               vec2 d = c_boundsPosition.xy*ar - vec2(0.5)*ar;
               float l = length(d);
               float sl = smoothstep(0.24, 0.5, l);
               x_fill = vec4(vec3(sl), 1.0); 
               x_fill.rgb *= p_color.rgb;
            """
        })
        extend {
            shadeFilter.parameter("color", ColorRGBa.PINK)
            shadeFilter.apply(emptyArray(), filtered)
            drawer.image(filtered)
        }
    }
}
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Polar
import org.openrndr.shape.contour

fun main() = application {
    program {
        val style = shadeStyle {
            //fragmentTransform = "x_stroke.a *= step(0.5, fract(c_contourPosition / p_dashLen));"
            fragmentTransform = "x_stroke.a *= smoothstep(0.0, 1.0, mod(c_contourPosition, p_dashLen)) * smoothstep(p_dashLen, p_dashLen-1.0, mod(c_contourPosition, p_dashLen));"
            parameter("dashLen", 20.0)
        }
        extend {
            drawer.run {
                clear(ColorRGBa.WHITE)
                stroke = ColorRGBa.BLACK.opacify(0.5)
                val c = contour {
                    moveTo(100.0, 100.0)
                    continueTo(100.0, 300.0)
                    continueTo(bounds.center + Polar(seconds * 30, 100.0).cartesian)
                    continueTo(500.0, 100.0)
                    continueTo(600.0, 100.0)
                }
                shadeStyle = style
                contour(c)

                drawer.lineSegment(0.0, 0.0, width*1.0, height*1.0)
            }
        }
    }
}
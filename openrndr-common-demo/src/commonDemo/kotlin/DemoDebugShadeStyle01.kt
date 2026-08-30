import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import org.openrndr.internal.Driver
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            title = "LGM - OPENRNDR - Test"
        }
        program {
            fun createCurve() = Circle(drawer.bounds.center, 100.0).contour.open // <--- *****
            var curve = createCurve()
            window.sized.listen {
                curve = createCurve()
            }
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.strokeWeight = 10.0
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                    float lfo = sin(p_seconds) * 0.5 + 0.5;
                    x_stroke = vec4(lfo, lfo, lfo, 1.0);
                """.trimIndent()
                    parameter("seconds", seconds)
                }
                drawer.contour(curve)
                Driver.instance.finish()
            }
        }
    }
}
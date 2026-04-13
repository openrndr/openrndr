import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.contour

fun main() {
    application {
        program {
            val sic0 = contour {
                moveTo(0.0, 0.0)
                lineTo(50.0, 0.0)
                lineTo(75.0, 25.0)
                lineTo(50.0, 25.0)
                lineTo(75.0, 0.0)
                lineTo(100.0, 0.0)
                lineTo(100.0, 100.0)
                lineTo(0.0, 100.0)
                close()

            }
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.translate(25.0, 25.0)
                drawer.contour(sic0)

                val ssu = sic0.shape.selfUnion()
                drawer.translate(0.0, 150.0)
                drawer.shape(ssu)
            }
        }
    }
}
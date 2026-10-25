import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.shape.Rectangle

fun main() {
    application {
        program {
            extend {
                val c = Rectangle(100.0, 100.0, 200.0, 200.0).contour

                drawer.lineJoin = LineJoin.ROUND
                drawer.strokeWeight = 10.0
                drawer.clear(ColorRGBa.WHITE)
                drawer.contour(c)

            }
        }
    }
}
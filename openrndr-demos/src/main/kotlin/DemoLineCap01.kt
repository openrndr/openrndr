import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap

fun main() = application {
    configure {
        height = 640
    }
    program {
        extend {
            drawer.clear(ColorRGBa.PINK)

            val weight = 50.0 // <-- change me

            drawer.translate(drawer.bounds.center)
            drawer.rotate(seconds * 30.0)
            drawer.translate(-drawer.bounds.center)

            drawer.stroke = ColorRGBa.WHITE

            drawer.lineSegment(50.0, -1000.0, 50.0, 2000.0)
            drawer.lineSegment(width - 50.0, -1000.0, width - 50.0, 2000.0)

            drawer.lineSegment(50.0 - weight / 2, -1000.0, 50.0 - weight / 2, 2000.0)
            drawer.lineSegment(width - 50.0 + weight / 2, -1000.0, width - 50.0 + weight / 2, 2000.0)

            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = weight

            drawer.lineCap = LineCap.ROUND
            drawer.lineSegment(50.0, height / 2.0 - 70.0, width - 50.0, height / 2.0 - 70.0)

            drawer.lineCap = LineCap.BUTT
            drawer.lineSegment(50.0, height / 2.0, width - 50.0, height / 2.0)

            drawer.lineCap = LineCap.SQUARE
            drawer.lineSegment(50.0, height / 2.0 + 70.0, width - 50.0, height / 2.0 + 70.0)
        }
    }
}

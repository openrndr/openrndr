import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.shape.contour

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            extend {
                drawer.translate(300.0, 0.0)
                for (i in 0 until 20) {
                    val c = contour {
                        moveOrLineTo(40.0, 40.0)
                        lineTo(100.0, 40.0)

                        lineTo(-100.0, 40.0 + i)
                    }
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.lineJoin = LineJoin.MITER
                    drawer.drawStyle.miterLimit = 100000.0
                    drawer.contour(c)
                    drawer.translate(0.0, 20.0)

                }
            }

        }
    }
}
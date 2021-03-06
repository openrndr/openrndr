import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.shape.Rectangle

fun main() {
    application {
        program {
            extend {

                drawer.clear(ColorRGBa.GRAY)
                drawer.fill = ColorRGBa.PINK
                drawer.stroke = ColorRGBa.BLACK

                drawer.isolated {
                    drawer.translate(20.0, 20.0)
                    drawer.contour(Rectangle(0.0, 0.0, 100.0, 100.0).contour)
                }

                drawer.isolated {
                    drawer.translate(140.0, 20.0)
                    drawer.shape(Rectangle(0.0, 0.0, 100.0, 100.0).shape)
                }



                drawer.isolated {
                    drawer.fill = null
                    drawer.translate(20.0, 140.0)
                    drawer.contour(Rectangle(0.0, 0.0, 100.0, 100.0).contour)
                }

                drawer.isolated {
                    drawer.stroke = null
                    drawer.translate(140.0, 140.0)
                    drawer.shape(Rectangle(0.0, 0.0, 100.0, 100.0).shape)
                }

                drawer.isolated {
                    drawer.strokeWeight = 0.0
                    drawer.translate(260.0, 140.0)
                    drawer.shape(Rectangle(0.0, 0.0, 100.0, 100.0).shape)
                }

                drawer.isolated {
                    drawer.strokeWeight = -10.0
                    drawer.translate(380.0, 140.0)
                    drawer.shape(Rectangle(0.0, 0.0, 100.0, 100.0).shape)
                }



            }
        }
    }
}
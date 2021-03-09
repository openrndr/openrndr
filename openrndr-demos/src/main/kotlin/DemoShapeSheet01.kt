import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 860
            height = 260
        }
        program {
            val font = loadFont(
                "https://github.com/IBM/plex/raw/master/IBM-Plex-Mono/fonts/complete/otf/IBMPlexMono-Text.otf",
                14.0
            )
            val rect = Rectangle(0.0, 0.0, 100.0, 100.0)

            extend {

                fun drawContour(x: Int, y: Int, s: String, pre: () -> Unit) {
                    drawer.isolated {
                        pre()
                        drawer.translate(20.0 + x * 120, 20.0 + y * 120)
                        drawer.contour(rect.contour)
                        drawer.fill = ColorRGBa.BLACK
                        drawer.text(s, 0.0, 111.0)
                    }
                }

                fun drawShape(x: Int, y: Int, s: String, pre: () -> Unit) {
                    drawer.isolated {
                        pre()
                        drawer.translate(20.0 + x * 120, 20.0 + y * 120)
                        drawer.shape(rect.shape)
                        drawer.fill = ColorRGBa.BLACK
                        drawer.text(s, 0.0, 111.0)
                    }
                }

                drawer.clear(ColorRGBa.GRAY)
                drawer.fill = ColorRGBa.PINK
                drawer.stroke = ColorRGBa.BLACK
                drawer.fontMap = font

                drawContour(0, 0, "contour") { }
                drawContour(1, 0, "null fill") { drawer.fill = null }
                drawContour(2, 0, "null stroke") { drawer.stroke = null }
                drawContour(3, 0, "0.0 stroke") { drawer.strokeWeight = 0.0 }
                drawContour(4, 0, "-10.0 stroke") { drawer.strokeWeight = -10.0 }
                drawContour(5, 0, "5.0 stroke") { drawer.strokeWeight = 5.0 }
                drawContour(6, 0, "10.0 stroke") { drawer.strokeWeight = 10.0 }

                drawShape(0, 1, "shape") { }
                drawShape(1, 1, "null fill") { drawer.fill = null }
                drawShape(2, 1, "null stroke") { drawer.stroke = null }
                drawShape(3, 1, "0.0 stroke") { drawer.strokeWeight = 0.0 }
                drawShape(4, 1, "-10.0 stroke") { drawer.strokeWeight = -10.0 }
                drawShape(5, 1, "5.0 stroke") { drawer.strokeWeight = 5.0 }
                drawShape(6, 1, "10.0 stroke") { drawer.strokeWeight = 10.0 }
            }
        }
    }
}
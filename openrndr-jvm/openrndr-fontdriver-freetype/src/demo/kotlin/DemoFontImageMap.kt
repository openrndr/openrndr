import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.loadFont

fun main() {
    application {
        program {

            val driverFreeType = FontDriverFreetype()
            FontDriver.driver = driverFreeType

            val face = loadFace("data/fonts/default.otf", 64.0, 1.0)
            val font = loadFont("data/fonts/default.otf", 64.0, contentScale = 2.0)

            extend {

                drawer.translate(0.0, 100.0)
                drawer.fontMap = font
                drawer.text("HELLO WORLD klhjg", 64.0, 64.0)
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineSegment(0.0, 64.5, width.toDouble(), 64.5)

                drawer.stroke = ColorRGBa.GREEN
                drawer.lineSegment(0.0, 64.5 - face.ascent, width.toDouble(), 64.5 - face.ascent)

                drawer.stroke = ColorRGBa.RED
                drawer.lineSegment(0.0, 64.5 - face.descent, width.toDouble(), 64.5 - face.descent)

                drawer.stroke = ColorRGBa.YELLOW
                drawer.lineSegment(0.0, 64.5 - face.xHeight, width.toDouble(), 64.5 - face.xHeight)


                drawer.stroke = ColorRGBa.MAGENTA
                drawer.lineSegment(0.0, 64.5 - face.capHeight, width.toDouble(), 64.5 - face.capHeight)



            }


        }
    }
}
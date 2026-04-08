import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.loadFont

fun main() {
    application {
        program {

            val driverFreeType = FontDriverFreetype()
            FontDriver.driver = driverFreeType

            val font = loadFont("data/fonts/Platypi-Regular.ttf", 64.0, contentScale = 2.0)
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fontMap = font
                drawer.text("Hello OPENRNDR", 64.0, 64.0)

            }
        }
    }
}
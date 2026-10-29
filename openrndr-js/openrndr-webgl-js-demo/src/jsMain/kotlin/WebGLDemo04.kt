import org.openrndr.ApplicationBuilder
import org.openrndr.annotations.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont

@Program("drawer.text")
fun ApplicationBuilder.webGLDemo04() {
    program {
        val platypi = loadFont("Platypi-Regular.ttf", 40.0)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fill = ColorRGBa.BLACK
            // the default font
            drawer.text("drawer.text with the default font", 20.0, 140.0)
            drawer.text("The quick brown fox jumps over the lazy dog, AV To Wa", 20.0, 170.0)
            drawer.fontMap = platypi
            drawer.fill = ColorRGBa.fromHex("#2040a0")
            drawer.text("A font loaded with loadFont, AV To Wa", 20.0, 240.0)
            drawer.fontMap = null
        }
    }
}

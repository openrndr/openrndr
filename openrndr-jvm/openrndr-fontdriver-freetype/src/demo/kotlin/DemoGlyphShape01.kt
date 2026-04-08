import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated

fun main() {
    application {
        program {

            val faceStb = loadFace("data/fonts/Platypi-Regular.ttf", 64.0, 1.0)
            val driverFreeType = FontDriverFreetype()

            FontDriver.driver = driverFreeType
            val faceFt = loadFace("data/fonts/Platypi-Regular.ttf", 64.0, 1.0)
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.translate(200.0, height / 2.0)

                drawer.isolated {
                    for (c in "OPENRNDR") {
                        val glyph = faceFt.glyphForCharacter(c)
                        val shape = glyph.shape()
                        drawer.shape(shape)
                        drawer.translate(glyph.advanceWidth(), 0.0)
                    }

                }
                drawer.translate(0.0, 200.0)
                drawer.isolated {
                    for (c in "OPENRNDR") {
                        val glyph = faceStb.glyphForCharacter(c)
                        val shape = glyph.shape()
                        drawer.shape(shape)
                        drawer.translate(glyph.advanceWidth(), 0.0)
                    }
                }

            }
        }
    }
}
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 1280
        }
        program {
            val driverFreeType = FontDriverFreetype()

            FontDriver.driver = driverFreeType
            val faceFt = loadFace("data/fonts/Platypi-Variable.ttf", 264.0, 1.0) as FaceFreetype

            println(faceFt.isVariable)
            println(faceFt.axes)
            for (a in faceFt.axes) {
                println(faceFt.axisRange(a))
                println(faceFt.getAxisValue(a))
            }
            extend {
                drawer.clear(ColorRGBa.PINK)

                for (i in 0 until 4) {
                    drawer.translate(200.0, height / 4.0)
                    faceFt.setAxisValue("Weight", 550.0 + cos(i + seconds * 10.0) * 250.0)

                    drawer.isolated {
                        for (c in "12345") {
                            val glyph = faceFt.glyphForCharacter(c)
                            val shape = glyph.shape()
                            drawer.shape(shape)
                            drawer.translate(glyph.advanceWidth(), 0.0)
                        }
                    }
                }
            }
        }
    }
}
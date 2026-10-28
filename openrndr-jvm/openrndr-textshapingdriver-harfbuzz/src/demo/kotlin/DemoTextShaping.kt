import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.isolated
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz

fun main() {
    application {
        program {

            val fontDriver = FontDriverFreetype()
            FontDriver.driver = fontDriver
            val face = fontDriver.loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf").withSize(64.0)


            val driver = TextShapingDriverHarfBuzz()
            val text = "오픈RNDR"
            val result = driver.shape(face, text)

            extend {
                drawer.clear(ColorRGBa.PINK)

                drawer.stroke = null
                drawer.translate(0.0, 200.0)

                for (i in 0 until result.size) {
                    val glyph = face.glyphForIndex(result[i].glyphIndex)
                    drawer.isolated {
                        drawer.translate(result[i].offset)
                        drawer.shape(glyph.shape())
                    }
                    drawer.translate(result[i].advance)
                }
            }
        }
    }
}
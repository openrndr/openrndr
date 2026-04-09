import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.isolated

fun main() {
    application {
        program {

            val fontDriver = FontDriverFreetype()
            FontDriver.driver = fontDriver
            val face = fontDriver.loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 64.0, RenderTarget.active.contentScale)

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
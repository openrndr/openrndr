import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {

            val face = loadFace("https://github.com/IBM/plex/raw/master/packages/plex-mono/fonts/complete/otf/IBMPlexMono-Bold.otf")
            extend {
                drawer.clear(ColorRGBa.PINK)
                val size = 200.0
                drawer.translate(200.0, height / 2.0)

                for (c in "OPENRNDR") {
                    val glyph = face.glyphForCharacter(c)
                    val shape = glyph.shape(size)
                    drawer.shape(shape)
                    drawer.translate(glyph.advanceWidth(size), 0.0)

                }

            }
        }
    }
}

import org.openrndr.application
import org.openrndr.draw.font.loadFace

fun main() {
    application {
        program {
            val face = loadFace("https://github.com/IBM/plex/raw/master/IBM-Plex-Mono/fonts/complete/otf/IBMPlexMono-Bold.otf")
            val size = 500.0
            val shape = face.glyphForCharacter('O').shape(size)
            extend {
                drawer.translate(width/2.0, height/2.0)
                drawer.shape(shape)
            }
        }
    }
}
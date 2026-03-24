import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.drawImage
import org.openrndr.resourceUrl

/**
 * Program written to solve an issue with creating two images
 * containing text and the second text not being correctly rendered.
 */
fun main() {
    application {
        program {
            val font = loadFont(resourceUrl("/fonts/IBMPlexMono-Regular.ttf"), 50.0)
            val image = drawImage(400, 100) {
                fontMap = font
                text("hello world!", 5.0, 50.0)
            }
            val image2 = drawImage(400, 100) {
                fontMap = font
                text("hello world!", 5.0, 50.0)
            }
            extend {
                drawer.image(image)
                drawer.translate(0.0, 110.0)
                drawer.image(image2)
            }
        }
    }
}
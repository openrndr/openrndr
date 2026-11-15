import org.openrndr.PresentationMode
import org.openrndr.application
import org.openrndr.color.ColorRGBa

/**
 * Draws a new frame for every mouse click, and when the window is exposed
 */
fun main() = application {
    program {
        window.presentationMode = PresentationMode.MANUAL
        mouse.buttonDown.listen {
            window.requestDraw()
        }

        extend {
            drawer.clear(ColorRGBa.PINK.shade(Math.random()))
        }
    }
}

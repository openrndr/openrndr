import org.openrndr.application
import org.openrndr.shape.Rectangle

/**
 * Draw a single rectangle
 */
fun main() {
    application {
        program {
            extend {
                drawer.rectangle(Rectangle.fromCenter(drawer.bounds.center, 100.0, 100.0))
            }
        }
    }
}
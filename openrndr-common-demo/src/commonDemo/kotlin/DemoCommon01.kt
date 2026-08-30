import org.openrndr.application

/**
 * Draw a single circle
 */
fun main() {
    application {
        program {
            extend {
                drawer.circle(drawer.bounds.center, 40.0)
            }
        }
    }
}
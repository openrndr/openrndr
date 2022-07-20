import org.openrndr.application

/**
 * Demonstrates querying and using displays.
 */
fun main() {
    application {
        configure {
            // First detected display, most likely your primary display.
            display = displays[0]
            width = 800
            height = 800
        }

        program {
            extend {}
        }
    }
}
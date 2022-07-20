import org.openrndr.application

/**
 * Demonstrates querying and using displays.
 */
fun main() {
    application {
        // Find the display with the largest resolution from the list of detected displays.
        val biggestDisplay = displays.maxByOrNull { it.width!! * it.height!! }!!

        configure {
            display = biggestDisplay
            width = 800
            height = 800
        }

        program {
            extend {}
        }
    }
}
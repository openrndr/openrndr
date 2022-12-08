import org.openrndr.KeyTracker
import org.openrndr.application

fun main() = application {
    program {
        val keyTracker = KeyTracker(keyboard)
        extend {
            drawer.translate(40.0, 40.0)
            for (key in keyTracker.pressedKeys) {
                drawer.rectangle(0.0, 0.0, 15.0, 15.0)
                drawer.translate(0.0, 20.0)
            }
        }
    }
}
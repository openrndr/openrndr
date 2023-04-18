import org.openrndr.MouseButton
import org.openrndr.MouseTracker
import org.openrndr.application

fun main() = application {
    program {
        val mouseTracker = MouseTracker(mouse)
        extend {
            if (MouseButton.LEFT in mouseTracker.pressedButtons) {
                drawer.rectangle(drawer.bounds.sub(0.1, 0.1, 0.2, 0.9))
            }

            if (MouseButton.CENTER in mouseTracker.pressedButtons) {
                drawer.rectangle(drawer.bounds.sub(0.45, 0.1, 0.55, 0.9))
            }

            if (MouseButton.RIGHT in mouseTracker.pressedButtons) {
                drawer.rectangle(drawer.bounds.sub(0.8, 0.1, 0.9, 0.9))
            }

        }
    }
}
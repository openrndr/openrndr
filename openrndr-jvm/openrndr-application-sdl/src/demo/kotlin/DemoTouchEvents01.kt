import org.openrndr.PointerTracker
import org.openrndr.application

fun main() {
    application {
        program {
            val pointerTracker = PointerTracker(pointers)
            extend {
                drawer.circles(pointerTracker.pointers.map { it.value.position }, 10.0)

            }
        }
    }
}
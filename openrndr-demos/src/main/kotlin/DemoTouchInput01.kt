import org.openrndr.application

fun main() {
    System.setProperty("org.openrndr.pointerevents", "true")
    application {
        program {
            extend {
                drawer.circles(pointers.pointers.map { it.position }, 20.0)
                drawer.circle(mouse.position, 10.0)
            }
        }
    }
}
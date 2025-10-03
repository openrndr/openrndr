import org.openrndr.application

fun main() {
    application {
        program {
            extend {
                drawer.circle(mouse.position, 40.0)
            }
        }
    }
}
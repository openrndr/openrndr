import org.openrndr.application

fun main() {
    application {
        program {
            extend {
                drawer.circle(100.0, 100.0, 40.0)
            }
        }
    }
}
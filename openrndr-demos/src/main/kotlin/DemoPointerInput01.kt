import org.openrndr.application

fun main() {
    application {
        program {
            extend {
                drawer.circles(pointers.pointers.map { it.position}, 20.0)
            }
        }
    }
}
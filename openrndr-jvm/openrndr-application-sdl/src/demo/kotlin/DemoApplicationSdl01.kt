import org.openrndr.application

fun main() {
    application {

        program {
            window.drop.listen {
                println(it.files)
            }

            extend {
                drawer.circle(mouse.position, 40.0)
            }
        }
    }
}
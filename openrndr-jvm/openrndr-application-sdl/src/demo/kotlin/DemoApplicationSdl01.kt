import org.openrndr.application

fun main() {
    application {

        program {
            window.drop.listen {
                println("files")
                println(it.files)
            }

            window.dropTexts.listen {
                println("texts")
                println(it.texts)
            }

            extend {
                drawer.circle(mouse.position, 40.0)
            }
        }
    }
}
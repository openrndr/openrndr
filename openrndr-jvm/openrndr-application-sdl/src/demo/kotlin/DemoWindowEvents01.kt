import org.openrndr.application

fun main() {
    application {
        configure {
            windowResizable = true
            hideWindowDecorations = false
        }

        program {

            window.sized.listen {
                println("window resized - $it")
            }

            window.maximized.listen {
                println("window maximized - $it")
            }

            window.restored.listen {
                println("window restored - $it")
            }

            window.minimized.listen {
                println("window minimized - $it")
            }

            extend {




            }
        }
    }
}
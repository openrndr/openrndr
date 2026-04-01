import org.openrndr.application
import org.openrndr.window

fun main() {
    application {
        configure {
            hideWindowDecorations = true
            windowResizable = true
        }

        program {

            window.maximized.listen {
                println("window maximized - $it")
            }

            window.restored.listen {
                println("window restored - $it")
            }

            window.minimized.listen {
                println("window minimized - $it")
            }

            var i = 0

            mouse.buttonUp.listen {
                when(i) {
                    0 -> window.maximize()
                    1 -> window.restore()
                    2 -> window.minimize()
                }
                i = (i + 1).mod(3)
            }

            extend {




            }
        }
    }
}
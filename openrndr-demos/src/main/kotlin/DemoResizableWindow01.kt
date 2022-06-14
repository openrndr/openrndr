import org.openrndr.application

/**
 * Demonstration of toggling the option to resize windows
 */
fun main() = application {
    configure {
        windowResizable = true
    }
    program {
        mouse.buttonDown.listen {
            window.resizable = !window.resizable
        }
    }
}

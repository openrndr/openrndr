import org.openrndr.Fullscreen
import org.openrndr.application

fun main() {
    application {
        configure {
            fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        }
        program {

        }
    }
}
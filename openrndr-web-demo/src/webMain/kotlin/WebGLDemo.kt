import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() {

    application {
        configure {
            canvasId = "openrndr-canvas-0"
        }
        program {
            extend {
                drawer.clear(ColorRGBa.RED)
                drawer.circle(mouse.position, 100.0)
            }
        }
    }

    application {
        configure {
            canvasId = "openrndr-canvas-1"
        }
        program {

            extend {
                drawer.clear(ColorRGBa.GREEN)
                drawer.circle(mouse.position, 100.0)
            }
        }
    }
}
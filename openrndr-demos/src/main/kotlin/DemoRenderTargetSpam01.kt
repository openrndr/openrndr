import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget

fun main() {
    application {
        program {
            extend {
                val rt = renderTarget(width, height) {
                    colorBuffer()
                    depthBuffer()
                }
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.PINK)
                    drawer.circle(drawer.bounds.center, 100.0)
                }

                drawer.image(rt.colorBuffer(0))

                rt.destroy()
            }
        }
    }
}
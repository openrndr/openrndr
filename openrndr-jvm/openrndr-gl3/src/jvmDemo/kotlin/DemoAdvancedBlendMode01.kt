import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode

fun main() {
    System.setProperty("org.openrndr.gl3.debug", "true")
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.RED)

                drawer.drawStyle.blendMode = BlendMode.COLOR_DODGE
                drawer.fill = ColorRGBa.BLUE

                drawer.circle(drawer.bounds.center, 100.0)
            }
        }
    }
}
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.arrayTexture

fun main() {
    application {
        configure {

        }
        program {
            val at = arrayTexture(512, 512, 16, type = ColorType.FLOAT32 )
            at.fill(ColorRGBa.PINK, 0, 0)
            at.fill(ColorRGBa.BLUE, 1, 0)
            extend {
                drawer.image(at, 1)
            }
        }
    }
}
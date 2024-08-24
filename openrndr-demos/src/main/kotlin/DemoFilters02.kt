import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.createEquivalent
import org.openrndr.drawImage
import org.openrndr.filter.color.linearize
import org.openrndr.shape.Rectangle

fun main() {
    application {
        program {
            val image = drawImage(width, height) {
                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = ColorRGBa.PINK
                drawer.circle(drawer.bounds.center, width/2.0)
            }

            val linearized = image.createEquivalent()
            linearized.fill(ColorRGBa.TRANSPARENT)

            extend {
                drawer.image(image)
                linearize.apply(image, linearized, Rectangle(100.0, 100.0, 100.0, 100.0))
                linearize.apply(image, linearized, Rectangle(300.0, 300.0, 100.0, 100.0))
                drawer.image(linearized)

            }
        }
    }
}
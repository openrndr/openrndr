import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.drawImage

/*
A quick demonstration of the Program.drawImage() function to create images by drawing. The drawImage() function is
intended to be used to create one-off images.
 */
fun main() {
    application {
        program {
            val image = drawImage(width / 2, height / 2) {
                clear(ColorRGBa.PINK)
                fill = ColorRGBa.WHITE
                drawer.circle(drawer.bounds.center, 100.0)
            }
            extend {
                drawer.image(image)
            }
        }
    }
}
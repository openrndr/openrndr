import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.drawImage
import org.openrndr.shape.IntRectangle

/**
 * Copies a color buffer into a grid of rectangles of a larger color buffer with [org.openrndr.draw.ColorBuffer.copyTo],
 * see https://github.com/openrndr/openrndr/issues/392
 */
fun main() = application {
    configure {
        width = 400
        height = 400
    }

    program {
        val buffer = colorBuffer(width, height).apply { fill(ColorRGBa.BLACK) }
        val test = drawImage(100, 100, contentScale = 1.0) {
            drawer.clear(ColorRGBa.PINK)
            drawer.circle(width / 2.0, 0.0, 40.0)
        }

        extend {
            for (j in 0 until 4) {
                for (i in 0 until 4) {
                    test.copyTo(
                        buffer,
                        sourceRectangle = IntRectangle(0, 0, test.width, test.height),
                        targetRectangle = IntRectangle(i * test.width, j * test.height, test.width, test.height),
                    )
                }
            }
            drawer.image(buffer)
        }
    }
}

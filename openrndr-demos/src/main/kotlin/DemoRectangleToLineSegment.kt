import org.openrndr.application
import org.openrndr.color.ColorRGBa
import kotlin.math.pow
import kotlin.math.sin
import org.openrndr.shape.Rectangle

/**
 * Demonstrates the use of [Rectangle.horizontal] and
 * [Rectangle.vertical].
 */
fun main() = application {
    program {
        val rect = drawer.bounds.offsetEdges(-180.0)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = null
            drawer.rectangle(rect)

            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 4.0

            when ((frameCount / 30) % 12) {
                // Draw `rect` edges.
                0 -> drawer.lineSegment(rect.horizontal(0.0))
                1 -> drawer.lineSegment(rect.vertical(0.0))
                2 -> drawer.lineSegment(rect.horizontal(1.0))
                3 -> drawer.lineSegment(rect.vertical(1.0))

                // Draw an animated vertical line sliding between
                // the left (0.0) and the right (1.0) edges of the rectangle.
                4, 5, 6 -> drawer.lineSegment(
                    rect.vertical(sin(seconds * 5) * 0.5 + 0.5)
                )

                // Shows the unconstrained nature of the argument.
                // By feeding values below 0.0 or above 1.0 we obtain
                // `LineSegment`s outside the rectangle.
                else -> repeat(8) {
                    val off = 0.1 + (it / 7.0).pow(1.6)
                    drawer.lineSegment(rect.horizontal(-off))
                    drawer.lineSegment(rect.horizontal(1 + off))
                }
            }
        }
    }
}
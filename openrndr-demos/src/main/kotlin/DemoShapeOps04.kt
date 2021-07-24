import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Circle
import org.openrndr.shape.union
import kotlin.math.cos

/**
 * A demonstration of complex shape unions.
 * The demo reveals high computation time variance and cases in which intersections are missed.
 */

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            val circles = List(18) { y ->
                List(18) { x ->
                    Circle(
                        (x + 1.5) * 40.0,
                        (y + 1.5) * 40.0,
                        cos(seconds * 0.1 + (x + y) * 0.1) * 40.0 + 40.0
                    ).shape
                }
            }.flatten()
            val result = circles.reduce { a, b -> a.union(b) }
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.shape(result)
        }
    }
}
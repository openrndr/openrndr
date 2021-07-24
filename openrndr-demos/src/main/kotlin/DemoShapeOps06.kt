import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.intersections
import org.openrndr.shape.union
import kotlin.math.cos

/**
 * A demonstration of complex shape unions.
 * The demo was used to debug some artefacts in the bezier polygon clipper
 */

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            val radius = 40.0+cos(seconds)*10.0
            // previously intersection misses would appear at radius = 44.944391
            val circles = List(18) { y ->
                List(18) { x ->
                    Circle(
                        (x + 1.5) * 40.0,
                        (y + 1.5) * 40.0,
                        radius

                    ).shape
                }
            }.flatten()

            var clipped = circles.first()
            val intersections = mutableListOf<Vector2>()
            for (circle in circles.drop(1)) {
                val localInts = clipped.intersections(circle).map { it.position }
                if (localInts.size < 2) {
                    println("intersection miss at $radius")
                }
                intersections.addAll(localInts)
                clipped = clipped.union(circle)
            }

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.shape(clipped)

            // draw accumulated intersection points
            drawer.fill = ColorRGBa.RED
            drawer.stroke = null
            drawer.circles(intersections, 3.0)
        }
    }
}
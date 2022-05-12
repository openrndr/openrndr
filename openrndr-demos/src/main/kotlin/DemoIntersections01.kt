import org.openrndr.application
import org.openrndr.shape.Circle
import org.openrndr.shape.intersections
import kotlin.math.cos

/**
 * A demonstration of complex shape unions.
 * The demo reveals high computation time variance and cases in which intersections are missed.
 */

fun main() = application {
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
                        cos(seconds * 0.1) * 10.0 + 40.0,
                    ).shape
                }
            }.flatten()
            for (i in circles.indices) {
                for (j in 0 until i) {
                    val ints =
                        circles[i].intersections(circles[j]).map { it.position }
                    drawer.circles(ints, 5.0)
                }
            }
        }
    }
}
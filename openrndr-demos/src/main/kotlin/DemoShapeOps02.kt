import org.openrndr.application
import org.openrndr.applicationSynchronous
import org.openrndr.draw.isolated
import org.openrndr.shape.Circle
import org.openrndr.shape.difference
import org.openrndr.shape.intersection
import org.openrndr.shape.union
import kotlin.math.cos

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            val d = cos(seconds)*50.0
            val c0 = Circle(-d, 0.0, 100.0).contour
            val c1 = Circle(d, 0.0, 100.0).contour
            for (y in 0 until 2) {
                for (x in 0 until 2) {
                    val case = y * 2 + x
                    drawer.isolated {
                        drawer.translate(width / 2.0 * x + width / 4.0, height / 2.0 * y + height / 4)
                        drawer.shape(
                            when (case) {
                                0 -> c0.intersection(c1.shape)
                                1 -> c0.union(c1.shape)
                                2 -> c0.difference(c1.shape)
                                3 -> c1.difference(c0.shape)
                                else -> error("no such case")
                            }
                        )
                    }
                }
            }
        }
    }
}
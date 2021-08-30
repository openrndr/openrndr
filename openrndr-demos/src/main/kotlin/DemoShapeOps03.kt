import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.math.Polar
import org.openrndr.shape.Circle
import org.openrndr.shape.difference
import org.openrndr.shape.intersection
import org.openrndr.shape.union
import kotlin.math.cos

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {

            val sides = 3
            val circles = List(sides) {
                Circle(Polar(it * 360/sides + seconds * 30.0, cos(seconds)*50.0).cartesian, 100.0).shape
            }
            for (y in 0 until 2) {
                for (x in 0 until 2) {
                    val case = y * 2 + x
                    drawer.isolated {
                        drawer.translate(width / 2.0 * x + width / 4.0, height / 2.0 * y + height / 4)
                        drawer.shape(
                            when (case) {
                                0 -> circles.reduce { a, b -> a.intersection(b) }
                                1 -> circles.reduce { a, b -> a.union(b) }
                                2 -> circles.reduce { a, b -> a.difference(b) }
                                3 -> circles.reduce { a, b -> b.difference(a) }
                                else -> error("no such case")
                            }
                        )
                    }
                }
            }
        }
    }
}
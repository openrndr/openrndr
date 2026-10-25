import org.openrndr.application
import org.openrndr.math.Polar
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.intersections
import org.openrndr.shape.removeSelfIntersections

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {


            val pentagram = ShapeContour.fromPoints(
                (0 until 5).map { i ->
                    drawer.bounds.center + Polar(-90.0 + i * 144.0, 200.0).cartesian
                },
                closed = true
            )

            val ints = pentagram.intersections(pentagram)

            println(pentagram.intersections(pentagram))
            val cleaned = pentagram.shape.removeSelfIntersections()


            val cints = cleaned.intersections(cleaned)
            //println(c.intersections(pentagram))

            extend {

                drawer.contour(pentagram)

                for (i in ints) {
                    drawer.circle(i.position, 5.0)
                }
                drawer.translate(200.0, 200.0)
                drawer.shape(cleaned)

                for (i in cints) {
                    drawer.circle(i.position, 5.0)

                }

            }

        }
    }
}
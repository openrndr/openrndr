import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.union

fun main() {
    application {
        program {

            val o = ShapeContour(
                (0 until 8).map {

                    val p0 = Polar((it * 2 * 360) / 16.0, 100.0).cartesian + drawer.bounds.center
                    val p1 = Polar(((it * 2 + 1) * 360) / 16.0, 200.0).cartesian + drawer.bounds.center
                    val p2 = Polar(((it * 2 + 2) * 360) / 16.0, 100.0).cartesian + drawer.bounds.center

                    if (it % 2 == 0)
                    Segment2D(p0, p1, p2) //.cubic
                    else
                        Segment2D(p0,  p2)
                }, true
            )

            val i = ShapeContour(
                (0 until 8).map {

                    val p0 = Polar(45.0 +(it * 2 * 360) / 16.0, 50.0).cartesian + drawer.bounds.center
                    val p1 = Polar(45.0 +((it * 2 + 1) * 360) / 16.0, 100.0).cartesian + drawer.bounds.center
                    val p2 = Polar(45.0 + ((it * 2 + 2) * 360) / 16.0, 50.0).cartesian + drawer.bounds.center

                    if (it % 2 == 0)
                        Segment2D(p0, p1, p2) //.cubic
                    else
                        Segment2D(p0,  p2)
                }, true
            ).reversed


            val s = Shape(listOf(o, i))


            extend {
                drawer.clear(ColorRGBa.PINK)
//                drawer.shape(s)

                val s2 = Rectangle.fromCenter(Vector2.ZERO, 10.0, 100.0).shape.transform(
                    buildTransform {
                        translate(mouse.position)
                        rotate(seconds * 45.0)
                    }

                )
                drawer.shape(s2.union(s))

            }
        }
    }
}
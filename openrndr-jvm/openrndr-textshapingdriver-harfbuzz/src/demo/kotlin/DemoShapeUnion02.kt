import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.font.internal.FontDriver
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
            val fontDriver = FontDriverFreetype()
            FontDriver.driver = fontDriver
            val face = fontDriver.loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 256.0, RenderTarget.active.contentScale)

            var os = face.glyphForCharacter('O').shape()
            os = os.transform(buildTransform {
                translate(drawer.bounds.center - os.bounds.center)
            })
            os = Shape(os.contours.map { it.sampleLinear() })
            val ogos = os
            os = Shape(os.contours.take(1))

            val co = Circle(drawer.bounds.center, 90.0).contour
            val ci =Circle(drawer.bounds.center, 45.0).contour.reversed

            val aos = Shape(listOf(ogos.contours.first(), ci))


            println(ogos.contours[0].winding)
            println(ogos.contours[1].winding)

            println("-----")

            println(aos.contours[0].winding)
            println(aos.contours[1].winding)

            val o = ShapeContour(
                (0 until 8).map {

                    val p0 = Polar((it * 2 * 360) / 16.0, 100.0).cartesian + drawer.bounds.center
                    val p1 = Polar(((it * 2 + 1) * 360) / 16.0, 200.0).cartesian + drawer.bounds.center
                    val p2 = Polar(((it * 2 + 2) * 360) / 16.0, 100.0).cartesian + drawer.bounds.center
                    Segment2D(p0, p1, p2)
                }, true
            )

            val i = Circle(drawer.bounds.center, 90.0).contour.reversed

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
                drawer.shape(s2.union(ogos))
                drawer.circle(ogos.contours[0].position(seconds.mod(1.0)),10.0)
                drawer.circle(ogos.contours[1].position(seconds.mod(1.0)),10.0)

                drawer.fill = null
//                drawer.contour(ogos.contours[0])
//                drawer.contour(ogos.contours[1])

            }
        }
    }
}
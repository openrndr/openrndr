package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.slug.SlugShapeDrawer
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.Ellipse
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
            program {


                val slugShapeDrawer = SlugShapeDrawer()


                val shape = Circle(0.0, 0.0, 8.0).contour.shape
                val shape2 = Ellipse(0.0, 0.0, 8.0, 4.0).contour.shape
                val shape3 = Rectangle.fromCenter(Vector2(0.0, 0.0), 8.0, 8.0).contour.shape
                val shape4 = Rectangle.fromCenter(Vector2(0.0, 0.0), 8.0, 4.0).contour.shape

                val c0 = Circle(0.0, 0.0, 8.0).contour
                val c1 = Ellipse(0.0, 0.0, 4.0, 1.0).contour.reversed

                val shape5 = Shape(listOf(c0, c1))

                val colors = List(10000) {
                    ColorRGBa(Random.nextDouble(), Random.nextDouble(), Random.nextDouble(), Random.nextDouble())
                }
                val shapes = listOf(shape, shape2, shape3, shape4, shape5)
                extend {

                    drawer.translate(drawer.bounds.center)
                    drawer.scale(1.0 + 3.0 * mouse.position.y / height)
                    drawer.translate(-drawer.bounds.center)

                    val seconds = seconds * 0.1
                    slugShapeDrawer.clear()
                    for (i in 0 until 10000) {
                        slugShapeDrawer.addShape(shapes[i.mod(shapes.size)], transform {
                            translate(
                                width / 2.0 + cos(seconds + i * 0.132) * width / 2.0,
                                sin(seconds + i) * height / 2.0 + height / 2.0
                            )
                            rotate(seconds * 1000.0 + i)
                            //scale(cos(seconds + i * 0.9432) * 1.0 + 3.0)
                        }, colors[i])
                    }
                    slugShapeDrawer.draw(drawer)
                }
            }
        }
    }
}
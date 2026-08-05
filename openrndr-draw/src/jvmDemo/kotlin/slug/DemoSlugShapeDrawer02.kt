package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.slug.SlugShapeDrawer
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.Ellipse
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.contour
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


                val c = contour {
                    moveTo(0.0, 0.0)
                    for (i in 0 until 100) {
                        val x = Random.nextDouble(0.0, drawer.bounds.width/10.0)
                        val y = Random.nextDouble(0.0, drawer.bounds.height/10.0)
                        lineTo(
                            x,
                            y
                        )

                    }
                }

                val shape = c.shape


                val colors = List(10000) {
                    ColorRGBa(Random.nextDouble(), Random.nextDouble(), Random.nextDouble(), Random.nextDouble())
                }
                val shapes = listOf(shape)
                extend {

                    drawer.translate(drawer.bounds.center)
                    drawer.scale(1.0 + 3.0 * mouse.position.y / height)
                    drawer.translate(-drawer.bounds.center)

                    val seconds = seconds * 0.1
                    slugShapeDrawer.clear()
                    for (i in 0 until 1) {
                        slugShapeDrawer.addShape(shapes[i.mod(shapes.size)], Matrix44.IDENTITY,ColorRGBa.TRANSPARENT, stroke = ColorRGBa.WHITE, strokeWeight = 3.5)
                    }
                    slugShapeDrawer.draw(drawer)
                }
            }
        }
    }
}
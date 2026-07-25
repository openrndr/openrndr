package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.clip

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            extend {

                val c = Circle(drawer.bounds.center,100.0).contour

                val m = Rectangle(mouse.position, 50.0, 50.0)


                drawer.rectangle(m)
                drawer.stroke = ColorRGBa.RED
                for (s in c.segments) {
                    drawer.segments(s.clip(m))
                }


            }
        }
    }
}
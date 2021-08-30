import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.union
import kotlin.math.cos

/**
 * A demonstration of complex shape unions.
 * This demonstrates perfect clipping for line based shapes
 */

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            drawer.clear(ColorRGBa.GRAY)
            val circles = List(18) { y ->
                List(18) { x ->
                    Rectangle.fromCenter(
                       Vector2.ZERO,
                        cos(seconds * 0.1 + (x + y) * 0.1) * 50.0 + 50.0,
                        cos(seconds * 0.1 + (x + y) * 0.1) * 50.0 + 50.0
                    ).shape.transform(buildTransform {
                        translate(Vector2((x + 1.5) * 40.0,
                            (y + 1.5) * 40.0))
                        rotate(Vector3.UNIT_Z, 45.0 + x*10.0 + y*10.0 + seconds * 30.0)
                    })
                }
            }.flatten()
            val result = circles.reduce { a, b -> a.union(b) }
            drawer.fill = ColorRGBa.BLACK
            //drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.shape(result)
        }
    }
}
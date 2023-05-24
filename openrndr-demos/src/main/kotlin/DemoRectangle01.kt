import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.stroke = ColorRGBa.GREEN.opacify(0.5)

            drawer.strokeWeight = 4.0
            drawer.model = buildTransform {
                translate(drawer.bounds.center)
                scale(1.0 + mouse.position.y / 100.0)
                rotate(Vector3.UNIT_Z, 10.0 * seconds)
                translate(-drawer.bounds.center)
            }
            drawer.rectangle(Rectangle.fromCenter(drawer.bounds.center, 200.0, 100.0))
        }
    }
}
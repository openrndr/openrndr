import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform

fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {

        val rt = renderTarget(width / 10, height / 10, contentScale = 10.0) {
            colorBuffer()
            depthBuffer()
        }

        extend {
            drawer.stroke = ColorRGBa.GREEN.opacify(0.5)
            drawer.strokeWeight = 4.0 / 10.0

            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.BLACK)
                drawer.ortho(rt)
                drawer.circle(drawer.bounds.center - Vector2(9.0, 0.0), 9.0)
                drawer.circle(drawer.bounds.center + Vector2(9.0, 0.0), 9.0)
            }

            drawer.strokeWeight = 4.0

            drawer.image(rt.colorBuffer(0), 0.0, 0.0, width * 1.0, height * 1.0)

            drawer.model = buildTransform {
                translate(drawer.bounds.center)
                scale(1.0 + mouse.position.y / 100.0)
                rotate(Vector3.UNIT_Z, 10.0 * seconds)
                translate(-drawer.bounds.center)
            }
            drawer.circle(drawer.bounds.center - Vector2(90.0, 0.0), 90.0)
            drawer.circle(drawer.bounds.center + Vector2(90.0, 0.0), 90.0)
        }
    }
}
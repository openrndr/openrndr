import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import kotlin.math.cos

/**
 * This demonstrates that drawing a circle using drawer.circle and drawer.contour results in equivalent output
 * while drawer.circle uses an entirely raster based method (which is much faster).
 */

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.model = buildTransform {
                translate(drawer.bounds.center)
                scale(1.0 + mouse.position.y / 100.0)
                rotate(Vector3.UNIT_Z, 10.0 * seconds)
                translate(-drawer.bounds.center)
            }

            drawer.stroke = ColorRGBa.GRAY

            val radius = 100.0
            val strokeWeight = 10.0 * cos(seconds) + 10.0

            // draw a simple grid to demonstrate where stroke and fill are expected
            drawer.lineSegment(
                drawer.bounds.center + Vector2(-100.0 - radius, -height.toDouble()),
                drawer.bounds.center + Vector2(-100.0 - radius, 2.0 * height.toDouble())
            )
            drawer.lineSegment(
                drawer.bounds.center + Vector2(-100.0 - radius - strokeWeight / 2.0, -height.toDouble()),
                drawer.bounds.center + Vector2(-100.0 - radius - strokeWeight / 2.0, 2.0 * height.toDouble())
            )
            drawer.lineSegment(
                drawer.bounds.center + Vector2(-100.0 - radius + strokeWeight / 2.0, -height.toDouble()),
                drawer.bounds.center + Vector2(-100.0 - radius + strokeWeight / 2.0, 2.0 * height.toDouble())
            )
            drawer.lineSegment(
                drawer.bounds.center + Vector2(100.0 + radius, -height.toDouble()),
                drawer.bounds.center + Vector2(100.0 + radius, 2.0 * height.toDouble())
            )
            drawer.lineSegment(
                drawer.bounds.center + Vector2(100.0 + radius + strokeWeight / 2.0, -height.toDouble()),
                drawer.bounds.center + Vector2(100.0 + radius + strokeWeight / 2.0, 2.0 * height.toDouble())
            )
            drawer.lineSegment(
                drawer.bounds.center + Vector2(100.0 + radius - strokeWeight / 2.0, -height.toDouble()),
                drawer.bounds.center + Vector2(100.0 + radius - strokeWeight / 2.0, 2.0 * height.toDouble())
            )

            drawer.strokeWeight = strokeWeight
            drawer.stroke = ColorRGBa.GREEN.opacify(0.5)

            drawer.circle(drawer.bounds.center - Vector2(100.0, 0.0), 100.0)
            val contour = Circle(drawer.bounds.center + Vector2(100.0, 0.0), 100.0).contour
            drawer.contour(contour)
        }
    }
}
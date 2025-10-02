import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.shape.Circle

fun main() {
    application {

        program {
            extend(Screenshots())
            val rt = renderTarget(width, height, multisample = BufferMultisample.SampleCount(4)) {
                colorBuffer()
                depthBuffer()
            }
            val resolved = colorBuffer(width, height)

            drawer.isolatedWithTarget(rt) {
                drawer.rectangle(drawer.bounds.offsetEdges(-200.0))
                drawer.circle(drawer.bounds.center, 100.0)
            }
            extend {
                rt.colorBuffer(0).copyTo(resolved)
                drawer.image(resolved)
                //resolved.read(target)
                drawer.fill = ColorRGBa.PINK
                drawer.contour(Circle(100.0, 100.0, 50.0).contour)
            }
        }
    }
}
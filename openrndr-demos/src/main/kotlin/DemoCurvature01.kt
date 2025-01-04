import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Ellipse
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 1000
            height = 1000
        }
        program {
            extend {
                drawer.clear(ColorRGBa.PINK)
                val c = Ellipse(drawer.bounds.center, 100.0, 200.0 + cos(seconds*0.1 * Math.PI*2.0)*100.0)
                val cc = c.contour
                drawer.stroke = ColorRGBa.BLACK.opacify(0.5)
                for (i in 0 until  72) {
                    val k = cc.curvature(i / 72.0)
                    val p = cc.position(i/72.0)
                    val n = cc.normal(i/72.0)
                    val r = 1.0 / k
                    drawer.fill = null
                    drawer.circle(p - n * r, r)
                }
            }
        }
    }
}
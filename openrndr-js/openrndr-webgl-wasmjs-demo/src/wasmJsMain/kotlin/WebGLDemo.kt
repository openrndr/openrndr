import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.cos

private val logger by lazy { KotlinLogging.logger { } }
fun main() {
    KotlinLoggingConfiguration.logLevel = Level.DEBUG

    application {
        configure {
            title = "openrndr-webgl-demo"
        }
        program {
            val rt = renderTarget(300, 300) {
                colorBuffer(format = ColorFormat.RGBa)
                depthBuffer()
            }
            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.clear(ColorRGBa.RED)
                drawer.fill = ColorRGBa.WHITE
                drawer.circle(150.0, 150.0, 30.0)
            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.circle(40.0, 40.0, 20.0 + cos(seconds) * 20.0 + 20.0)
                drawer.rectangle(80.0, 80.0, 200.0, 200.0)
                val c = Circle(100.0, 100.0, 40.0).contour
                drawer.contour(c)

                val c2 = Circle(200.0, 200.0, 100.0).contour
//                val c2 = Rectangle.fromCenter(Vector2(200.0, 200.0), 100.0, 100.0).contour
                for (i in 0 until 10) {
                    drawer.contour(c2.sub(0.0, cos(seconds + i) * 0.5 + 0.5))
                    drawer.translate(0.0, 10.0)
                }

                drawer.image(rt.colorBuffer(0), 300.0, 300.0)
            }
        }
    }
}
import org.openrndr.ApplicationBuilder
import org.openrndr.annotations.Program
import org.openrndr.color.ColorRGBa
import kotlin.math.cos

@Program("Just a circle")
fun ApplicationBuilder.webGLDemo01() {
    program {
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.circle(drawer.bounds.center, 20.0 + cos(seconds) * 20.0 + 20.0)
        }
    }
}

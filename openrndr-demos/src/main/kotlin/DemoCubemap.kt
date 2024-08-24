import org.openrndr.application
import org.openrndr.draw.ColorType
import org.openrndr.draw.cubemap

fun main() {
    application {
        program {
            val cubemap = cubemap(512)
        }
    }
}
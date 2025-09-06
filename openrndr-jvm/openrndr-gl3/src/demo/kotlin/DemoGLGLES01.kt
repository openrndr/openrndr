import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() {
    application {
        program {
            extend {
                val start = System.currentTimeMillis()
                drawer.clear(ColorRGBa.RED)
                for (i in 0 until 1000) {
                    drawer.circle(Math.random() * width, Math.random() * height, 40.0)
                }
                val end = System.currentTimeMillis()
                println("that took ${end - start}ms")
            }
        }
    }
}
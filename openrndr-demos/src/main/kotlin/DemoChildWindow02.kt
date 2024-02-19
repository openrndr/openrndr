import org.openrndr.WindowConfiguration
import org.openrndr.application
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.window

fun main() {
    application {
        program {
            for (j in 0 until 4) {
                for (i in 0 until 9) {
                    window(
                        WindowConfiguration(
                            title = "Child",
                            position = IntVector2(i * 200 + 60, j * 235 + 30),
                            width = 200,
                            height = 200
                        )
                    ) {
                        extend {
                            drawer.clear(ColorRGBa.BLACK)
                            drawer.fill = ColorHSVa(((j * 9 + i) / 36.0) * 360.0, 0.5, 0.5).toRGBa()
                            drawer.circle(drawer.bounds.center, 100.0)

                        }
                    }
                }
            }

            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.circle(drawer.bounds.center, 100.0)
            }
        }
    }
}
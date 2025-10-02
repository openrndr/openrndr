import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() {
    application {
        program {
            extend {

                drawer.scale(2.0 * mouse.position.y / height)


                drawer.strokeWeight = 20.0
                drawer.stroke = ColorRGBa.PINK

                drawer.lineStrip(listOf(drawer.bounds.position(0.0, 0.0), drawer.bounds.position(1.0, 1.0)))

                drawer.stroke = ColorRGBa.RED
                drawer.lineStrip(listOf(drawer.bounds.position(0.0, 1.0), drawer.bounds.position(1.0, 0.0)).map { it.vector3() })


            }
        }
    }
}
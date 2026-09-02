import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage

fun main() {
    application {
        program {
            // JVM only
//                        val cb = loadImage("src/commonMain/resources/images/cheeta.jpg")
            // JS & WASM/JS only
            val cb = loadImage("images/cheeta.jpg")
            // Multiplatform
//            val cb = loadImage("https://picsum.photos/id/1084/600/400")

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = ColorRGBa.fromHex("707070")
                drawer.image(cb, 25.0, 25.0)
                drawer.image(cb, 25.0, 340.0, 300.0, 200.0)
                drawer.image(cb, 25.0, 500.0, 150.0, 100.0)
                drawer.image(cb, 25.0, 580.0, 75.0, 50.0)
                drawer.image(cb, 25.0, 620.0, 37.5, 25.0)
            }
        }
    }
}
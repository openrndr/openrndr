import org.openrndr.ApplicationBuilder
import org.openrndr.annotations.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImageSuspend

@Program("Images")
fun ApplicationBuilder.webGLDemo05() {
    program {
        // browsers decode images asynchronously, images are loaded with loadImageSuspend
        val jpeg = loadImageSuspend("rgb-8_8_8.jpg")
        val png = loadImageSuspend("rgba-8_8_8_8.png")
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.image(jpeg, 20.0, 120.0, 320.0, 240.0)
            drawer.image(png, 360.0, 120.0, 320.0, 240.0)
            drawer.fill = ColorRGBa.BLACK
            drawer.text("rgb-8_8_8.jpg", 20.0, 385.0)
            drawer.text("rgba-8_8_8_8.png", 360.0, 385.0)
        }
    }
}

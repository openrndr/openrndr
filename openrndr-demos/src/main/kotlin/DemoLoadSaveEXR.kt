import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.loadImage
import org.openrndr.drawImage
import org.openrndr.math.Vector2
import java.io.File

/**
 * Generate an image, save it in EXR format, load it, and show side by side the generated and the loaded one.
 * Try also with `ColorFormat.RGB`.
 */
fun main() = application {
    program {
        val f = File("RGBW.exr")
        val img = drawImage(width / 2, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32) {
            stroke = ColorRGBa.PINK
            strokeWeight = 10.0
            lineSegment(Vector2.ZERO, bounds.position(1.0, 1.0))
            listOf(
                ColorRGBa.RED,
                ColorRGBa.GREEN,
                ColorRGBa.BLUE,
                ColorRGBa.WHITE,
                ColorRGBa.RED.opacify(0.5),
                ColorRGBa.GREEN.opacify(0.5),
                ColorRGBa.BLUE.opacify(0.5),
                ColorRGBa.WHITE.opacify(0.5)
            ).forEachIndexed { it, c ->
                stroke = null
                fill = c
                rectangle(bounds.sub(0.1, it * 0.125, 0.9, it * 0.125 + 0.125))
            }
        }
        img.saveToFile(f)
        val img2 = loadImage(f)
        f.delete()
        extend {
            drawer.clear(ColorRGBa.CYAN)
            drawer.image(img)
            drawer.translate(width / 2.0, 0.0)
            drawer.image(img2)
        }
    }
}
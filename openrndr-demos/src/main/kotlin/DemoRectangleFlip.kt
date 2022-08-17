import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.resourceUrl
import org.openrndr.shape.Rectangle

/**
 * Demonstrates the use of [Rectangle.flippedHorizontally] and
 * [Rectangle.flippedVertically] by:
 * - Creating a [RenderTarget].
 * - Drawing the word "mirror" into it.
 * - Drawing the `RenderTarget`'s [ColorBuffer] flipped horizontally and vertically.
 * Notice how calling the flip functions without arguments flips the Rectangle
 * in place.
 */
fun main() = application {
    program {
        val rt = renderTarget(300, 70) {
            colorBuffer()
        }
        val word = "mirror"
        drawer.isolatedWithTarget(rt) {
            clear(ColorRGBa.PINK)
            ortho(rt)
            fontMap = loadFont(
                resourceUrl("/fonts/IBMPlexMono-Regular.ttf"),
                100.0,
                word.toSet()
            )
            fill = ColorRGBa.WHITE
            text(word, 5.0, rt.height - 5.0)
        }

        val img = rt.colorBuffer(0)

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.translate(drawer.bounds.center)

            when (seconds.toInt() % 6) {
                0 -> drawer.image(img)
                1 -> drawer.image(
                    img,
                    img.bounds,
                    img.bounds.flippedHorizontally()
                )
                2 -> drawer.image(
                    img,
                    img.bounds,
                    img.bounds.flippedVertically()
                )
                3 -> drawer.image(
                    img,
                    img.bounds,
                    img.bounds.flippedHorizontally().flippedVertically()
                )
                else -> {
                    drawer.image(img)
                    drawer.image(
                        img,
                        img.bounds,
                        img.bounds.flippedVertically(0.0)
                            .flippedHorizontally(0.0)
                    )

                    drawer.drawStyle.colorMatrix = grayscale()
                    drawer.image(
                        img,
                        img.bounds,
                        img.bounds.flippedVertically(0.0)
                    )
                    drawer.image(
                        img,
                        img.bounds,
                        img.bounds.flippedHorizontally(0.0)
                    )
                }
            }
        }
    }
}
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.*
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that [Drawer.rawRectangle] covers exactly the pixels whose centers lie inside the rectangle, and that
 * `drawStyle.smooth = false` draws hard edges.
 */
class TestRawRectangleGL3 : AbstractApplicationTestFixture() {
    private val size = 128

    /** a rectangle with fractional edges, covering the pixel centers of columns 10..60 and rows 21..50 */
    private val rectangle = Rectangle(10.3, 20.6, 50.4, 30.2)

    private fun isCovered(x: Int, y: Int) = x in 10..60 && y in 21..50

    private val sentinel = ColorRGBa(1.0, 0.0, 1.0, 1.0, Linearity.LINEAR)

    private fun render(draw: Drawer.() -> Unit): List<List<ColorRGBa>> {
        val rt = renderTarget(size, size, contentScale = 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
            depthBuffer(DepthFormat.DEPTH24_STENCIL8)
        }
        val drawer = program.drawer
        drawer.isolatedWithTarget(rt) {
            drawer.ortho(rt)
            drawer.clear(sentinel)
            drawer.draw()
        }
        val shadow = rt.colorBuffer(0).shadow
        shadow.download()
        val pixels = (0 until size).map { y -> (0 until size).map { x -> shadow[x, y] } }
        rt.colorBuffer(0).destroy()
        rt.depthBuffer?.destroy()
        rt.destroy()
        return pixels
    }

    private fun close(a: ColorRGBa, b: ColorRGBa) =
        abs(a.r - b.r) < 1E-5 && abs(a.g - b.g) < 1E-5 && abs(a.b - b.b) < 1E-5 && abs(a.alpha - b.alpha) < 1E-5

    @Test
    fun rawRectangleWritesExactlyItsPixels() {
        val fill = ColorRGBa(0.25, 0.5, 0.75, 0.5, Linearity.LINEAR)
        // the output is premultiplied
        val expected = ColorRGBa(0.125, 0.25, 0.375, 0.5, Linearity.LINEAR)
        val pixels = render {
            drawStyle.blendMode = BlendMode.REPLACE
            this.fill = fill
            // the stroke is ignored by raw rectangles
            stroke = ColorRGBa.WHITE
            strokeWeight = 10.0
            rawRectangle(rectangle)
        }
        val failures = mutableListOf<String>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val want = if (isCovered(x, y)) expected else sentinel
                if (!close(pixels[y][x], want)) {
                    failures += "($x, $y): ${pixels[y][x]}, expected $want"
                }
            }
        }
        assertTrue(failures.isEmpty(), "${failures.size} wrong pixels:\n" + failures.take(10).joinToString("\n"))
    }

    @Test
    fun rawRectangleWithCustomOutput() {
        val pixels = render {
            drawStyle.blendMode = BlendMode.REPLACE
            shadeStyle = shadeStyle {
                suppressDefaultOutput = true
                output("color", ShadeStyleOutput(0))
                fragmentTransform = "o_color = vec4(c_boundsPosition.xy, 0.0, 1.0);"
            }
            rawRectangle(rectangle)
        }
        val failures = mutableListOf<String>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val p = pixels[y][x]
                if (isCovered(x, y)) {
                    // c_boundsPosition at the pixel center
                    val u = (x + 0.5 - rectangle.x) / rectangle.width
                    val v = (y + 0.5 - rectangle.y) / rectangle.height
                    if (abs(p.r - u) > 1E-3 || abs(p.g - v) > 1E-3 || p.alpha != 1.0) {
                        failures += "($x, $y): $p, expected bounds position ($u, $v)"
                    }
                } else if (!close(p, sentinel)) {
                    failures += "($x, $y): $p, expected the sentinel"
                }
            }
        }
        assertTrue(failures.isEmpty(), "${failures.size} wrong pixels:\n" + failures.take(10).joinToString("\n"))
    }

    @Test
    fun smoothFalseDrawsHardEdges() {
        for ((name, draw) in listOf<Pair<String, Drawer.() -> Unit>>(
            "rectangle" to { rotate(17.0); rectangle(40.0, 10.0, 50.0, 40.0) },
            "circle" to { circle(64.0, 64.0, 40.0) },
        )) {
            val pixels = render {
                drawStyle.smooth = false
                fill = ColorRGBa.WHITE
                stroke = ColorRGBa.BLACK
                strokeWeight = 6.0
                draw()
            }
            val partial = pixels.flatten().count { it.alpha > 1E-5 && it.alpha < 1.0 - 1E-5 }
            assertEquals(0, partial, "$name has $partial partially covered pixels")
        }

        // a hard edged disc covers about pi r^2 pixels, the outside of the circle is untouched
        val pixels = render {
            drawStyle.smooth = false
            fill = ColorRGBa.WHITE
            stroke = null
            circle(64.0, 64.0, 40.0)
        }
        val covered = pixels.flatten().count { close(it, ColorRGBa(1.0, 1.0, 1.0, 1.0, Linearity.LINEAR)) }
        val untouched = pixels.flatten().count { close(it, sentinel) }
        assertTrue(abs(covered - PI * 40.0 * 40.0) < 2 * PI * 40.0 * 0.1, "covered $covered pixels")
        assertEquals(size * size, covered + untouched)
    }
}

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawQuality
import org.openrndr.draw.Drawer
import org.openrndr.draw.arrayTexture
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadFont
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.resourceUrl
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Shade styles of all drawers read `v_objectPosition`, the position before the model matrix
 */
class TestObjectPositionGL3 : AbstractApplicationTestFixture() {
    private val width = 128
    private val height = 96

    /** the model matrix moves everything by this, object positions do not move with it */
    private val offset = Vector2(4.0, 20.0)

    /**
     * Draws with [draw] and a shade style that writes the object position, returns the object positions and
     * alphas of the pixels
     */
    private fun render(draw: Drawer.() -> Unit): (Int, Int) -> Pair<Vector2, Double> {
        val target = renderTarget(width, height, 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            translate(offset)
            stroke = ColorRGBa.WHITE
            fill = ColorRGBa.WHITE
            shadeStyle = shadeStyle {
                fragmentTransform = "x_fill = vec4(v_objectPosition.xy / 128.0, 0.0, 1.0); x_stroke = x_fill;"
            }
            draw()
        }
        val data = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        target.destroy()
        return { x, y ->
            val i = ((height - 1 - y) * width + x) * 16
            val alpha = data.getFloat(i + 12).toDouble()
            // the output is premultiplied by alpha
            Vector2(data.getFloat(i).toDouble(), data.getFloat(i + 4).toDouble()) * 128.0 / alpha.coerceAtLeast(1E-9) to alpha
        }
    }

    /** asserts that pixel ([x], [y]) holds the object position of its center */
    private fun assertObjectPosition(name: String, pixels: (Int, Int) -> Pair<Vector2, Double>, x: Int, y: Int, tolerance: Double = 0.05) {
        val (actual, alpha) = pixels(x, y)
        val expected = Vector2(x + 0.5, y + 0.5) - offset
        assertTrue(alpha > 0.0, "$name pixel ($x, $y) was not drawn")
        assertTrue(
            abs(actual.x - expected.x) < tolerance && abs(actual.y - expected.y) < tolerance,
            "$name pixel ($x, $y): expected $expected, was $actual"
        )
    }

    @Test
    fun `circles, rectangles and images`() {
        val image = colorBuffer(4, 4)
        val array = arrayTexture(4, 4, 2)
        val pixels = render {
            circle(30.0, 20.0, 10.0)
            rectangle(60.0, 10.0, 20.0, 20.0)
            image(image, 90.0, 10.0, 20.0, 20.0)
            image(array, 1, 10.0, 40.0, 20.0, 20.0)
        }
        assertObjectPosition("circle", pixels, 34, 40)
        assertObjectPosition("circle", pixels, 30, 36)
        assertObjectPosition("rectangle", pixels, 70, 35)
        assertObjectPosition("rectangle", pixels, 80, 45)
        assertObjectPosition("image", pixels, 100, 40)
        assertObjectPosition("image", pixels, 110, 48)
        assertObjectPosition("array texture image", pixels, 20, 66)
        image.destroy()
        array.destroy()
    }

    @Test
    fun `points and lines`() {
        val pixels = render {
            point(10.5, 10.5)
            drawStyle.quality = DrawQuality.PERFORMANCE
            lineSegment(Vector2(20.0, 5.5), Vector2(100.0, 5.5))
            drawStyle.quality = DrawQuality.QUALITY
            strokeWeight = 3.0
            lineSegment(Vector3(20.0, 50.5, 0.0), Vector3(100.0, 50.5, 0.0))
        }
        assertObjectPosition("point", pixels, 14, 30)
        assertObjectPosition("fast line", pixels, 50, 25)
        assertObjectPosition("fast line", pixels, 90, 25)
        assertObjectPosition("mesh line", pixels, 50, 70)
        assertObjectPosition("mesh line", pixels, 90, 70)
    }

    @Test
    fun `text`() {
        val font = program.loadFont(resourceUrl("/fonts/Roboto-Medium.ttf"), 32.0)
        val pixels = render {
            fontMap = font
            text("HHH", 10.0, 50.0)
        }
        // the glyphs cover pixels, those that are fully covered hold their object position
        var checked = 0
        for (y in 0 until height) for (x in 0 until width) {
            if (pixels(x, y).second > 0.999) {
                assertObjectPosition("text", pixels, x, y, tolerance = 0.5)
                checked++
            }
        }
        assertTrue(checked > 20, "only $checked pixels of the text were covered")
    }
}

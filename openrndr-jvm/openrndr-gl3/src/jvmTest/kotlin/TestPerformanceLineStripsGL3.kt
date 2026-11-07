import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.DrawQuality
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.ShapeContour
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Line loops drawn with [DrawQuality.PERFORMANCE] are closed, line strips are open unless their closed flag is set
 */
class TestPerformanceLineStripsGL3 : AbstractApplicationTestFixture() {
    private val width = 64
    private val height = 64

    /** the square (10.5, 10.5)-(50.5, 50.5), its closing edge is the left edge at x = 10.5 */
    private val square = listOf(Vector2(10.5, 10.5), Vector2(50.5, 10.5), Vector2(50.5, 50.5), Vector2(10.5, 50.5))

    /** draws with [draw] and returns whether the left and the bottom edge of the square are drawn */
    private fun edges(draw: Drawer.() -> Unit): Pair<Boolean, Boolean> {
        val target = renderTarget(width, height, 1.0) {
            colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
            depthBuffer()
        }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            drawStyle.quality = DrawQuality.PERFORMANCE
            stroke = ColorRGBa.WHITE
            draw()
        }
        val data = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        target.destroy()
        fun lit(x: Int, y: Int) = data.getFloat(((height - 1 - y) * width + x) * 16 + 12) > 0.5f
        return lit(10, 30) to lit(30, 50)
    }

    @Test
    fun `loops are closed and strips are open`() {
        val square3 = square.map { it.xy0 }
        assertEquals(true to true, edges { lineLoop(square) }, "lineLoop 2d")
        assertEquals(true to true, edges { lineLoop(square3) }, "lineLoop 3d")
        assertEquals(true to true, edges { lineLoops(listOf(square)) }, "lineLoops 2d")
        assertEquals(true to true, edges { lineLoops(listOf(square3)) }, "lineLoops 3d")
        assertEquals(false to true, edges { lineStrip(square) }, "lineStrip 2d")
        assertEquals(false to true, edges { lineStrip(square3) }, "lineStrip 3d")
        assertEquals(false to true, edges { lineStrips(listOf(square)) }, "lineStrips 2d")
        assertEquals(false to true, edges { lineStrips(listOf(square3)) }, "lineStrips 3d")
        assertEquals(true to true, edges { lineStrips(listOf(square3), closed = listOf(true)) }, "closed lineStrips 3d")
        assertEquals(false to true, edges { lineStrips(listOf(square3), closed = listOf(false)) }, "open lineStrips 3d")
        assertEquals(true to true, edges { contour(ShapeContour.fromPoints(square, closed = true)) }, "closed contour")
        assertEquals(false to true, edges { contour(ShapeContour.fromPoints(square, closed = false)) }, "open contour")
    }

    @Test
    fun `strips with more points than a small buffer draw all segments`() {
        // 2 * 99 vertices, more than the 128 vertices of the small buffers
        val strip = List(100) { Vector3(10.5, 10.5 + it * 40.0 / 99.0, 0.0) }
        assertEquals(true, edges { lineStrips(listOf(strip), closed = listOf(false)) }.first)
    }

    @Test
    fun `vertex buffer shade styles read object positions`() {
        val geometry = vertexBuffer(vertexFormat { position(3) }, 6)
        geometry.put {
            for (p in listOf(Vector2(0.0, 0.0), Vector2(40.0, 0.0), Vector2(40.0, 20.0),
                Vector2(0.0, 0.0), Vector2(40.0, 20.0), Vector2(0.0, 20.0))) write(p.xy0)
        }
        val target = renderTarget(width, height, 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            // the model matrix moves the geometry, object positions do not move with it
            translate(10.0, 20.0)
            shadeStyle = shadeStyle { fragmentTransform = "x_fill = vec4(v_objectPosition.xy / 64.0, 0.0, 1.0);" }
            vertexBuffer(geometry, DrawPrimitive.TRIANGLES)
        }
        val data = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        for ((x, y) in listOf(12 to 22, 30 to 30, 48 to 38)) {
            val i = ((height - 1 - y) * width + x) * 16
            val actual = Vector2(data.getFloat(i).toDouble(), data.getFloat(i + 4).toDouble()) * 64.0
            val expected = Vector2(x + 0.5 - 10.0, y + 0.5 - 20.0)
            assertTrue(abs(actual.x - expected.x) < 0.05 && abs(actual.y - expected.y) < 0.05,
                "pixel ($x, $y): expected $expected, was $actual")
        }
        target.destroy()
        geometry.destroy()
    }
}

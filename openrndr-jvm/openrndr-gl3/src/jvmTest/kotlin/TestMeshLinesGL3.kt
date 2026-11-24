import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.DrawQuality
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.draw.LineWidthMode
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 3D lines drawn with [DrawQuality.QUALITY]: caps, joins, miter limits, anti-aliasing, single coverage of joins and
 * clipping by the near plane
 */
class TestMeshLinesGL3 : AbstractApplicationTestFixture() {
    private val size = 64

    /** the pixels drawn by [draw], rows from the top */
    private class Pixels(val data: ByteBuffer, val size: Int) {
        fun channel(x: Int, y: Int, c: Int) = data.getFloat(((size - 1 - y) * size + x) * 16 + c * 4)
        fun alpha(x: Int, y: Int) = channel(x, y, 3)
        fun all() = (0 until size).flatMap { y -> (0 until size).flatMap { x -> (0 until 4).map { channel(x, y, it) } } }
    }

    private fun draw(stencil: Boolean = false, samples: Int = 0, draw: Drawer.() -> Unit): Pixels {
        val multisample = if (samples > 1) BufferMultisample.SampleCount(samples) else BufferMultisample.Disabled
        val target = renderTarget(size, size, 1.0, multisample) {
            colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
            if (stencil) depthBuffer()
        }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            drawStyle.quality = DrawQuality.QUALITY
            stroke = ColorRGBa.WHITE
            draw()
        }
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        val resolved = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        target.colorBuffer(0).copyTo(resolved)
        resolved.read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        resolved.destroy()
        target.destroy()
        return Pixels(data, size)
    }

    private fun strip(vararg points: Pair<Double, Double>) = points.map { Vector3(it.first, it.second, 0.0) }

    private fun assertAlpha(expected: Double, actual: Float, message: String, tolerance: Double = 0.02) =
        assertTrue(abs(expected - actual) <= tolerance, "$message: expected alpha $expected, was $actual")

    @Test
    fun `caps`() {
        val line = strip(10.0 to 32.0, 54.0 to 32.0)
        for ((cap, covered) in listOf(LineCap.BUTT to listOf(false, false), LineCap.ROUND to listOf(true, false), LineCap.SQUARE to listOf(true, true))) {
            val pixels = draw {
                strokeWeight = 10.0
                lineCap = cap
                lineStrip(line)
            }
            assertAlpha(1.0, pixels.alpha(11, 32), "$cap inside")
            assertAlpha(1.0, pixels.alpha(32, 36), "$cap side")
            assertAlpha(0.0, pixels.alpha(32, 38), "$cap outside")
            // 2.5 pixels before the start, and 3.5 pixels above the line 4.5 pixels before the start
            assertAlpha(if (covered[0]) 1.0 else 0.0, pixels.alpha(7, 32), "$cap before the start")
            assertAlpha(if (covered[1]) 1.0 else 0.0, pixels.alpha(5, 28), "$cap corner before the start")
        }
    }

    @Test
    fun `edges are anti-aliased`() {
        val pixels = draw {
            strokeWeight = 10.5
            lineStrip(strip(10.0 to 32.0, 54.0 to 32.0))
        }
        // the edge lies at y = 37.25, the pixel center at 37.5
        assertAlpha(0.25, pixels.alpha(32, 37), "lower edge")
        assertAlpha(0.25, pixels.alpha(32, 26), "upper edge")
        assertAlpha(1.0, pixels.alpha(32, 36), "inside")
    }

    @Test
    fun `edges are anti-aliased by multisampling without smooth`() {
        val pixels = draw(samples = 4) {
            drawStyle.smooth = false
            strokeWeight = 10.5
            lineStrip(strip(10.0 to 32.0, 54.0 to 32.0))
        }
        // the edges lie a quarter pixel into the pixels, some of their samples are covered
        for ((y, name) in listOf(37 to "lower edge", 26 to "upper edge")) {
            val alpha = pixels.alpha(32, y)
            assertTrue(alpha > 0.05f && alpha < 0.95f, "$name: expected partial coverage, was $alpha")
        }
        assertAlpha(1.0, pixels.alpha(32, 36), "inside")
    }

    @Test
    fun `joins and miter limits`() {
        val corner = strip(10.0 to 10.0, 40.0 to 10.0, 40.0 to 40.0)
        fun pixels(join: LineJoin, limit: Double = 4.0) = draw {
            strokeWeight = 10.0
            lineJoin = join
            miterLimit = limit
            lineStrip(corner)
        }
        // the miter reaches the outer corner (45, 5)
        val miter = pixels(LineJoin.MITER)
        assertAlpha(1.0, miter.alpha(44, 5), "miter tip")
        assertAlpha(1.0, miter.alpha(43, 7), "miter")
        val bevel = pixels(LineJoin.BEVEL)
        assertAlpha(0.0, bevel.alpha(44, 5), "bevel tip")
        assertAlpha(0.0, bevel.alpha(43, 6), "bevel")
        assertAlpha(1.0, bevel.alpha(42, 8), "bevel inside")
        val round = pixels(LineJoin.ROUND)
        assertAlpha(0.0, round.alpha(44, 5), "round tip")
        assertAlpha(1.0, round.alpha(43, 7), "round")
        // the miter ratio of a right angle is sqrt(2)
        val limited = pixels(LineJoin.MITER, 1.2)
        assertAlpha(0.0, limited.alpha(43, 6), "miter over the limit")
        val clipped = pixels(LineJoin.MITER_CLIP, 1.2)
        assertAlpha(1.0, clipped.alpha(43, 7), "clipped miter")
        assertTrue(clipped.alpha(44, 5) < 0.5f, "clipped miter tip: ${clipped.alpha(44, 5)}")
        for (p in listOf(miter, bevel, round, limited, clipped)) {
            assertAlpha(1.0, p.alpha(40, 10), "joint")
            assertAlpha(1.0, p.alpha(36, 14), "inner corner")
        }
    }

    @Test
    fun `joins are drawn once`() {
        for (join in LineJoin.entries) {
            val pixels = draw {
                stroke = ColorRGBa.WHITE.opacify(0.5)
                strokeWeight = 10.0
                lineJoin = join
                lineStrips(listOf(strip(10.0 to 10.0, 40.0 to 10.0, 40.0 to 40.0, 20.0 to 30.0)), closed = listOf(false))
            }
            val max = (0 until size).maxOf { y -> (0 until size).maxOf { x -> pixels.alpha(x, y) } }
            assertTrue(max <= 0.501f, "$join: alpha $max, pixels are drawn more than once")
            assertAlpha(0.5, pixels.alpha(40, 10), "$join joint")
            assertAlpha(0.5, pixels.alpha(36, 14), "$join inner corner")
        }
    }

    @Test
    fun `closed strips are joined at their start`() {
        val square = strip(16.0 to 16.0, 48.0 to 16.0, 48.0 to 48.0, 16.0 to 48.0)
        val closed = draw {
            strokeWeight = 6.0
            lineStrips(listOf(square), closed = listOf(true))
        }
        val open = draw {
            strokeWeight = 6.0
            lineStrips(listOf(square), closed = listOf(false))
        }
        assertAlpha(1.0, closed.alpha(14, 14), "closed corner")
        assertAlpha(1.0, closed.alpha(14, 32), "closed side")
        assertAlpha(0.0, open.alpha(14, 14), "open corner")
        assertAlpha(0.0, open.alpha(14, 32), "open side")
        assertAlpha(1.0, open.alpha(50, 50), "open corner")
    }

    @Test
    fun `lines thinner than a pixel fade`() {
        val pixels = draw {
            strokeWeight = 0.5
            lineStrip(strip(10.0 to 32.5, 54.0 to 32.5))
        }
        assertAlpha(0.5, pixels.alpha(32, 32), "thin line")
    }

    @Test
    fun `shade styles read texture coordinates and positions of the corners`() {
        val pixels = draw {
            strokeWeight = 10.0
            shadeStyle = shadeStyle {
                fragmentTransform = "x_stroke = vec4(va_texCoord0.y, va_position.x / 64.0, va_texCoord0.x, 1.0);"
            }
            lineStrip(strip(10.0 to 32.0, 54.0 to 32.0))
        }
        assertAlpha((32.5 - 10.0) / 44.0, pixels.channel(32, 32, 0), "arc length", 0.01)
        assertAlpha(32.5 / 64.0, pixels.channel(32, 32, 1), "position", 0.01)
        // across the line from 0 to 1 against the y-down drawer coordinates, like the texture coordinates of before
        assertAlpha(0.5 - 0.5 * 3.5 / 5.0, pixels.channel(32, 35, 2), "across", 0.02)
    }

    @Test
    fun `segments are clipped by the near plane`() {
        val pixels = draw {
            perspective(90.0, 1.0, 0.1, 100.0)
            view = Matrix44.IDENTITY
            model = Matrix44.IDENTITY
            strokeWeight = 2.0
            // from in front of the camera to behind it, beside the camera
            lineStrip(listOf(Vector3(-1.0, 0.0, -5.0), Vector3(-1.0, 0.0, 5.0)))
        }
        assertTrue(pixels.all().all { it.isFinite() }, "finite pixels")
        assertTrue(maxOf(pixels.alpha(12, 31), pixels.alpha(12, 32)) > 0.5f, "the line in front of the camera")
        assertEquals(0f, maxOf(pixels.alpha(40, 31), pixels.alpha(40, 32)), "nothing beyond the far end")
    }

    @Test
    fun `lines that cross are blended once with lineBlendOnce`() {
        // two strips and a strip that crosses itself
        val strips = listOf(
            strip(10.0 to 32.0, 54.0 to 32.0),
            strip(32.0 to 10.0, 32.0 to 54.0),
            strip(10.0 to 10.0, 54.0 to 54.0, 54.0 to 10.0, 10.0 to 54.0)
        )
        fun maxAlpha(blendOnce: Boolean, stencil: Boolean): Float {
            val pixels = draw(stencil) {
                stroke = ColorRGBa.WHITE.opacify(0.5)
                strokeWeight = 6.0
                drawStyle.lineBlendOnce = blendOnce
                lineStrips(strips, closed = listOf(false, false, false))
            }
            return (0 until size).maxOf { y -> (0 until size).maxOf { x -> pixels.alpha(x, y) } }
        }
        assertTrue(maxAlpha(blendOnce = true, stencil = true) <= 0.501f, "blended once")
        assertTrue(maxAlpha(blendOnce = false, stencil = true) > 0.6f, "blended where the lines cross")
        // without a stencil buffer lines are blended where they cross
        assertTrue(maxAlpha(blendOnce = true, stencil = false) > 0.6f, "blended without a stencil buffer")

        // the stencil is reset, a second draw is blended again
        val twice = draw(stencil = true) {
            stroke = ColorRGBa.WHITE.opacify(0.5)
            strokeWeight = 6.0
            drawStyle.lineBlendOnce = true
            lineStrip(strip(10.0 to 32.0, 54.0 to 32.0))
            lineStrip(strip(10.0 to 32.0, 54.0 to 32.0))
        }
        assertAlpha(0.75, twice.alpha(32, 32), "two draws")
    }

    @Test
    fun `screen widths do not depend on the distance or the model scale`() {
        fun rows(mode: LineWidthMode, z: Double, scale: Double): Int {
            val pixels = draw {
                perspective(90.0, 1.0, 0.1, 1000.0)
                view = Matrix44.IDENTITY
                model = buildTransform { scale(scale) }
                strokeWeight = 6.0
                drawStyle.lineWidthMode = mode
                lineStrip(listOf(Vector3(-0.5 * z / scale, 0.0, -z / scale), Vector3(0.5 * z / scale, 0.0, -z / scale)))
            }
            return (0 until size).count { y -> pixels.alpha(32, y) > 0.5f }
        }
        for ((z, scale) in listOf(1.0 to 1.0, 50.0 to 1.0, 500.0 to 1.0, 50.0 to 4.0)) {
            assertEquals(6, rows(LineWidthMode.SCREEN, z, scale), "screen width at z = $z, scale $scale")
        }
        // perspective widths get thinner with distance
        assertEquals(6, rows(LineWidthMode.PERSPECTIVE, 1.0, 1.0), "perspective width at z = 1")
        assertEquals(0, rows(LineWidthMode.PERSPECTIVE, 500.0, 1.0), "perspective width at z = 500")
    }
}

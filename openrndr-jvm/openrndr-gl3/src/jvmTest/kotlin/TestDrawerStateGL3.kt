import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.rotate
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.LineSegment
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

/**
 * The state of the drawer: its transforms, context, style stack and batch builders, and line segments drawn with
 * [DrawQuality.PERFORMANCE]
 */
class TestDrawerStateGL3 : AbstractApplicationTestFixture() {

    @Test
    fun `transforms multiply the matrices like Matrix44 does`() {
        val drawer = program.drawer
        drawer.isolated {
            model = Matrix44.IDENTITY
            var expected = Matrix44.IDENTITY
            translate(3.0, -4.0)
            expected *= Matrix44.translate(3.0, -4.0, 0.0)
            assertEquals(expected, model)
            rotate(33.0)
            expected *= Matrix44.rotateZ(33.0)
            assertEquals(expected, model)
            scale(1.5, -2.0)
            expected *= Matrix44.scale(1.5, -2.0, 1.0)
            assertEquals(expected, model)
            translate(Vector3(1.0, 2.0, 3.0))
            expected *= Matrix44.translate(1.0, 2.0, 3.0)
            assertEquals(expected, model)
            scale(0.25)
            expected *= Matrix44.scale(0.25, 0.25, 0.25)
            assertEquals(expected, model)
            rotate(Vector3.UNIT_X, 12.0)
            expected *= Matrix44.rotate(Vector3.UNIT_X, 12.0)
            assertEquals(expected, model)

            view = Matrix44.IDENTITY
            translate(5.0, 6.0, target = TransformTarget.VIEW)
            assertEquals(Matrix44.translate(5.0, 6.0, 0.0), view)
        }
    }

    @Test
    fun `the context follows the matrices and the render target`() {
        val drawer = program.drawer
        val target = renderTarget(32, 16) { colorBuffer() }
        drawer.isolated {
            val before = context
            assertSame(before, context, "an unchanged context is reused")
            translate(10.0, 0.0)
            assertEquals(model, context.model)
            projection = Matrix44.scale(2.0, 2.0, 2.0)
            assertEquals(projection, context.projection)
            scale(2.0)
            // the scaling factor is the length of the model view transform of the unit diagonal
            assertEquals(2.0, context.modelViewScalingFactor, 1E-12)
            isolatedWithTarget(target) {
                assertEquals(32, context.width)
                assertEquals(16, context.height)
                assertEquals(32.0, bounds.width)
            }
            assertEquals(RenderTarget.active.width, context.width)
        }
        target.destroy()
    }

    @Test
    fun `stencil changes inside isolated do not leak`() {
        val drawer = program.drawer
        drawer.isolated {
            drawStyle.stencil.stencilTest = StencilTest.NEVER
            drawStyle.stencil.stencilWriteMask = 0x0f
        }
        assertEquals(StencilTest.DISABLED, drawer.drawStyle.stencil.stencilTest)
        assertEquals(0xff, drawer.drawStyle.stencil.stencilWriteMask)
    }

    @Test
    fun `batch builders keep their entries and can be nested`() {
        val drawer = program.drawer
        drawer.isolated {
            fill = ColorRGBa.RED
            circles {
                circle(1.0, 2.0, 3.0)
                stroke = null
                circle(Vector2(4.0, 5.0), 6.0)
                // a batch drawn while building a batch
                drawer.circles { circle(0.0, 0.0, 1.0); assertEquals(1, entries.size) }
                assertEquals(2, entries.size)
                assertEquals(Vector3(4.0, 5.0, 0.0), entries[1].offset)
                assertEquals(Vector2(6.0, 6.0), entries[1].radius)
                assertNull(entries[1].stroke)
                assertEquals(ColorRGBa.RED.toLinear(), entries[0].fill)
            }
            // the builder of the drawer starts empty
            circles { assertEquals(0, entries.size) }
            rectangles {
                rectangle(1.0, 2.0, 3.0, 4.0, 45.0)
                assertEquals(45.0, entries[0].rotation)
                assertEquals(Vector2(3.0, 4.0), entries[0].dimensions)
            }
            points {
                point(1.0, 2.0, 3.0)
                assertEquals(Vector3(1.0, 2.0, 3.0), entries[0].offset)
            }
        }
    }

    @Test
    fun `performance line segments are drawn segment by segment`() {
        val width = 64
        val height = 64
        val target = renderTarget(width, height, 1.0) { colorBuffer() }
        val drawer = program.drawer
        drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.BLACK)
            stroke = ColorRGBa.WHITE
            drawStyle.quality = DrawQuality.PERFORMANCE
            lineSegments(emptyList<LineSegment>())
            // two disjoint horizontal segments, as a chain of their points the first segment would not be drawn
            lineSegments(listOf(LineSegment(Vector2(4.0, 10.5), Vector2(60.0, 10.5)), LineSegment(Vector2(4.0, 50.5), Vector2(60.0, 50.5))))
        }
        val data = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data)
        fun red(x: Int, y: Int) = data.get(((height - 1 - y) * width + x) * 4).toInt() and 0xff
        assertTrue(red(32, 10) > 128, "the first segment is drawn")
        assertTrue(red(32, 50) > 128, "the second segment is drawn")
        assertTrue(red(32, 30) < 32, "nothing is drawn between the segments")
        target.destroy()
    }
}

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Copies and clears are not cut by the scissor of the clip of an earlier draw style
 */
class TestScissorGL3 : AbstractApplicationTestFixture() {
    private val size = 32

    private fun colors(colorBuffer: org.openrndr.draw.ColorBuffer): Set<List<Float>> {
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        colorBuffer.read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        return (0 until size * size).map { i -> List(4) { data.getFloat(i * 16 + it * 4) } }.toSet()
    }

    @Test
    fun `multisample resolves and clears ignore the clip of an earlier draw style`() {
        val target = renderTarget(size, size, 1.0, BufferMultisample.SampleCount(4)) {
            colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
            depthBuffer()
        }
        val resolved = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        resolved.fill(ColorRGBa.BLUE)
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.RED)
            // the clip is restored, the scissor test stays until the next draw
            isolated {
                drawStyle.clip = Rectangle(4.0, 4.0, 8.0, 8.0)
                fill = ColorRGBa.WHITE
                rectangle(0.0, 0.0, 32.0, 32.0)
            }
        }
        target.colorBuffer(0).copyTo(resolved, 0, 0, MagnifyingFilter.NEAREST)
        assertEquals(setOf(listOf(1f, 0f, 0f, 1f), listOf(1f, 1f, 1f, 1f)), colors(resolved), "resolved colors")

        program.drawer.isolatedWithTarget(target) {
            isolated {
                drawStyle.clip = Rectangle(4.0, 4.0, 8.0, 8.0)
                rectangle(0.0, 0.0, 32.0, 32.0)
            }
            target.clearColor(0, ColorRGBa.GREEN)
            target.clearDepth()
        }
        target.colorBuffer(0).copyTo(resolved, 0, 0, MagnifyingFilter.NEAREST)
        assertEquals(setOf(listOf(0f, 1f, 0f, 1f)), colors(resolved), "cleared colors")
        resolved.destroy()
        target.destroy()
    }
}

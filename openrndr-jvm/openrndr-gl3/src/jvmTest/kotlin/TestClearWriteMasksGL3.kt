import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ChannelMask
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Clears are not masked by the color write mask of an earlier draw style, and leave the masks of the draw style intact
 */
class TestClearWriteMasksGL3 : AbstractApplicationTestFixture() {
    private val noChannels = ChannelMask(red = false, green = false, blue = false, alpha = false)

    @Test
    fun `clears ignore the write mask of an earlier draw style`() {
        val target = renderTarget(16, 16, 1.0) {
            colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
            depthBuffer()
        }
        val data = ByteBuffer.allocateDirect(16 * 16 * 16).order(ByteOrder.nativeOrder())
        fun color(): List<Float> {
            target.colorBuffer(0).read(data, ColorFormat.RGBa, ColorType.FLOAT32)
            return List(4) { data.getFloat(it * 4) }
        }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.RED)
            // a draw with all channels masked leaves the color mask off after the style is restored
            isolated {
                drawStyle.channelWriteMask = noChannels
                fill = ColorRGBa.WHITE
                rectangle(0.0, 0.0, 16.0, 16.0)
            }
            clear(ColorRGBa.GREEN)
        }
        assertEquals(listOf(0f, 1f, 0f, 1f), color(), "drawer.clear")

        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            isolated {
                drawStyle.channelWriteMask = noChannels
                fill = ColorRGBa.WHITE
                rectangle(0.0, 0.0, 16.0, 16.0)
            }
            target.clearColor(0, ColorRGBa.BLUE)
        }
        assertEquals(listOf(0f, 0f, 1f, 1f), color(), "RenderTarget.clearColor")

        // after a clear, the masks of the draw style still apply
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            drawStyle.channelWriteMask = noChannels
            fill = ColorRGBa.WHITE
            rectangle(0.0, 0.0, 16.0, 16.0)
            clear(ColorRGBa.RED)
            rectangle(0.0, 0.0, 16.0, 16.0)
        }
        assertEquals(listOf(1f, 0f, 0f, 1f), color(), "masked draw after clear")
        target.destroy()
    }
}

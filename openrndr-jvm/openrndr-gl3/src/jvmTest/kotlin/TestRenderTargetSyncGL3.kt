import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ShadeStyleFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Render targets are used as textures right after they are drawn into, without waiting for the GPU: every pass adds
 * 1/1024 to the red channel of the previous result, a pass that reads stale data gives a smaller sum
 */
class TestRenderTargetSyncGL3 : AbstractApplicationTestFixture() {
    private val size = 256
    private val passes = 200
    private val step = 1.0 / 1024.0
    private val add = shadeStyle { fragmentTransform = "x_fill = vec4(x_fill.r + $step, 0.0, 0.0, 1.0);" }

    private fun red(buffer: ColorBuffer): Double {
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        buffer.read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        return data.getFloat((128 * size + 128) * 16).toDouble()
    }

    private fun buffer() = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).apply { fill(ColorRGBa.BLACK) }

    @Test
    fun filtersReadTheirInputsRightAfterTheyAreDrawn() {
        val filter = ShadeStyleFilter(add)
        var a = buffer()
        var b = buffer()
        repeat(passes) { filter.apply(a, b); val t = a; a = b; b = t }
        assertEquals(passes * step, red(a), 1E-6)
        a.destroy(); b.destroy()
    }

    @Test
    fun renderTargetsAreSampledRightAfterTheyAreDrawn() {
        val first = renderTarget(size, size, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        val second = renderTarget(size, size, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(first) { clear(ColorRGBa.BLACK) }
        var source = first
        var target = second
        repeat(passes) {
            program.drawer.isolatedWithTarget(target) {
                ortho(target); clear(ColorRGBa.TRANSPARENT); shadeStyle = add
                image(source.colorBuffer(0))
            }
            val t = source; source = target; target = t
        }
        assertEquals(passes * step, red(source.colorBuffer(0)), 1E-6)
        first.destroy(); second.destroy()
    }

    @Test
    fun multisampledTargetsAreResolvedRightAfterTheyAreDrawn() {
        val multisampled = renderTarget(size, size, 1.0, multisample = BufferMultisample.SampleCount(4)) {
            colorBuffer(type = ColorType.FLOAT32)
        }
        val resolved = buffer()
        val next = buffer()
        repeat(passes / 4) {
            program.drawer.isolatedWithTarget(multisampled) {
                ortho(multisampled); clear(ColorRGBa.TRANSPARENT); shadeStyle = add
                image(resolved)
            }
            multisampled.colorBuffer(0).copyTo(next)
            next.copyTo(resolved)
        }
        assertEquals((passes / 4) * step, red(resolved), 1E-6)
        multisampled.destroy(); resolved.destroy(); next.destroy()
    }

    @Test
    fun renderTargetsCanBeDestroyedRightAfterTheyAreUsed() {
        var current = buffer()
        repeat(passes) {
            val target = renderTarget(size, size, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
            program.drawer.isolatedWithTarget(target) {
                ortho(target); clear(ColorRGBa.TRANSPARENT); shadeStyle = add
                image(current)
            }
            val result = target.colorBuffer(0)
            target.detachColorAttachments()
            target.destroy()
            current.destroy()
            current = result
        }
        assertEquals(passes * step, red(current), 1E-6)
        current.destroy()
    }
}

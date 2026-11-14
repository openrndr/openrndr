import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.ColorBufferGL3
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
import org.openrndr.math.Matrix55
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.*

/**
 * State that DriverGL3 and its resources keep track of instead of querying GL
 */
class TestDriverStateGL3 : AbstractApplicationTestFixture() {
    private fun redOf(target: RenderTarget): Double {
        val buffer = ByteBuffer.allocateDirect(4 * 4 * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(buffer, ColorFormat.RGBa, ColorType.FLOAT32)
        return buffer.getFloat(0).toDouble()
    }

    @Test
    fun `render targets that are attached to and detached from many times keep one blend mode per attachment`() {
        val target = renderTarget(4, 4, 1.0) {}
        val buffer = colorBuffer(4, 4, type = ColorType.FLOAT32)
        repeat(16) {
            target.attach(buffer)
            target.detachColorAttachments()
        }
        target.attach(buffer)
        assertEquals(1, target.blendModes.size)
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.BLACK)
            fill = ColorRGBa.WHITE
            stroke = null
            rectangle(-1.0, -1.0, 6.0, 6.0)
        }
        assertEquals(1.0, redOf(target), 1E-3)
        target.detachColorAttachments()
        target.destroy()
        buffer.destroy()
    }

    @Test
    fun `functions queued for the main thread by functions that run on it run on the next call`() {
        val driver = Driver.instance as DriverGL3
        val ran = mutableListOf<String>()
        driver.executeOnMainThread {
            ran += "first"
            driver.executeOnMainThread { ran += "queued by first" }
        }
        driver.processMainThreadExecutables()
        assertEquals(listOf("first"), ran)
        driver.processMainThreadExecutables()
        assertEquals(listOf("first", "queued by first"), ran)
    }

    @Test
    fun `functions after one that throws stay queued`() {
        val driver = Driver.instance as DriverGL3
        val ran = mutableListOf<String>()
        driver.executeOnMainThread { error("failed") }
        driver.executeOnMainThread { ran += "after" }
        assertFailsWith<IllegalStateException> { driver.processMainThreadExecutables() }
        driver.processMainThreadExecutables()
        assertEquals(listOf("after"), ran)
    }

    @Test
    fun `the color matrix of images is uploaded when it changes`() {
        val image = colorBuffer(4, 4, type = ColorType.FLOAT32).apply { fill(ColorRGBa.WHITE) }
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        fun drawWith(colorMatrix: Matrix55): Double {
            program.drawer.isolatedWithTarget(target) {
                ortho(target)
                clear(ColorRGBa.BLACK)
                drawStyle.colorMatrix = colorMatrix
                image(image, 0.0, 0.0, 4.0, 4.0)
            }
            return redOf(target)
        }
        val noRed = Matrix55.IDENTITY.copy(c0r0 = 0.0)
        assertEquals(1.0, drawWith(Matrix55.IDENTITY), 1E-3)
        assertEquals(0.0, drawWith(noRed), 1E-3)
        assertEquals(0.0, drawWith(noRed.copy()), 1E-3)
        assertEquals(1.0, drawWith(Matrix55.IDENTITY), 1E-3)
        image.destroy()
        target.destroy()
    }

    @Test
    fun `texture parameters can be read back, also the wrap modes`() {
        val buffer = colorBuffer(4, 4)
        buffer.wrapU = WrapMode.REPEAT
        buffer.wrapV = WrapMode.MIRRORED_REPEAT
        buffer.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
        assertEquals(WrapMode.REPEAT, buffer.wrapU)
        assertEquals(WrapMode.MIRRORED_REPEAT, buffer.wrapV)
        assertEquals(MinifyingFilter.NEAREST, buffer.filterMin)
        assertEquals(MagnifyingFilter.NEAREST, buffer.filterMag)

        // a color buffer that was not set reads its parameters from GL
        val fresh = colorBuffer(4, 4)
        val queried = fresh.wrapU
        assertTrue(queried in WrapMode.entries)
        buffer.destroy()
        fresh.destroy()
    }

    @Test
    fun `binding a color buffer keeps the texture bindings of the driver right`() {
        // a shade style samples a texture on unit 0 after another color buffer was bound there to change it
        val red = colorBuffer(4, 4, type = ColorType.FLOAT32).apply { fill(ColorRGBa.RED) }
        val blue = colorBuffer(4, 4, type = ColorType.FLOAT32).apply { fill(ColorRGBa.BLUE) }
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.BLACK)
            image(red, 0.0, 0.0, 4.0, 4.0)
            // binds blue to unit 0 to set its filter
            (blue as ColorBufferGL3).filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)
            image(red, 0.0, 0.0, 4.0, 4.0)
        }
        assertEquals(1.0, redOf(target), 1E-3)
        listOf(red, blue).forEach { it.destroy() }
        target.destroy()
    }

    @Test
    fun `reading a color buffer restores the pack alignment`() {
        val glType = Driver.glType
        fun packAlignment(): Int = if (glType == DriverTypeGL.GL) {
            org.lwjgl.opengl.GL33C.glGetInteger(0x0D05)
        } else {
            org.lwjgl.opengles.GLES30.glGetInteger(0x0D05)
        }
        fun setPackAlignment(value: Int) = if (glType == DriverTypeGL.GL) {
            org.lwjgl.opengl.GL33C.glPixelStorei(0x0D05, value)
        } else {
            org.lwjgl.opengles.GLES30.glPixelStorei(0x0D05, value)
        }
        setPackAlignment(4)
        val buffer = colorBuffer(4, 4)
        val data = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder())
        buffer.read(data)
        if (glType == DriverTypeGL.GL) {
            assertEquals(4, packAlignment())
        }
        buffer.destroy()
    }

    @Test
    fun `copies between color buffers work many times in a row and leave the active target bound`() {
        val source = colorBuffer(8, 8, type = ColorType.FLOAT32).apply { fill(ColorRGBa(0.25, 0.0, 0.0, 1.0)) }
        val targets = List(3) { colorBuffer(8, 8, type = ColorType.FLOAT32) }
        val msaa = renderTarget(8, 8, 1.0, BufferMultisample.SampleCount(4)) { colorBuffer(type = ColorType.FLOAT32) }
        val active = RenderTarget.active
        repeat(20) {
            for (target in targets) {
                source.copyTo(target)
            }
            msaa.colorBuffer(0).copyTo(targets[0])
        }
        assertSame(active, RenderTarget.active)
        val data = ByteBuffer.allocateDirect(8 * 8 * 16).order(ByteOrder.nativeOrder())
        targets[1].read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        assertTrue(abs(data.getFloat(0) - 0.25f) < 1E-3)
        (targets + source).forEach { it.destroy() }
        msaa.destroy()
    }
}

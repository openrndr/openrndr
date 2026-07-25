import org.junit.jupiter.api.Assertions.assertEquals
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test

class TestColorBufferGL3 : AbstractApplicationTestFixture() {

    /**
     * Tests the functionality of copying the contents of one `ColorBuffer` to another when both buffers
     * use the default format (RGBa).
     *
     * This test creates two `ColorBuffer` instances with identical dimensions and the default format.
     * It verifies the `copyTo` method by copying the contents of the first buffer to the second buffer.
     * After the operation, both buffers are properly released to ensure resource cleanup.
     */
    @Test
    fun testCopyRGBaToRGBa() {
        val a = colorBuffer(256, 256)
        val b = colorBuffer(256, 256)
        a.copyTo(b)
        a.close()
        b.close()
    }

    /**
     * Tests the functionality of copying the contents of one `ColorBuffer` to another
     * when both buffers use the RGB color format.
     *
     * This test performs the following steps:
     * - Creates two `ColorBuffer` instances with identical dimensions and the RGB format.
     * - Verifies the `copyTo` method by copying the contents of the first buffer to the second buffer.
     * - Ensures the resources are properly released by closing both buffers after the operation.
     *
     * The test is only executed if the underlying graphical driver is of type `DriverTypeGL.GL`.
     */
    @Test
    fun testCopyRGBToRGB() {
        if (Driver.glType == DriverTypeGL.GL) {
            val a = colorBuffer(256, 256, format = ColorFormat.RGB)
            val b = colorBuffer(256, 256, format = ColorFormat.RGB)
            a.copyTo(b)
            a.close()
            b.close()
        }
    }

    /**
     * Tests the functionality of copying data from a `ColorBuffer` with RGB color format
     * to a `ColorBuffer` with RGBa color format.
     *
     * This test performs the following steps:
     * - Creates an RGB `ColorBuffer` and an RGBa `ColorBuffer` with identical dimensions.
     * - Verifies the `copyTo` method by copying data from the RGB buffer to the RGBa buffer.
     * - Closes both buffers after the operation to ensure resource cleanup.
     */
    @Test
    fun testCopyRGBtoRGBa() {
            val a = colorBuffer(256, 256, format = ColorFormat.RGB)
            val b = colorBuffer(256, 256, format = ColorFormat.RGBa)
            a.copyTo(b)
            a.close()
            b.close()
    }

    @Test
    fun testIntFormat() {
        val c = colorBuffer(256, 256, format = ColorFormat.R, type = ColorType.UINT32_INT)
        val buffer = ByteBuffer.allocateDirect(256 * 256 * 4)
        buffer.order(ByteOrder.nativeOrder())
        for (i in 0 until 256 * 256) {
            buffer.putInt(i)
        }
        buffer.rewind()
        c.writeBuffer(buffer)
        buffer.rewind()
        for (i in 0 until 256 * 256) {
            assertEquals(i, buffer.getInt())
        }
        c.read(buffer)
    }

    @Test
    fun testInt16Format() {
        val c = colorBuffer(256, 256, format = ColorFormat.R, type = ColorType.UINT16_INT)
        val buffer = ByteBuffer.allocateDirect(256 * 256 *2)
        buffer.order(ByteOrder.nativeOrder())
        for (i in 0 until 256 * 256) {
            buffer.putShort(i.toShort())
        }
        buffer.rewind()
        c.writeBuffer(buffer)
        buffer.rewind()
        for (i in 0 until 256 * 256) {
            assertEquals(i.toUShort(), buffer.getShort().toUShort())
        }
        c.read(buffer)
    }

    @Test
    fun testIntFormatMPP() {
        val c = colorBuffer(256, 256, format = ColorFormat.R, type = ColorType.UINT32_INT)
        val buffer = MPPBuffer.allocate(256 * 256 * 4)
        for (i in 0 until 256 * 256) {
            buffer.putInt(i)
        }
        buffer.rewind()
        c.write(buffer, x = 0, y = 0, width = 256, height = 256, level = 0)
        buffer.rewind()
        for (i in 0 until 256 * 256) {
            assertEquals(i, buffer.byteBuffer.getInt())
        }
        c.read(buffer.byteBuffer)
    }


}
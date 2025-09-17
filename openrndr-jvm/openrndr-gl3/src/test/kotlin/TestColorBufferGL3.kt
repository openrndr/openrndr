import org.openrndr.draw.ColorFormat
import org.openrndr.draw.colorBuffer
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
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
}
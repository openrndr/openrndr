import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.test.Test

class TestColorBufferWebGL : AbstractApplicationTestFixture() {

    @Test
    fun testColorBuffer() {
        val formats = listOf(ColorFormat.RGBa, ColorFormat.RG, ColorFormat.R)
        val types = listOf(ColorType.UINT8, ColorType.FLOAT16, ColorType.FLOAT32)

        for (format in formats) {
            for (type in types) {
                val cb = colorBuffer(100, 100, 1.0, format, type)
                cb.fill(ColorRGBa.BLACK)
                cb.destroy()
            }
        }
    }

    @Test
    fun testColorBufferWrite() {
        val buffer = MPPBuffer.allocate(100 * 100 * 4)
//        for (i in 0 until 100 * 100) {
//            buffer.put(100)
//            buffer.put(100)
//            buffer.put(100)
//            buffer.put(255.toByte())
//
//        }
//        buffer.rewind()
        val cb = colorBuffer(100, 100)
        cb.write(buffer)
        cb.destroy()
    }

}
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.internal.gl3.floatToHalf
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestColorBufferReadGL3 : AbstractApplicationTestFixture() {

    private fun buffer(size: Int) = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())

    /** a 2x2 FLOAT16 color buffer holding [values], RGBA for each pixel */
    private fun float16ColorBuffer(values: FloatArray) = colorBuffer(2, 2, format = ColorFormat.RGBa, type = ColorType.FLOAT16).apply {
        val source = buffer(values.size * 4)
        values.forEachIndexed { index, value -> source.putFloat(index * 4, value) }
        writeBuffer(source, ColorFormat.RGBa, ColorType.FLOAT32)
    }

    private val floatValues = floatArrayOf(
        0.25f, 0.5f, 2.0f, 1.0f,
        -1.5f, 0.0f, 4.0f, 0.75f,
        0.125f, 1.0f, 0.375f, 1.0f,
        8.0f, 0.0625f, 0.5f, 0.0f
    )

    @Test
    fun `floatToHalf matches the JDK`() {
        val random = Random(1)
        val values = listOf(
            0.0f, -0.0f, 1.0f, -1.0f, 0.5f, 65504.0f, 65520.0f, 1e-8f, 6.0e-8f, 3.0e-5f, 6.1e-5f,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.MIN_VALUE, Float.MAX_VALUE
        ) + List(100_000) { Float.fromBits(random.nextInt()) }.filter { !it.isNaN() }
        for (value in values) {
            assertEquals(java.lang.Float.floatToFloat16(value), floatToHalf(value), "converting $value")
        }
        assertEquals(true, java.lang.Float.float16ToFloat(floatToHalf(Float.NaN)).isNaN())
    }

    @Test
    fun `read FLOAT16 as FLOAT16`() {
        val colorBuffer = float16ColorBuffer(floatValues)
        val target = buffer(floatValues.size * 2)
        colorBuffer.read(target, ColorFormat.RGBa, ColorType.FLOAT16)
        val read = FloatArray(floatValues.size) { java.lang.Float.float16ToFloat(target.getShort(it * 2)) }
        assertEquals(floatValues.toList(), read.toList())
        colorBuffer.destroy()
    }

    @Test
    fun `read FLOAT16 as FLOAT32`() {
        val colorBuffer = float16ColorBuffer(floatValues)
        val target = buffer(floatValues.size * 4)
        colorBuffer.read(target, ColorFormat.RGBa, ColorType.FLOAT32)
        val read = FloatArray(floatValues.size) { target.getFloat(it * 4) }
        assertEquals(floatValues.toList(), read.toList())
        colorBuffer.destroy()
    }

    @Test
    fun `read FLOAT16 as UINT8`() {
        val colorBuffer = float16ColorBuffer(floatValues)
        val target = buffer(floatValues.size)
        colorBuffer.read(target, ColorFormat.RGBa, ColorType.UINT8)
        val read = List(floatValues.size) { target.get(it).toInt() and 0xff }
        val expected = floatValues.map { (it.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt() }
        assertEquals(expected, read)
        colorBuffer.destroy()
    }

    @Test
    fun `read UINT8 as FLOAT32 and R`() {
        val bytes = (0 until 16).map { it * 16 }
        val colorBuffer = colorBuffer(2, 2, format = ColorFormat.RGBa, type = ColorType.UINT8).apply {
            val source = buffer(bytes.size)
            bytes.forEachIndexed { index, value -> source.put(index, value.toByte()) }
            writeBuffer(source)
        }

        val floats = buffer(bytes.size * 4)
        colorBuffer.read(floats, ColorFormat.RGBa, ColorType.FLOAT32)
        assertEquals(bytes.map { it / 255.0f }, List(bytes.size) { floats.getFloat(it * 4) })

        val reds = buffer(4)
        colorBuffer.read(reds, ColorFormat.R, ColorType.UINT8)
        assertEquals(bytes.filterIndexed { index, _ -> index % 4 == 0 }, List(4) { reds.get(it).toInt() and 0xff })
        colorBuffer.destroy()
    }

    @Test
    fun `read a mipmap level`() {
        val colorBuffer = colorBuffer(4, 4, format = ColorFormat.RGBa, type = ColorType.FLOAT16, levels = 2)
        val level1 = buffer(4 * 4 * 4)
        (0 until 16).forEach { level1.putFloat(it * 4, it.toFloat()) }
        colorBuffer.writeBuffer(level1, ColorFormat.RGBa, ColorType.FLOAT32, level = 1)

        val target = buffer(2 * 2 * 4 * 4)
        colorBuffer.read(target, ColorFormat.RGBa, ColorType.FLOAT32, level = 1)
        assertEquals((0 until 16).map { it.toFloat() }, List(16) { target.getFloat(it * 4) })
        colorBuffer.destroy()
    }

    @Test
    fun `read fills the buffer from its start`() {
        val colorBuffer = colorBuffer(16, 16, format = ColorFormat.R, type = ColorType.UINT32_INT)
        val source = buffer(16 * 16 * 4)
        (0 until 16 * 16).forEach { source.putInt(it * 4, it) }
        colorBuffer.writeBuffer(source)

        // a buffer that was just written or read through has its position at its end
        val target = buffer(16 * 16 * 4)
        target.position(target.capacity())
        colorBuffer.read(target)
        assertEquals((0 until 16 * 16).toList(), List(16 * 16) { target.getInt(it * 4) })
        colorBuffer.destroy()
    }

    @Test
    fun `read into a buffer that is too small fails`() {
        val colorBuffer = colorBuffer(16, 16, format = ColorFormat.RGBa, type = ColorType.UINT8)
        assertFailsWith<IllegalArgumentException> {
            colorBuffer.read(buffer(16 * 16 * 4 - 1), ColorFormat.RGBa, ColorType.UINT8)
        }
        colorBuffer.destroy()
    }
}

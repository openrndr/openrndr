import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.DriverVersionGL
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.internal.gl3.glType
import org.openrndr.internal.gl3.glVersion
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that writes at element offsets end up at those offsets
 * (https://github.com/openrndr/openrndr/issues/310). Every buffer holds 20 floats, initialized to -1, five values
 * are written at element 10.
 */
class TestBufferOffsetsGL3 : AbstractApplicationTestFixture() {
    private val bufferSize = 20
    private val offset = 10
    private val values = listOf(0f, 1f, 2f, 3f, 4f)

    /** the expected contents after writing [values] at [offset] */
    private val expected = List(bufferSize) { i -> if (i - offset in values.indices) values[i - offset] else -1f }

    private fun floats(count: Int) = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder())

    private fun ByteBuffer.toList(): List<Float> {
        rewind()
        return List(bufferSize) { getFloat() }
    }

    private val format = vertexFormat { attribute("value", VertexElementType.FLOAT32) }

    @Test
    fun vertexBufferPutWithOffset() {
        val vb = vertexBuffer(format, bufferSize)
        vb.put { repeat(bufferSize) { write(-1f) } }
        val count = vb.put(offset) { values.forEach { write(it) } }
        val result = floats(bufferSize)
        (vb as VertexBufferGL3).read(result)
        vb.destroy()
        assertEquals(values.size, count)
        assertEquals(expected, result.toList())
    }

    @Test
    fun vertexBufferUploadElementsWithOffset() {
        val vb = vertexBuffer(format, bufferSize)
        vb.shadow.writer().apply {
            rewind()
            repeat(bufferSize) { write(-1f) }
        }
        vb.shadow.upload()
        vb.shadow.writer().apply {
            positionElements = offset
            values.forEach { write(it) }
        }
        vb.shadow.uploadElements(offset, values.size)
        val result = floats(bufferSize)
        (vb as VertexBufferGL3).read(result)
        vb.destroy()
        assertEquals(expected, result.toList())
    }

    @Test
    fun bufferTextureWriteWithOffset() {
        val bt = BufferTexture.create(bufferSize, ColorFormat.R, ColorType.FLOAT32)
        bt.write(floats(bufferSize).apply { repeat(bufferSize) { putFloat(-1f) }; rewind() })
        bt.write(floats(values.size).apply { values.forEach { putFloat(it) }; rewind() }, offset, values.size)
        val result = floats(bufferSize)
        bt.read(result)
        bt.destroy()
        assertEquals(expected, result.toList())
    }

    @Test
    fun shaderStorageBufferShadowUploadWithOffset() {
        // shader storage buffers need OpenGL 4.3 or OpenGL ES 3.1
        if (Driver.glType == DriverTypeGL.GL && !Driver.glVersion.isAtLeast(DriverVersionGL.GL_VERSION_4_3)) {
            return
        }
        val ssbo = shaderStorageBuffer(shaderStorageFormat {
            primitive("values", BufferPrimitiveType.FLOAT32, bufferSize)
        })
        ssbo.put { repeat(bufferSize) { write(-1f) } }
        ssbo.shadow.writer().apply {
            rewind()
            repeat(offset) { write(-1f) }
            values.forEach { write(it) }
        }
        ssbo.shadow.upload(offset * 4, values.size * 4)
        val result = floats(bufferSize)
        ssbo.read(result)
        ssbo.destroy()
        assertEquals(expected, result.toList())
    }
}

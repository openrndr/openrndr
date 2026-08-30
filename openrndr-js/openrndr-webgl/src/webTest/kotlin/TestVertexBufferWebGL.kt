import org.openrndr.color.ColorRGBa
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector3
import kotlin.test.Test

class TestVertexBufferWebGL: AbstractApplicationTestFixture() {

    @Test
    fun testVertexBufferCreation() {
        val format = vertexFormat {
            position(3)
            color(4)
        }
        val vertexBuffer = vertexBuffer(format, 100)

        vertexBuffer.close()
    }

    @Test
    fun testVertexBufferPut() {
        val format = vertexFormat {
            position(3)
            color(4)
        }
        val vertexBuffer = vertexBuffer(format, 100)

        vertexBuffer.put(0) {
            for (i in 0 until 100) {
                write(Vector3.ZERO)
                write(ColorRGBa.WHITE)
            }
        }
        vertexBuffer.close()
    }
}
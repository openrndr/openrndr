import org.lwjgl.BufferUtils
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.gl3.VertexBufferGL3
import java.nio.ByteBuffer
import kotlin.test.*

class TestVertexBufferGL3 : AbstractApplicationTestFixture() {
    lateinit var vbgl3: VertexBufferGL3

    @BeforeTest
    override fun setup() {
        super.setup()
        vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10, null)
    }

    @AfterTest
    override fun teardown() {
        vbgl3.destroy()
        super.teardown()
    }

    @Test
    fun `should be able to write a non-direct byte buffer`() {
        val nonDirectByteBuffer = ByteBuffer.allocate(vbgl3.vertexFormat.size * vbgl3.vertexCount)
        vbgl3.write(nonDirectByteBuffer)
    }

    @Test
    fun `should be able to write a direct byte buffer`() {
        val nonDirectByteBuffer = BufferUtils.createByteBuffer(vbgl3.vertexFormat.size * vbgl3.vertexCount)
        vbgl3.write(nonDirectByteBuffer)
    }

    @Test
    fun `should be able to read to a non-direct byte buffer (with copy)`() {
        val nonDirectByteBuffer = ByteBuffer.allocate(vbgl3.vertexFormat.size * vbgl3.vertexCount)
        vbgl3.read(nonDirectByteBuffer)
    }

    @Test
    fun `should be able to read to a direct byte buffer`() {
        val nonDirectByteBuffer = BufferUtils.createByteBuffer(vbgl3.vertexFormat.size * vbgl3.vertexCount)
        vbgl3.read(nonDirectByteBuffer)
    }

    @Test
    fun `a vertex buffer with array attributes`() {
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
            attribute("someArrayAttribute", VertexElementType.FLOAT32, 2)
        }, 10, null)
        program.drawer.vertexBuffer(vbgl3, DrawPrimitive.TRIANGLES)
        vbgl3.destroy()
    }
}
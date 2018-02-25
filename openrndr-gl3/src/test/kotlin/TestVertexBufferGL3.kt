import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.lwjgl.BufferUtils
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.gl3.ApplicationGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import java.nio.ByteBuffer

object TestVertexBufferGL3 : Spek({
    describe("a vertexbuffer") {
        val app = ApplicationGL3(Program(), Configuration())
        app.setup()
        app.preloop()
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10)

        it("should be able to write a non-direct byte buffer") {
            val nonDirectByteBuffer = ByteBuffer.allocate(vbgl3.vertexFormat.size*vbgl3.vertexCount)
            vbgl3.write(nonDirectByteBuffer)
        }

        it("should be able to write a direct byte buffer") {
            val nonDirectByteBuffer = BufferUtils.createByteBuffer(vbgl3.vertexFormat.size*vbgl3.vertexCount)
            vbgl3.write(nonDirectByteBuffer)
        }

        it("should be able to read to a non-direct byte buffer (with copy)") {
            val nonDirectByteBuffer = ByteBuffer.allocate(vbgl3.vertexFormat.size*vbgl3.vertexCount)
            vbgl3.read(nonDirectByteBuffer)
        }

        it("should be able to read to a direct byte buffer") {
            val nonDirectByteBuffer = BufferUtils.createByteBuffer(vbgl3.vertexFormat.size*vbgl3.vertexCount)
            vbgl3.read(nonDirectByteBuffer)
        }
    }
})
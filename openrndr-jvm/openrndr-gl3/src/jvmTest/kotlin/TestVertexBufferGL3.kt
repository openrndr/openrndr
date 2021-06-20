import kotlinx.coroutines.runBlocking
import org.lwjgl.BufferUtils
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import java.nio.ByteBuffer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestVertexBufferGL3 : Spek({

    describe("a program") {
        val program = Program()
        val app = ApplicationGLFWGL3(program, Configuration())
        runBlocking { app.setup() }
        app.preloop()

        describe("a vertex buffer") {
            val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
                position(3)
            }, 10, null)

            it("should be able to write a non-direct byte buffer") {
                val nonDirectByteBuffer = ByteBuffer.allocate(vbgl3.vertexFormat.size * vbgl3.vertexCount)
                vbgl3.write(nonDirectByteBuffer)
            }

            it("should be able to write a direct byte buffer") {
                val nonDirectByteBuffer = BufferUtils.createByteBuffer(vbgl3.vertexFormat.size * vbgl3.vertexCount)
                vbgl3.write(nonDirectByteBuffer)
            }

            it("should be able to read to a non-direct byte buffer (with copy)") {
                val nonDirectByteBuffer = ByteBuffer.allocate(vbgl3.vertexFormat.size * vbgl3.vertexCount)
                vbgl3.read(nonDirectByteBuffer)
            }

            it("should be able to read to a direct byte buffer") {
                val nonDirectByteBuffer = BufferUtils.createByteBuffer(vbgl3.vertexFormat.size * vbgl3.vertexCount)
                vbgl3.read(nonDirectByteBuffer)
            }
        }

        describe("a vertex buffer with array attributes") {
            val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
                position(3)
                attribute("someArrayAttribute", VertexElementType.FLOAT32, 2)
            }, 10, null)
            program.drawer.vertexBuffer(vbgl3, DrawPrimitive.TRIANGLES)

        }
    }


})
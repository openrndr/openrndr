import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferPrimitiveType
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shaderStorageBuffer
import org.openrndr.draw.shaderStorageFormat
import org.openrndr.math.Vector3

fun main() = application {
    program {

        val ssf = shaderStorageFormat {
            struct("Vertex", "vertex", 3) {
                primitive("position", BufferPrimitiveType.VECTOR3_FLOAT32)
                primitive("color", BufferPrimitiveType.VECTOR4_FLOAT32)
            }
        }

        val ssbo = shaderStorageBuffer(ssf)
        ssbo.put {
            write(Vector3(10.0, 10.0, 0.0))
            write(ColorRGBa.RED)
            write(Vector3(10.0, 100.0, 0.0))
            write(ColorRGBa.GREEN)
            write(Vector3(100.0, 100.0, 0.0))
            write(ColorRGBa.BLUE)
        }
        val vb = ssbo.vertexBufferView()
        println(vb.vertexFormat)
        println(vb.vertexCount)
        extend {
            drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)
        }
    }
}
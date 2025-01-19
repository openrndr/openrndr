import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.IntVector3
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main() {
    application {
        program {

            val ib = indexBuffer(99, IndexType.INT32)
            val ssib = ib.shaderStorageBufferView()

            val vb0 = vertexBuffer(vertexFormat {
                position(3)
                paddingFloat(1)
            }, 99)

            val vb1 = vertexBuffer(vb0.vertexFormat, vb0.vertexCount)

            val ssb0 = vb0.shaderStorageBufferView()
            val ssb1 = vb1.shaderStorageBufferView()


            vb0.put {
                for (i in 0 until 99) {
                    val x = Math.random() * width
                    val y = Math.random() * height
                    write(x.toFloat(), y.toFloat(), 0.0f)
                    write(0.0f)
                }
            }
            extend {
                val cs = computeStyle {
                    workGroupSize = IntVector3(1, 1, 1)
                    computeTransform = """
                        int idx = int(gl_GlobalInvocationID.x);
                        b_output_vertices.vertex[idx] = b_input_vertices.vertex[idx];
                        b_output_vertices.vertex[idx].position.x += cos(b_output_vertices.vertex[idx].position.y*0.02) * 40.0;
                        b_indices.indices[idx] = idx;
            """.trimIndent()
                    buffer("input_vertices", ssb0)
                    buffer("output_vertices", ssb1)
                    buffer("indices", ssib)
                }
                cs.execute(99, 1, 1)

                val bb = ByteBuffer.allocateDirect(99 * 4)
                bb.order(ByteOrder.nativeOrder())
                ib.read(bb)
                bb.rewind()

                drawer.fill = ColorRGBa.RED
                drawer.vertexBuffer(vb0, DrawPrimitive.TRIANGLES)

                drawer.fill = ColorRGBa.BLUE
                drawer.vertexBuffer(vb1, DrawPrimitive.TRIANGLES)
            }
        }
    }
}
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.IntVector3

/**
 * This program demonstrates the use of vertex buffers, compute shaders, and rendering techniques
 * in a creative and dynamic way.
 *
 * Main operations include:
 * - The creation of two vertex buffers (`vb0` and `vb1`) to hold vertex data.
 * - Population of the `vb0` vertex buffer with initial random vertex positions.
 * - Creation of shader storage buffer views (`ssb0` and `ssb1`) from the vertex buffers.
 * - Establishment of a compute style to perform vertex transformation via compute shaders.
 * - Execution of the compute style which updates vertex positions with animated transformations.
 * - Drawing of the transformed vertex buffer using triangular primitives with partial opacity applied.
 *
 */
fun main() {
    application {
        program {
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
                        b_output_vertices.vertex[idx].position.x += cos(p_time + b_input_vertices.vertex[idx].position.y*0.02) * 40.0;
                        b_output_vertices.vertex[idx].position.y += sin(p_time + b_input_vertices.vertex[idx].position.x*0.02) * 40.0;
            """.trimIndent()
                    buffer("input_vertices", ssb0)
                    buffer("output_vertices", ssb1)
                    parameter("time", seconds)
                }
                cs.execute(99, 1, 1)

                drawer.fill = ColorRGBa.WHITE.opacify(0.25)
                drawer.vertexBuffer(vb1, DrawPrimitive.TRIANGLES)
            }
        }
    }
}
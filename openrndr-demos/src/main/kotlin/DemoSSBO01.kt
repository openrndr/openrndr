import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.math.IntVector3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

// A Shader Storage Buffer Object is a Buffer Object that is used to
// store and retrieve data from within the OpenGL Shading Language.

// https://www.khronos.org/opengl/wiki/Shader_Storage_Buffer_Object

// This demo uses two SSBOs: the first one is used as data for calculations,
// the second one is the result which is read back from GPU to CPU.

// The compute shader is executed `csWidth` times in parallel.
// Each instance uses the same input data and outputs different results
// by dividing the data by the shader execution instance ID.

// Note that reading data back to the CPU is optional.

fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val cs = computeStyle {
            computeTransform = """
                ivec2 id = ivec2(gl_GlobalInvocationID.xy);
                int idx = id.x * 6;
                float k =  1.0 + float(id.x);
                          
                b_Block2.results[idx + 0] = float(b_Block1.particles[0].position.x) / k;
                b_Block2.results[idx + 1] = float(b_Block1.particles[0].position.y) / k;
t                b_Block2.results[idx + 2] = float(b_Block1.particles[0].position.z) / k;
                
                b_Block2.results[idx + 3] = float(b_Block1.agents[0].position.x) / k;
                b_Block2.results[idx + 4] = float(b_Block1.agents[0].position.y) / k;
                b_Block2.results[idx + 5] = float(b_Block1.agents[0].position.z) / k;                
            """.trimIndent()
        }

        // number of parallel calculations in the GPU
        val csWidth = 10
        val resultLength = 6

        // define structures matching the two `buffer` entries from GLSL
        val block1 = shaderStorageBuffer(shaderStorageFormat {
            primitive("trap", BufferPrimitiveType.VECTOR2_FLOAT32, 8)
            struct("Particle", "particles", 4) {
                primitive("position", BufferPrimitiveType.VECTOR3_INT32)
                primitive("velocity", BufferPrimitiveType.VECTOR3_FLOAT32)
                primitive("age", BufferPrimitiveType.FLOAT32)
                primitive("isActive", BufferPrimitiveType.INT32) // was BOOLEAN
            }
            struct("Agent", "agents", 2) {
                primitive("position", BufferPrimitiveType.VECTOR3_FLOAT32)
            }
        })

        val block2 = shaderStorageBuffer(shaderStorageFormat {
            primitive("results", BufferPrimitiveType.FLOAT32, csWidth * resultLength)
        })

        // upload data to the compute shader
        block1.put {
            // trap
            for (i in 1..8) {
                write(Vector2(500.0, 500.0))
            }

            // particles
            for (i in 1..4) {
                // position
                write(IntVector3(201 * i, 202 * i, 203 * i))
                // velocity
                write(Vector3(420.0 * i))
                // age
                write(20f * i)
                // isActive
                write(if (Random.nextBoolean()) 1 else 0)
            }

            // agents
            for (i in 1..2) {
                // position
                write(Vector3(420.0 * i))
            }
        }

        // bind SSBOs
        cs.buffer("Block1", block1)
        cs.buffer("Block2", block2)

        // do calculation
        cs.execute(csWidth, 1, 1)

        // read back results
        val byteBuffer = ByteBuffer.allocateDirect(block2.format.size)
            .order(ByteOrder.nativeOrder())
        block2.read(byteBuffer)

        // show results
        println("Results:")
        byteBuffer.rewind()
        repeat(csWidth) {
            repeat(resultLength) {
                print("${byteBuffer.float}, ")
            }
            println()
        }
    }
}

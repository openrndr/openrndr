import org.openrndr.application
import org.openrndr.draw.BufferPrimitiveType
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.shaderStorageBuffer
import org.openrndr.draw.shaderStorageFormat
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
        val shader = """#version ${(Driver.instance as DriverGL3).version.glslVersion}
            //#extension GL_EXT_shader_implicit_conversions: require
            
            layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;
            
            struct Particle {
                ivec3 position;
                vec3 velocity;
                float age;
                bool isActive;
            };
            
            struct Agent {
                vec3 position;
            };
            
            // SSBOs are declared as using the `buffer` keyword
            layout (std430, binding = 3) buffer Block1 {
                vec2 trap[8];
                Particle particles[4];
                Agent agents[2];
            };
            
            layout (std430, binding = 5) buffer Block2 {
                float results[];
            };
            
            void main(void) {
                ivec2 id = ivec2(gl_GlobalInvocationID.xy);
                int idx = id.x * 6;
                float k =  1.0 + float(id.x);
                          
                results[idx + 0] = float(particles[0].position.x) / k;
                results[idx + 1] = float(particles[0].position.y) / k;
                results[idx + 2] = float(particles[0].position.z) / k;
                
                results[idx + 3] = float(agents[0].position.x) / k;
                results[idx + 4] = float(agents[0].position.y) / k;
                results[idx + 5] = float(agents[0].position.z) / k;
            }
        """.trimIndent()

        val cs = ComputeShader.fromCode(shader, "Compute")
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
                primitive("isActive", BufferPrimitiveType.BOOLEAN)
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
                write(Random.nextBoolean())
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

import org.intellij.lang.annotations.Language
import org.openrndr.application
import org.openrndr.collections.pmap
import org.openrndr.draw.*
import org.openrndr.math.IntVector3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

// TODO: Find a fix for reading back struct
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        @Language("GLSL")
        val shader = """
            #version 450 core
            
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
            
            layout (std430, binding = 0) buffer Block1 {
                vec2 trap[8];
                Particle particles[4];
                Agent agents[2];
            };
            
            layout (std430, binding = 1) buffer Block2 {
                float results[];
            };
            
            void main(void) {
                ivec2 id = ivec2(gl_GlobalInvocationID.xy);
                int idx = 0;
                
                results[idx++] = particles[0].position.x;
                results[idx++] = particles[0].position.y;
                results[idx++] = particles[0].position.z;
                
                results[idx++] = agents[0].position.x;
                results[idx++] = agents[0].position.y;
                results[idx++] = agents[0].position.z;
            }
        """.trimIndent()

        val count = 10
        val cs = ComputeShader.fromCode(shader, "Compute")

        val block1 = shaderStorageBuffer(shaderStorageFormat {
            member("trap", BufferMemberType.VECTOR2_FLOAT, 8)
            struct("Particle", "particles", 4) {
                member("position", BufferMemberType.VECTOR3_INT)
                member("velocity", BufferMemberType.VECTOR3_FLOAT)
                member("age", BufferMemberType.FLOAT)
                member("isActive", BufferMemberType.BOOLEAN)
            }
            struct("Agent", "agents", 2) {
                member("position", BufferMemberType.VECTOR3_FLOAT)
            }
        })

        val block2 = shaderStorageBuffer(shaderStorageFormat {
            member("results", BufferMemberType.FLOAT, 6)
        })

        block1.put {
            for (i in 0 until 8) {
                write(Vector2(500.0, 500.0))
            }

            for (i in 1..4) {
                write(IntVector3(201 * i, 202 * i, 203 * i))
                write(Vector3(420.0 * i))
                write(20f * i)
                write(Random.nextBoolean())
            }

            for (i in 1..2) {
                write(Vector3(420.0 * i))
            }
        }

        val bb2 = ByteBuffer.allocateDirect(block2.format.size).order(ByteOrder.nativeOrder())

        cs.buffer("Block1", block1)
        cs.buffer("Block2", block2)

        cs.execute(count, 1, 1)

        block2.read(bb2)
        bb2.rewind()

        println("Results:")

        for (i in 0 until 6) {
            print("${bb2.float}, ")
        }
    }
}

import org.openrndr.draw.BufferPrimitiveType
import org.openrndr.draw.shaderStorageBuffer
import org.openrndr.draw.shaderStorageFormat
import org.openrndr.math.Vector3
import kotlin.random.Random
import kotlin.test.Test

class TestShaderStorageBufferGL43: AbstractApplicationTestFixture() {

    /**
     * Tests the process of populating a Shader Storage Buffer Object (SSBO) with a specified data format.
     *
     * This function performs the following steps:
     * - Initializes a list of 3D vectors (`positions`) with random values.
     * - Creates a shader storage format composed of two primitives:
     *   - `centers` of type `VECTOR4_FLOAT32` to store vector data in 4D (xyz1).
     *   - `radius` of type `FLOAT32` to store scalar data.
     * - Allocates a shader storage buffer (`sphereBuff`) with the defined format.
     * - Writes data into the SSBO:
     *   - For `centers`, writes the 4D extension (xyz1) of each position vector.
     *   - For `radius`, writes the x-component of each vector as a float.
     *
     * This test ensures that the format definition and data writing mechanism for the SSBO are functioning as intended.
     */
    @Test
    fun testSSBOPut1() {
        val positions = List(15) { Vector3(Random.nextDouble(), Random.nextDouble(), Random.nextDouble()) }

        val format = shaderStorageFormat {
            primitive("centers", BufferPrimitiveType.VECTOR4_FLOAT32, positions.size)
            primitive("radius", BufferPrimitiveType.FLOAT32, positions.size)
        }
        val sphereBuff = shaderStorageBuffer(format)

        sphereBuff.put {
            positions.forEachIndexed { i, it ->
                val v0 = it.xyz1
                write(v0)
            }
            positions.forEachIndexed { index, it ->
                val v1 = it.x.toFloat()
                write(v1)
            }
        }
    }

    /**
     * Tests the functionality of writing data into a Shader Storage Buffer Object (SSBO) with a specific format.
     *
     * This method initializes a list of 3D vectors (positions) and creates a shader storage format,
     * defining two primitives: "radius" of type `FLOAT32` and "centers" of type `VECTOR4_FLOAT32`.
     * The format size corresponds to the size of the positions list. A shader storage buffer is
     * created based on this format.
     *
     * Data is then written into the SSBO:
     * - Iterates over the list of positions and writes the x-component of each vector as a float value
     *   corresponding to the "radius" primitive.
     * - Writes the 4D extension (xyz1) of each position vector into the "centers" primitive.
     *
     * This test ensures that the format definition and data writing process are functioning as intended.
     */
    @Test
    fun testSSBOPut2() {
        val positions = List(15) { Vector3(Random.nextDouble(), Random.nextDouble(), Random.nextDouble()) }

        val format = shaderStorageFormat {
            primitive("radius", BufferPrimitiveType.FLOAT32, positions.size)
            primitive("centers", BufferPrimitiveType.VECTOR4_FLOAT32, positions.size)
        }
        val sphereBuff = shaderStorageBuffer(format)

        sphereBuff.put {

            positions.forEachIndexed { index, it ->
                val v1 = it.x.toFloat()
                write(v1)
            }
            positions.forEachIndexed { i, it ->
                val v0 = it.xyz1
                write(v0)
            }
        }
    }
}
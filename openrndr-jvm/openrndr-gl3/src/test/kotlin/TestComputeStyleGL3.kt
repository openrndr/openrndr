import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.*
import org.openrndr.math.*
import kotlin.test.Test
import kotlin.test.assertTrue

class TestComputeStyleGL3 : AbstractApplicationTestFixture() {
    @Test
    fun test() {
        if (Driver.capabilities.compute) {
            val cs = computeStyle {
                computeTransform = ""
            }
            cs.execute(1, 1, 1)
        }
    }

    @Test
    fun testImageBinding() {
        val img = colorBuffer(256, 256, type = ColorType.UINT8)
        if (Driver.capabilities.compute) {
            val cs = computeStyle {
                computeTransform = "p_img;"
                registerImageBinding(
                    "img",
                    BufferAccess.READ,
                    setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT)
                )
                image("img", img, 0)
            }
            cs.execute(1, 1, 1)
        }
        img.destroy()
    }

    @Test
    fun testVolumeImageBinding() {
        if (Driver.capabilities.compute) {
            val img = volumeTexture(16, 16, 16, type = ColorType.UINT8)
            val cs = computeStyle {
                computeTransform = "p_img; p_imgArray[0];"
                registerImageBinding(
                    "img",
                    BufferAccess.READ,
                    setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT)
                )
                registerImageBinding("imgArray", BufferAccess.READ, setOf(BufferFlag.COHERENT), 3)
                image("img", img, 0)
                image("imgArray", arrayOf(img, img, img), arrayOf(0, 0, 0))
            }
            cs.execute(1, 1, 1)
            img.destroy()
        }
    }

    /**
     * Tests image binding functionality for array textures using compute shaders.
     *
     * This test verifies:
     * - The creation of an array texture with specific dimensions and layers.
     * - The configuration of a compute style that registers an image binding, using the array texture as the image source.
     * - The execution of a compute shader leveraging the defined compute style and image binding.
     * - Proper cleanup and destruction of the allocated array texture resource.
     *
     * The test ensures correctness in the setup of image bindings for array textures and their usage
     * in compute shader executions. It only runs when compute capabilities are supported by the driver.
     */
    @Test
    fun testArrayTextureImageBinding() {
        if (Driver.capabilities.compute) {
            val img = arrayTexture(256, 256, 10, type = ColorType.UINT8)
            val cs = computeStyle {
                computeTransform = "p_img;"
                registerImageBinding(
                    "img",
                    BufferAccess.READ,
                    setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT)
                )
                image("img", img, 0)
            }
            cs.execute(1, 1, 1)
            img.destroy()
        }
    }

    /**
     * Tests the functionality of cubemap image bindings in compute shaders.
     *
     * This test verifies:
     * - The creation of a cubemap texture with specific dimensions and color type.
     * - The configuration of a compute style that registers an image binding referencing the created cubemap texture.
     * - Execution of a compute shader using the defined compute style and image binding.
     * - Proper cleanup and destruction of the allocated cubemap texture resource.
     *
     * This test ensures the correctness of cubemap image binding setup and their usage in compute shader executions.
     * It is executed only if compute capabilities are supported by the driver.
     */
    @Test
    fun testCubemapImageBinding() {
        if (Driver.capabilities.compute) {
            val img = cubemap(256, type = ColorType.UINT8)
            val cs = computeStyle {
                computeTransform = "p_img;"
                registerImageBinding(
                    "img",
                    BufferAccess.READ,
                    setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT)
                )
                image("img", img, 0)
            }
            cs.execute(1, 1, 1)
            img.destroy()
        }
    }

    /**
     * Tests the functionality of array cubemap image bindings in compute shaders.
     *
     * This test verifies:
     * - The creation of an array cubemap texture with specific dimensions and layers.
     * - The configuration of a compute style that registers an image binding referencing the created array cubemap texture.
     * - Execution of a compute shader using the defined compute style and image binding.
     * - Proper cleanup and destruction of the allocated array cubemap texture resource.
     *
     * The test ensures the correctness of array cubemap image bindings setup and their usage in compute shader executions.
     * It only runs when compute capabilities are supported by the driver.
     */
    @Test
    fun testArrayCubemapImageBinding() {
        if (Driver.capabilities.compute) {
            val img = arrayCubemap(256, 10, type = ColorType.UINT8)
            val cs = computeStyle {
                computeTransform = "p_img;"
                registerImageBinding(
                    "img",
                    BufferAccess.READ,
                    setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT)
                )
                image("img", img, 0)
            }
            cs.execute(1, 1, 1)
            img.destroy()
        }
    }

    @Test
    fun testIntVectors() {
        if (Driver.capabilities.compute) {
            val cs = computeStyle {
                computeTransform = "p_ivec2; p_ivec3; p_ivec4;"
                parameter("ivec2", IntVector2.ZERO)
                parameter("ivec3", IntVector3.ZERO)
                parameter("ivec4", IntVector4.ZERO)
            }
            cs.execute(1, 1, 1)
        }
    }

    /**
     * Tests the functionality of copying data between two shader storage buffers using compute shaders.
     *
     * This test verifies:
     * - The correct definition of a shader storage format containing various primitive types, vectors, and structs.
     * - The creation and population of source and target shader storage buffer objects (SSBOs) based on the defined format.
     * - The execution of a compute shader that performs a direct data copy operation from the source to the target buffer.
     * - The correctness of the transformed data in the target buffer through validation checks.
     *
     * The test works only if the compute capabilities are supported. It ensures that the data written into the source
     * buffer matches the data retrieved from the target buffer after executing the compute shader.
     */
    @Test
    fun testComputeCopy() {
        if (Driver.capabilities.compute) {
            val fmt = shaderStorageFormat {
                primitive("time", BufferPrimitiveType.FLOAT32)
                primitive("vertex", BufferPrimitiveType.VECTOR2_FLOAT32, 3)
                struct("Particle", "particles", 5) {
                    primitive("age", BufferPrimitiveType.FLOAT32)
                    primitive("pos", BufferPrimitiveType.VECTOR3_FLOAT32)
                }
            }

            // Create SSBOs
            val source = shaderStorageBuffer(fmt)
            val target = shaderStorageBuffer(fmt)

            // Populate SSBO
            source.put {
                write(3.0.toFloat()) // time
                repeat(3) {
                    write(Vector2(1.1, 1.2)) // vertex
                }
                repeat(5) {
                    write(1.0.toFloat()) // age
                    write(Vector3(2.1, 2.2, 2.3))// pos
                }
            }

            val cs = computeStyle {
                computeTransform = """
                    int i = int(gl_GlobalInvocationID.x);                    
                    b_output.time = b_input.time;
                    b_output.vertex = b_input.vertex;
                    b_output.particles = b_input.particles;
                """.trimIndent()

                buffer("input", source)
                buffer("output", target)
            }
            cs.execute(1, 1, 1)

            // verify results
            val eps = 1E-6
            target.shadow.download()
            target.shadow.reader().apply {
                rewind()
                assertTrue(readFloat() in 3.0-eps..3.0+eps)
                for (i in 0 until 3) {
                    val x = readVector2()
                    assertTrue(x.x in 1.1-eps..1.1+eps)
                    assertTrue(x.y in 1.2-eps..1.2+eps)
                }
                for (i in 0 until 5) {
                    val v = readFloat()
                    assertTrue(v in 1.0-eps..1.0+eps)
                    val v3 = readVector3()
                    assertTrue(v3.x in 2.1-eps..2.1+eps)
                    assertTrue(v3.y in 2.2-eps..2.2+eps)
                    assertTrue(v3.z in 2.3-eps..2.3+eps)
                }
            }
        }
    }
}
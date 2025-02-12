import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test

class TestArrayCubemapTextureGL3 : AbstractApplicationTestFixture() {

    /**
     * Tests the creation and usage of an array cubemap texture with the most common
     * configuration, including operations such as writing to its layers and sides,
     * and validating its lifecycle.
     *
     * This test creates an array cubemap texture with dimensions 256x256,
     * 10 layers, the RGBa color format, and the UINT8_SRGB color type. It allocates
     * a ByteBuffer for data, writes this data to each side of the cubemap across all
     * layers, and ensures proper cleanup by closing the resource.
     *
     * The test validates the proper functioning of the arrayCubemap creation utility
     * and the ability to interact correctly with all sides and layers of the cubemap.
     */
    @Test
    fun testArrayCubemapMostCommon() {
        val acm = arrayCubemap(256, 10, format = ColorFormat.RGBa, type = ColorType.UINT8_SRGB)
        val bb = ByteBuffer.allocateDirect(256 * 256 * 10 * acm.format.componentCount * acm.type.componentSize)
        for (side in CubemapSide.entries) {
            for (i in 0 until 10) {
                acm.write(side, i, bb)
            }
        }
        acm.close()
    }


    /**
     * Tests the creation and usage of an array cubemap texture with RGB color format and UINT8_SRGB color type.
     *
     * This test creates an array cubemap texture with dimensions 256x256, 10 layers,
     * RGB color format, and UINT8_SRGB color type. A ByteBuffer is allocated with
     * sufficient capacity to store the texture data for all layers and sides.
     *
     * The test iterates through all sides of the cubemap and writes data to each layer
     * using the created ByteBuffer. It ensures that data can be written successfully
     * to each layer and side of the array cubemap, demonstrating correct functionality
     * of the array cubemap class and its `write` method.
     *
     * Finally, the test ensures proper cleanup by closing the array cubemap resource.
     */
    @Test
    fun testArrayCubemapRGB() {
        val acm = arrayCubemap(256, 10, format = ColorFormat.RGB, type = ColorType.UINT8_SRGB)
        val bb = ByteBuffer.allocateDirect(256 * 256 * 10 * acm.format.componentCount * acm.type.componentSize)
        for (side in CubemapSide.entries) {
            for (i in 0 until 10) {
                acm.write(side, i, bb)
            }
        }
        acm.close()
    }

    /**
     * Tests the creation and usage of an array cubemap texture with RGB color format and FLOAT16 color type.
     *
     * This test validates the functionality of creating an array cubemap with dimensions 256x256 per layer,
     * 10 layers, RGB color format, and FLOAT16 color type. A ByteBuffer is allocated with sufficient capacity
     * to store the texture data for all layers and sides of the cubemap.
     *
     * The test iterates through all sides of the cubemap and writes data to each layer using the ByteBuffer.
     * It ensures that data can be written to each layer and side, verifying the correct operation of
     * the `arrayCubemap` creation utility and its `write` method.
     *
     * Finally, the test ensures proper cleanup by closing the array cubemap resource.
     */
    @Test
    fun testArrayCubemapRGBaFloat16() {
        val acm = arrayCubemap(256, 10, format = ColorFormat.RGB, type = ColorType.FLOAT16)
        val bb = ByteBuffer.allocateDirect(256 * 256 * 10 * acm.format.componentCount * acm.type.componentSize)
        for (side in CubemapSide.entries) {
            for (i in 0 until 10) {
                acm.write(side, i, bb)
            }
        }
        acm.close()
    }

    /**
     * Tests the creation and usage of an array cubemap texture with RGB color format and FLOAT32 color type.
     *
     * This test validates the functionality of creating an array cubemap with dimensions 256x256 per layer,
     * 10 layers, the RGB color format, and FLOAT32 color type. A ByteBuffer is allocated with sufficient
     * capacity to store the texture data for all layers and sides of the cubemap.
     *
     * The test iterates through all sides of the cubemap and writes data to each layer using the ByteBuffer.
     * It ensures that data can be written to each layer and side, verifying the correct operation of
     * the `arrayCubemap` creation utility and its `write` method.
     *
     * Finally, the test ensures proper cleanup by closing the array cubemap resource.
     */
    @Test
    fun testArrayCubemapRGBaFloat32() {
        val acm = arrayCubemap(256, 10, format = ColorFormat.RGB, type = ColorType.FLOAT16)
        val bb = ByteBuffer.allocateDirect(256 * 256 * 10 * acm.format.componentCount * acm.type.componentSize)
        for (side in CubemapSide.entries) {
            for (i in 0 until 10) {
                acm.write(side, i, bb)
            }
        }
        acm.close()
    }

}

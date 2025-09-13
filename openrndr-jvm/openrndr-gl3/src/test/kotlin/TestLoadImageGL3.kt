import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.loadImage
import java.io.File
import kotlin.math.log2
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLoadImageGL3 : AbstractApplicationTestFixture() {

    private fun locateImage(image: String) : String {
        val f0 = File(image)
        val f1 = File("../../$image")

        return if (f0.exists()) {
            f0.absolutePath
        } else if (f1.exists()) {
            f1.absolutePath
        } else {
            error("image not found $image")
        }
    }

    /**
     * Tests loading a grayscale image and verifies its properties.
     *
     * This function loads a grayscale image from a given file path using the `loadImage` function.
     * It asserts that the loaded image has the correct color format (`ColorFormat.RGBa`) and type
     * (`ColorType.UINT8_SRGB`), ensuring that the loading operation processes the image as expected.
     */
    @Test
    fun loadGrayscaleImage() {
        loadImage(locateImage("data/images/grayscale-8.jpg")).use { img ->
            assertEquals(ColorFormat.RGBa, img.format)
            assertEquals(ColorType.UINT8_SRGB, img.type)
            assertEquals(log2(img.effectiveWidth.toDouble()).toInt(), img.levels)
        }
    }

    /**
     * Tests loading an RGB image and verifies its format and type.
     *
     * This function loads an RGB image from a specified file path using the `loadImage` function.
     * It validates that the loaded image has the `ColorFormat.RGBa` format and the `ColorType.UINT8_SRGB` type.
     * Finally, it releases resources associated with the image after verification.
     */
    @Test
    fun loadRGBImage() {
        loadImage(locateImage("data/images/rgb-8_8_8.jpg")).use { img ->
            assertEquals(ColorFormat.RGBa, img.format)
            assertEquals(ColorType.UINT8_SRGB, img.type)
            assertEquals(log2(img.effectiveWidth.toDouble()).toInt(), img.levels)
        }
    }

    /**
     * Tests loading an RGB image without generating mipmaps and validates its format and type.
     *
     * This function loads an RGB image from a specified file path using the `loadImage` function
     * with mipmaps disabled. It verifies that the loaded image has the `ColorFormat.RGBa` format,
     * the `ColorType.UINT8_SRGB` type, and a single mipmap level. After validation, the resources
     * associated with the image are released.
     */
    @Test
    fun loadRGBImageWithoutMipmap() {
        loadImage(locateImage("data/images/rgb-8_8_8.jpg"), loadMipmaps = false).use { img ->
            assertEquals(ColorFormat.RGBa, img.format)
            assertEquals(ColorType.UINT8_SRGB, img.type)
            assertEquals(1, img.levels)
        }
    }

    /**
     * Tests the loading of an RGBA image with 8 bits per component and verifies its properties.
     *
     * This function loads an RGBA image from the specified file path using the `loadImage` function.
     * It asserts that the loaded image has the `ColorFormat.RGBa` format and the `ColorType.UINT8_SRGB` type,
     * ensuring that the image is correctly interpreted as having four components (red, green, blue, alpha) with 8 bits per component
     * and sRGB color encoding.
     */
    @Test
    fun loadRGBa8Image() {
        loadImage(locateImage("data/images/rgba-8_8_8_8.png")).use { img ->
            assertEquals(ColorFormat.RGBa, img.format)
            assertEquals(ColorType.UINT8_SRGB, img.type)
            assertEquals(log2(img.effectiveWidth.toDouble()).toInt(), img.levels)
        }
    }

    /**
     * Tests the loading of an RGBA image with 8 bits per component and verifies its properties.
     *
     * This function loads an RGBA image from the specified file path using the `loadImage` function.
     * It asserts that the loaded image has the `ColorFormat.RGBa` format and the `ColorType.UINT8_SRGB` type,
     * ensuring that the image is correctly interpreted as having four components (red, green, blue, alpha) with 8 bits per component
     * and sRGB color encoding.
     */
    @Test
    fun loadRGBa16Image() {
        loadImage(locateImage("data/images/rgba-16_16_16_16.png")).use { img ->
            assertEquals(ColorFormat.RGBa, img.format)
            assertEquals(ColorType.UINT16, img.type)
            assertEquals(log2(img.effectiveWidth.toDouble()).toInt(), img.levels)
        }
    }

    /**
     * Tests loading a 16-bit grayscale image and verifies its properties.
     *
     * This function loads a grayscale image from the specified path using the `loadImage` function.
     * It validates that the image has the `ColorFormat.R` format and the `ColorType.UINT16` type,
     * ensuring that the image is interpreted correctly as a single-component grayscale image with
     * 16 bits per channel.
     */
    @Test
    fun loadGrayscaleImage16() {
        loadImage(locateImage("data/images/grayscale-16.png")).use { img ->
            assertEquals(ColorFormat.R, img.format)
            assertEquals(ColorType.UINT16, img.type)
            assertEquals(log2(img.effectiveWidth.toDouble()).toInt(), img.levels)
        }
    }

    @Test
    fun dataUrls() {
        loadImage(locateImage("data/images/rgba-8_8_8_8.png")).use { img ->
            val dataUrl =  img.toDataUrl()
            loadImage(dataUrl).use { img2 ->
                assertEquals(img.width, img2.width)
            }
        }
    }
}
@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlinx.coroutines.test.runTest
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.imagedriver.browser.ImageDriverBrowser
import org.openrndr.imagedriver.browser.rgbaTopDown
import org.openrndr.internal.ImageData
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun browserDecoding(): Boolean = js("typeof OffscreenCanvas !== 'undefined' && typeof document !== 'undefined'")

private fun repositoryRoot(): JsString? =
    js("(globalThis.process && globalThis.process.env && globalThis.process.env.OPENRNDR_ROOT) || null")

/** the RGBA pixel at ([x], [y]) of 8-bit RGBA [data], y counted from the first row */
private fun ImageData.rgba(x: Int, y: Int): List<Int> {
    val buffer = data!!
    buffer.position((y * width + x) * 4)
    val pixel = List(4) { buffer.get().toInt() and 0xff }
    buffer.rewind()
    return pixel
}

class TestImageDriverBrowser {
    private val driver = ImageDriverBrowser()

    @Test
    fun `probing reads the dimensions and channels from the headers`() {
        assertEquals(ImageFileDetails(4, 2, 4), driver.probeImage(PNG_RGBA_4X2))
        assertEquals(ImageFileDetails(3, 3, 1), driver.probeImage(PNG_GRAY_3X3))
        assertEquals(ImageFileDetails(8, 8, 3), driver.probeImage(JPEG_RGB_8X8))
        assertEquals(ImageFileDetails(5, 4, 3), driver.probeImage(GIF_5X4))
        assertEquals(ImageFileDetails(4, 2, 4), driver.probeImage(WEBP_LOSSLESS_4X2))
        assertEquals(ImageFileDetails(7, 5, 3), driver.probeImage(BMP_RGB_7X5))
        assertEquals(ImageFileDetails(8, 4, 4), driver.probeImage(DDS_DXT5_8X4))
    }

    @Test
    fun `DDS images are loaded synchronously`() {
        val image = driver.loadImage(DDS_DXT5_8X4, ImageFileFormat.DDS, true, null)
        assertEquals(8, image.width)
        assertEquals(4, image.height)
        assertEquals(ColorType.DXT5, image.type)
    }

    @Test
    fun `other images are not loaded synchronously`() {
        val error = assertFailsWith<IllegalStateException> { driver.loadImage(PNG_RGBA_4X2, null, true, null) }
        assertTrue(error.message!!.contains("loadImageSuspend"))
    }

    @Test
    fun `cubemaps are loaded from DDS images`() {
        val root = repositoryRoot()?.toString() ?: return
        val cubemap = driver.loadCubemapImage("$root/openrndr-jvm/openrndr-gl3/src/jvmTest/resources/cubemaps/garage.dds", null)
        assertEquals(cubemap.width, cubemap.height)
        assertEquals(6, cubemap.sides.size)
    }

    @Test
    fun `image data is converted to RGBA with the top row first`() {
        // a 2 x 2 RGB image, stored with the bottom row first
        val data = MPPBuffer.allocate(12)
        for (value in listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)) data.put(value.toByte())
        data.rewind()
        val image = driver.createImageData(2, 2, ColorFormat.RGB, ColorType.UINT8, false, data)
        val rgba = rgbaTopDown(image)
        assertEquals(listOf(7, 8, 9, 255, 10, 11, 12, 255, 1, 2, 3, 255, 4, 5, 6, 255), List(16) { rgba.get().toInt() and 0xff })
    }

    @Test
    fun `PNG and WebP images are decoded with the bottom row first`() = runTest {
        if (!browserDecoding()) return@runTest
        for (url in listOf(PNG_RGBA_4X2, WEBP_LOSSLESS_4X2)) {
            val image = driver.loadImageSuspend(url, null, true, null)
            assertEquals(4, image.width)
            assertEquals(2, image.height)
            assertEquals(ColorFormat.RGBa, image.format)
            assertEquals(ColorType.UINT8_SRGB, image.type)
            // the bottom row of the image is the first row of the data
            assertEquals(listOf(10, 20, 30, 255), image.rgba(0, 0))
            assertEquals(listOf(200, 100, 50, 255), image.rgba(1, 0))
            assertEquals(0, image.rgba(2, 0)[3])
            assertEquals(listOf(255, 255, 0, 255), image.rgba(3, 0))
            assertEquals(listOf(255, 0, 0, 255), image.rgba(0, 1))
            assertEquals(listOf(0, 0, 255, 255), image.rgba(2, 1))
        }
        val gray = driver.loadImageSuspend(PNG_GRAY_3X3, null, false, null)
        assertEquals(ColorType.UINT8, gray.type)
        assertEquals(listOf(128, 128, 128, 255), gray.rgba(1, 1))
        val jpeg = driver.loadImageSuspend(JPEG_RGB_8X8, null, true, null)
        val (r, g, b) = jpeg.rgba(4, 4)
        assertTrue(kotlin.math.abs(r - 40) <= 3 && kotlin.math.abs(g - 120) <= 3 && kotlin.math.abs(b - 200) <= 3, "jpeg pixel $r $g $b")
    }

    @Test
    fun `images are encoded as PNG and JPEG data urls`() = runTest {
        if (!browserDecoding()) return@runTest
        val image = driver.loadImageSuspend(PNG_RGBA_4X2, null, true, null)
        val png = driver.imageToDataUrl(image, ImageFileFormat.PNG)
        assertTrue(png.startsWith("data:image/png;base64,"))
        // a round trip keeps the pixels and their orientation
        val decoded = driver.loadImageSuspend(png, null, true, null)
        for (x in 0 until 4) for (y in 0 until 2) {
            if (x == 2 && y == 0) continue
            assertEquals(image.rgba(x, y), decoded.rgba(x, y), "pixel $x, $y")
        }
        assertTrue(driver.imageToDataUrl(image, ImageFileFormat.JPG).startsWith("data:image/jpeg;base64,"))
    }
}

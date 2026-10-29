package org.openrndr.imagedriver.browser

import org.openrndr.dds.loadDDS
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.internal.CubemapImageData
import org.openrndr.internal.ImageData
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.ImageSaveConfiguration
import org.openrndr.internal.JpegImageSaveConfiguration
import org.openrndr.internal.PngImageSaveConfiguration
import org.openrndr.utils.buffer.MPPBuffer

class ImageDataBrowser(
    width: Int,
    height: Int,
    format: ColorFormat,
    type: ColorType,
    flipV: Boolean,
    data: MPPBuffer?,
    mipmapData: List<MPPBuffer> = emptyList()
) : ImageData(width, height, format, type, flipV, data, mipmapData) {
    override fun close() {}
}

class CubemapImageDataBrowser(
    width: Int,
    height: Int,
    format: ColorFormat,
    type: ColorType,
    mipmaps: Int,
    sides: List<MPPBuffer>,
    mipmapSides: List<MPPBuffer>
) : CubemapImageData(width, height, format, type, mipmaps, sides, mipmapSides) {
    override fun close() {}
}

/**
 * An [ImageDriver] for browsers. Browsers decode images asynchronously, PNG, JPEG, GIF, WebP and BMP images are loaded
 * with [loadImageSuspend], which decodes them to 8-bit RGBA with the bottom row first like the image driver on the JVM.
 * DDS images are also loaded synchronously. Images are encoded and saved as PNG or JPEG, saving downloads the image in
 * browsers.
 */
class ImageDriverBrowser : ImageDriver {

    override fun probeImage(fileOrUrl: String): ImageFileDetails? = probeImage(readFileSync(fileOrUrl), null, fileOrUrl)

    override fun probeImage(buffer: MPPBuffer, formatHint: ImageFileFormat?, name: String?): ImageFileDetails? =
        probeImageHeader(buffer)

    private fun isDds(buffer: MPPBuffer, formatHint: ImageFileFormat?) =
        formatHint == ImageFileFormat.DDS || imageFileFormat(buffer) == ImageFileFormat.DDS

    private fun loadDdsImage(buffer: MPPBuffer): ImageData {
        val dds = loadDDS(buffer)
        return ImageDataBrowser(dds.width, dds.height, dds.format, dds.type, dds.flipV, dds.image(0))
    }

    private fun synchronousLoadingError(name: String?): Nothing =
        error("browsers decode images asynchronously, load ${name ?: "the image"} with loadImageSuspend, only DDS images are loaded synchronously")

    override fun loadImage(fileOrUrl: String, formatHint: ImageFileFormat?, allowSRGB: Boolean, details: ImageFileDetails?): ImageData {
        if (formatHint != null && formatHint != ImageFileFormat.DDS) {
            synchronousLoadingError(fileOrUrl)
        }
        return loadImage(readFileSync(fileOrUrl), fileOrUrl, formatHint, allowSRGB, details)
    }

    override fun loadImage(
        buffer: MPPBuffer,
        name: String?,
        formatHint: ImageFileFormat?,
        allowSRGB: Boolean,
        details: ImageFileDetails?
    ): ImageData {
        if (!isDds(buffer, formatHint)) {
            synchronousLoadingError(name)
        }
        return loadDdsImage(buffer)
    }

    override suspend fun loadImageSuspend(
        fileOrUrl: String,
        formatHint: ImageFileFormat?,
        allowSRGB: Boolean,
        details: ImageFileDetails?
    ): ImageData = loadImageSuspend(readFile(fileOrUrl), formatHint, allowSRGB)

    /**
     * Loads an image from [buffer] without blocking
     */
    suspend fun loadImageSuspend(buffer: MPPBuffer, formatHint: ImageFileFormat? = null, allowSRGB: Boolean = true): ImageData {
        if (isDds(buffer, formatHint)) {
            return loadDdsImage(buffer)
        }
        val format = imageFileFormat(buffer)
        require(format != ImageFileFormat.EXR && format != ImageFileFormat.HDR) { "browsers do not decode $format images" }
        val decoded = decodeImage(buffer)
        return ImageDataBrowser(
            decoded.width,
            decoded.height,
            ColorFormat.RGBa,
            if (allowSRGB) ColorType.UINT8_SRGB else ColorType.UINT8,
            false,
            decoded.pixels
        )
    }

    override fun saveImage(imageData: ImageData, filename: String, configuration: ImageSaveConfiguration) {
        saveFile(encode(imageData, configuration), filename)
    }

    override fun imageToDataUrl(imageData: ImageData, formatHint: ImageFileFormat?): String =
        encode(imageData, if (formatHint == ImageFileFormat.JPG) JpegImageSaveConfiguration() else PngImageSaveConfiguration())

    private fun encode(imageData: ImageData, configuration: ImageSaveConfiguration): String {
        val (mimeType, quality) = when (configuration) {
            is PngImageSaveConfiguration -> "image/png" to -1.0
            is JpegImageSaveConfiguration -> "image/jpeg" to configuration.quality / 100.0
            else -> error("browsers encode PNG and JPEG images, not ${configuration::class.simpleName}")
        }
        return encodeDataUrl(rgbaTopDown(imageData), imageData.width, imageData.height, mimeType, quality)
    }

    override fun loadCubemapImage(fileOrUrl: String, formatHint: ImageFileFormat?): CubemapImageData =
        loadCubemapImage(readFileSync(fileOrUrl), fileOrUrl, formatHint)

    override fun loadCubemapImage(buffer: MPPBuffer, name: String?, formatHint: ImageFileFormat?): CubemapImageData {
        require(isDds(buffer, formatHint)) { "cubemaps are loaded from DDS images, ${name ?: "the image"} is not a DDS image" }
        val dds = loadDDS(buffer)
        require(dds.cubeMap) { "${name ?: "the image"} is not a cubemap" }
        require(dds.width == dds.height) { "the sides of a cubemap must be square" }
        return CubemapImageDataBrowser(dds.width, dds.height, dds.format, dds.type, dds.mipmaps, dds.bdata, dds.bdata2)
    }

    override fun createImageData(
        width: Int,
        height: Int,
        format: ColorFormat,
        type: ColorType,
        flipV: Boolean,
        buffer: MPPBuffer?
    ): ImageData = ImageDataBrowser(
        width, height, format, type, flipV,
        buffer ?: MPPBuffer.allocate(width * height * format.componentCount * type.componentSize)
    )
}

/**
 * Converts the pixels of [imageData] to 8-bit RGBA with the top row first, as canvases expect
 */
internal fun rgbaTopDown(imageData: ImageData): MPPBuffer {
    val source = imageData.data ?: error("no image data")
    val components = imageData.format.componentCount
    val componentSize = imageData.type.componentSize
    val readComponent: (Int) -> Int = when (imageData.type) {
        ColorType.UINT8, ColorType.UINT8_SRGB -> { offset -> source.getByteAt(offset) }
        ColorType.UINT16 -> { offset -> source.getShortAt(offset) shr 8 }
        ColorType.FLOAT32 -> { offset -> (source.getFloatAt(offset).coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt() }
        else -> error("encoding ${imageData.type} images is not supported")
    }
    val order = when (imageData.format) {
        ColorFormat.BGR, ColorFormat.BGRa -> intArrayOf(2, 1, 0, 3)
        else -> intArrayOf(0, 1, 2, 3)
    }
    val target = MPPBuffer.allocate(imageData.width * imageData.height * 4)
    val origin = source.position()
    for (row in 0 until imageData.height) {
        // the data holds the bottom row first, unless it is stored flipped
        val sourceRow = if (imageData.flipV) row else imageData.height - 1 - row
        for (x in 0 until imageData.width) {
            val pixel = origin + (sourceRow * imageData.width + x) * components * componentSize
            fun component(i: Int) = readComponent(pixel + order[i] * componentSize)
            val (r, g, b, a) = when (components) {
                1 -> component(0).let { listOf(it, it, it, 255) }
                2 -> component(0).let { listOf(it, it, it, component(1)) }
                3 -> listOf(component(0), component(1), component(2), 255)
                else -> listOf(component(0), component(1), component(2), component(3))
            }
            target.put(r.toByte()); target.put(g.toByte()); target.put(b.toByte()); target.put(a.toByte())
        }
    }
    source.position(origin)
    target.rewind()
    return target
}

private fun MPPBuffer.getByteAt(offset: Int): Int {
    position(offset)
    return get().toInt() and 0xff
}

private fun MPPBuffer.getShortAt(offset: Int): Int {
    position(offset)
    val low = get().toInt() and 0xff
    val high = get().toInt() and 0xff
    return (high shl 8) or low
}

private fun MPPBuffer.getFloatAt(offset: Int): Float {
    position(offset)
    var bits = 0
    for (i in 0 until 4) {
        bits = bits or ((get().toInt() and 0xff) shl (8 * i))
    }
    return Float.fromBits(bits)
}

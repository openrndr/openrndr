package org.openrndr.internal.gl3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.internal.CubemapImageData
import org.openrndr.internal.ExrImageSaveConfiguration
import org.openrndr.internal.HdrImageSaveConfiguration
import org.openrndr.internal.ImageData
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.ImageSaveConfiguration
import org.openrndr.internal.JpegImageSaveConfiguration
import org.openrndr.internal.PngImageSaveConfiguration
import org.openrndr.utils.buffer.MPPBuffer
import org.openrndr.utils.url.resolveFileOrUrl
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.io.path.Path
import kotlin.io.path.exists

private val logger = KotlinLogging.logger { }

/**
 * Android/GLES variant of ImageDataStb (no LWJGL).
 * Just frees its direct buffers by letting GC handle them.
 */
class ImageDataStbGLES(
    width: Int,
    height: Int,
    format: ColorFormat,
    type: ColorType,
    flipV: Boolean,
    data: MPPBuffer?,
    mipmapData: List<MPPBuffer> = emptyList()
) : ImageData(
    width, height, format, type, flipV, data, mipmapData
) {
    override fun close() {
        // Nothing explicit to free on Android (no MemoryUtil.memFree).
        // The direct buffers in MPPBuffer will be GC'd.
    }
}

/**
 * Android/GLES implementation using BitmapFactory and Bitmap.
 * Supports: PNG, JPG (8-bit). HDR/EXR/DDS are not supported here.
 */
class ImageDriverStbImageGLES : ImageDriver {

    // --- Utilities ----------------------------------------------------------

    private fun flipVertically(src: Bitmap): Bitmap {
        val m = android.graphics.Matrix().apply { preScale(1f, -1f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, false)
    }

    private fun rgbaByteBufferFromBitmap(bm: Bitmap, flipV: Boolean): ByteBuffer {
        // Ensure ARGB_8888
        val bmp = if (bm.config != Bitmap.Config.ARGB_8888) bm.copy(
            Bitmap.Config.ARGB_8888,
            false
        ) else bm

        val width = bmp.width
        val height = bmp.height
        val buf = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())

        val pixels = IntArray(width * height)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert ARGB -> RGBA; also handle flipV
        if (flipV) {
            for (y in 0 until height) {
                val row = (height - 1 - y) * width
                for (x in 0 until width) {
                    val p = pixels[row + x]
                    val a = (p ushr 24) and 0xff
                    val r = (p ushr 16) and 0xff
                    val g = (p ushr 8) and 0xff
                    val b = (p) and 0xff
                    buf.put(r.toByte()).put(g.toByte()).put(b.toByte()).put(a.toByte())
                }
            }
        } else {
            var idx = 0
            repeat(height) {
                repeat(width) {
                    val p = pixels[idx++]
                    val a = (p ushr 24) and 0xff
                    val r = (p ushr 16) and 0xff
                    val g = (p ushr 8) and 0xff
                    val b = (p) and 0xff
                    buf.put(r.toByte()).put(g.toByte()).put(b.toByte()).put(a.toByte())
                }
            }
        }

        (buf as Buffer).rewind()
        return buf
    }

    private fun bitmapFromRGBA(
        width: Int,
        height: Int,
        data: ByteBuffer,
        flipOut: Boolean
    ): Bitmap {
        // data is expected to be RGBA8, tightly packed
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        (data as Buffer).rewind()
        var i = 0
        repeat(height) {
            repeat(width) {
                val r = data.get().toInt() and 0xff
                val g = data.get().toInt() and 0xff
                val b = data.get().toInt() and 0xff
                val a = data.get().toInt() and 0xff
                pixels[i++] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        if (flipOut) {
            // If imageData.flipV is true (already top-left origin), we keep it.
            // If false, we flip vertically for file encoders that expect top-left origin.
            val flipped = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (y in 0 until height) {
                val srcRow = (height - 1 - y) * width
                flipped.setPixels(pixels, srcRow, width, 0, y, width, 1)
            }
            return flipped
        } else {
            bmp.setPixels(pixels, 0, width, 0, 0, width, height)
            return bmp
        }
    }

    // --- Probe --------------------------------------------------------------

    override fun probeImage(fileOrUrl: String): ImageFileDetails? {
        val (file, url) = resolveFileOrUrl(fileOrUrl)
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (file != null) {
                BitmapFactory.decodeFile(file.absolutePath, opts)
            } else if (url != null) {
                url.openStream().use { BitmapFactory.decodeStream(it, null, opts) }
            }
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                // Android does not expose channel count easily; assume 4 for ARGB_8888
                ImageFileDetails(opts.outWidth, opts.outHeight, 4)
            } else null
        } catch (e: Exception) {
            logger.warn(e) { "Failed to probe image '$fileOrUrl'" }
            null
        }
    }

    override fun probeImage(
        buffer: MPPBuffer,
        formatHint: ImageFileFormat?,
        name: String?
    ): ImageFileDetails? {
        val bytes = ByteArray(buffer.byteBuffer.remaining())
        buffer.byteBuffer.mark()
        buffer.byteBuffer.get(bytes)
        buffer.byteBuffer.reset()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return if (opts.outWidth > 0 && opts.outHeight > 0) {
            ImageFileDetails(opts.outWidth, opts.outHeight, 4)
        } else {
            logger.warn { "Failed to probe image '$name'" }
            null
        }
    }

    // --- Load ---------------------------------------------------------------

    override fun loadImage(
        fileOrUrl: String,
        formatHint: ImageFileFormat?,
        allowSRGB: Boolean,
        details: ImageFileDetails?
    ): ImageData {
        if (fileOrUrl.startsWith("data:")) {
            val commaIndex = fileOrUrl.indexOf(',')
            val base64Data = fileOrUrl.substring(commaIndex + 1).replace("\r", "").replace("\n", "")
            val decoded = Base64.getDecoder().decode(base64Data)
            return loadImage(
                MPPBuffer(ByteBuffer.wrap(decoded)),
                "data-url",
                formatHint,
                allowSRGB,
                details
            )
        }

        val url = try {
            URL(fileOrUrl)
        } catch (_: MalformedURLException) {
            null
        }
        return if (url != null) {
            url.openStream().use { input ->
                val bytes = input.readBytes()
                loadFromBytes(bytes, allowSRGB)
            }
        } else {
            require(Path(fileOrUrl).exists()) { "$fileOrUrl not found" }
            val bytes = File(fileOrUrl).readBytes()
            loadFromBytes(bytes, allowSRGB)
        }
    }

    override fun loadImage(
        buffer: MPPBuffer,
        name: String?,
        formatHint: ImageFileFormat?,
        allowSRGB: Boolean,
        details: ImageFileDetails?
    ): ImageData {
        val bytes = ByteArray(buffer.byteBuffer.remaining())
        buffer.byteBuffer.mark()
        buffer.byteBuffer.get(bytes)
        buffer.byteBuffer.reset()
        return loadFromBytes(bytes, allowSRGB)
    }

    private fun loadFromBytes(bytes: ByteArray, allowSRGB: Boolean): ImageData {
        // We decode always as ARGB_8888; then repack as RGBA into a direct ByteBuffer.
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: error("BitmapFactory.decodeByteArray failed")

        // ONDR prefers bottom-left origin; we’ll keep flipV=false and write RGBA in that coordinate system.
        val rgba = rgbaByteBufferFromBitmap(bmp, flipV = false)

        val format = when (opts.outMimeType?.lowercase()) {
            "image/jpeg" -> ColorFormat.RGBa // still RGBA packed (A = 255), we keep 4 channels to match pipeline
            "image/png" -> ColorFormat.RGBa
            else -> ColorFormat.RGBa
        }
        val type = if (allowSRGB) ColorType.UINT8_SRGB else ColorType.UINT8

        return ImageDataStbGLES(
            width = bmp.width,
            height = bmp.height,
            format = format,
            type = type,
            flipV = false,
            data = MPPBuffer(rgba)
        )
    }

    // --- Save ---------------------------------------------------------------

    override fun saveImage(
        imageData: ImageData,
        filename: String,
        configuration: ImageSaveConfiguration
    ) {
        when (configuration) {
            is PngImageSaveConfiguration -> {
                require(imageData.type == ColorType.UINT8 || imageData.type == ColorType.UINT8_SRGB) {
                    "Only 8-bit PNG is supported on Android path"
                }
                val bmp = bitmapFromRGBA(
                    imageData.width, imageData.height,
                    imageData.data?.byteBuffer ?: error("no data"),
                    // If flipV=false, we need to flip for file encoders that expect top-left origin
                    flipOut = !imageData.flipV
                )
                FileOutputStream(filename).use { out ->
                    require(bmp.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG save failed" }
                }
            }

            is JpegImageSaveConfiguration -> {
                require(imageData.type == ColorType.UINT8 || imageData.type == ColorType.UINT8_SRGB) {
                    "Only 8-bit JPEG is supported on Android path"
                }
                val bmp = bitmapFromRGBA(
                    imageData.width, imageData.height,
                    imageData.data?.byteBuffer ?: error("no data"),
                    flipOut = !imageData.flipV
                )
                FileOutputStream(filename).use { out ->
                    require(
                        bmp.compress(
                            Bitmap.CompressFormat.JPEG,
                            configuration.quality,
                            out
                        )
                    ) { "JPEG save failed" }
                }
            }

            is HdrImageSaveConfiguration,
            is ExrImageSaveConfiguration -> {
                error("HDR/EXR saving is not supported on the Android/GLES image driver")
            }

            else -> error("Unsupported save configuration: ${configuration::class.simpleName}")
        }
    }

    override fun imageToDataUrl(imageData: ImageData, formatHint: ImageFileFormat?): String {
        val fmt = formatHint ?: ImageFileFormat.JPG
        require(imageData.type == ColorType.UINT8 || imageData.type == ColorType.UINT8_SRGB) {
            "Only 8-bit toDataURL is supported on Android path"
        }

        val bmp = bitmapFromRGBA(
            imageData.width, imageData.height,
            imageData.data?.byteBuffer ?: error("no data"),
            flipOut = !imageData.flipV
        )

        val baos = ByteArrayOutputStream()
        when (fmt) {
            ImageFileFormat.JPG -> {
                require(bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)) { "JPEG encode failed" }
            }

            ImageFileFormat.PNG -> {
                require(bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)) { "PNG encode failed" }
            }

            else -> error("Unsupported format for data URL: $fmt")
        }

        val base64 = Base64.getEncoder().encodeToString(baos.toByteArray())
        return "data:${fmt.mimeType};base64,$base64"
    }

    // --- Cubemap (DDS) -----------------------------------------------------

    override fun loadCubemapImage(
        fileOrUrl: String,
        formatHint: ImageFileFormat?
    ): CubemapImageData {
        error("DDS/cubemap loading is not supported on the Android/GLES image driver")
    }

    override fun loadCubemapImage(
        buffer: MPPBuffer,
        name: String?,
        formatHint: ImageFileFormat?
    ): CubemapImageData {
        error("DDS/cubemap loading is not supported on the Android/GLES image driver")
    }

    override fun createImageData(
        width: Int,
        height: Int,
        format: ColorFormat,
        type: ColorType,
        flipV: Boolean,
        buffer: MPPBuffer?
    ): ImageData {
        TODO("Not yet implemented")
    }
}
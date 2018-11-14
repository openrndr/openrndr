package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer

/**
 * File format used while saving to file
 */
enum class FileFormat {
    JPG,
    PNG,
}

/**
 * Texture wrapping mode
 */
enum class WrapMode {
    CLAMP_TO_EDGE,
    REPEAT,
    MIRRORED_REPEAT
}

/**
 * Texture filters used for minification
 */
enum class MinifyingFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR
}

/**
 * Texture filters for magnification
 */
enum class MagnifyingFilter {
    NEAREST,
    LINEAR
}

sealed class BufferMultisample {
    object Disabled : BufferMultisample()
    data class SampleCount(val sampleCount: Int) : BufferMultisample()
}


interface ColorBuffer {
    val width: Int
    val height: Int
    val contentScale: Double
    val format: ColorFormat
    val type: ColorType
    val multisample: BufferMultisample

    val bounds: Rectangle get() = Rectangle(Vector2.ZERO, width * 1.0, height * 1.0)

    val effectiveWidth: Int get() = (width * contentScale).toInt()
    val effectiveHeight: Int get() = (height * contentScale).toInt()

    fun saveToFile(file: File, fileFormat: FileFormat = guessFromExtension(file))
    private fun guessFromExtension(file: File): FileFormat {
        val extension = file.extension.toLowerCase()
        return when (extension) {
            "jpg", "jpeg" -> FileFormat.JPG
            "png" -> FileFormat.PNG
            else -> throw IllegalArgumentException("unsupported format: \"$extension\"")
        }
    }

    fun destroy()
    fun bind(unit: Int)

    fun write(buffer: ByteBuffer)
    fun read(buffer: ByteBuffer)
    fun generateMipmaps()

    /**
     * resolves (or copies) to a non-multisampled color buffer
     */
    fun resolveTo(target: ColorBuffer)

    var wrapU: WrapMode
    var wrapV: WrapMode

    var filterMin: MinifyingFilter
    var filterMag: MagnifyingFilter

    val shadow: ColorBufferShadow
    var flipV: Boolean


    fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {
        this.filterMin = filterMin
        this.filterMag = filterMag
    }


    companion object {
        @Deprecated("use the colorBuffer() builder function instead")
        fun create(width: Int, height: Int,
                   contentScale: Double = 1.0,
                   format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8,
                   multisample: BufferMultisample = BufferMultisample.Disabled
        ) = Driver.instance.createColorBuffer(width, height, contentScale, format, type, multisample)

        fun fromUrl(url: String) = Driver.instance.createColorBufferFromUrl(url)

        fun fromFile(file: File) = Driver.instance.createColorBufferFromFile(file.absolutePath)

        fun fromFile(filename: String) = Driver.instance.createColorBufferFromFile(filename)
    }
}

fun colorBuffer(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, multisample:BufferMultisample = BufferMultisample.Disabled): ColorBuffer {
    return Driver.driver.createColorBuffer(width, height, contentScale, format, type, multisample)
}

fun loadImage(fileOrUrl: String): ColorBuffer {
    return try {
        URL(fileOrUrl)
        ColorBuffer.fromUrl(fileOrUrl)
    } catch (e: MalformedURLException) {
        ColorBuffer.fromFile(fileOrUrl)
    }
}

fun loadImage(file: File): ColorBuffer {
    return ColorBuffer.fromFile(file)
}

fun loadImage(url: URL): ColorBuffer {
    return ColorBuffer.fromUrl(url.toExternalForm())
}
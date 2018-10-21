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


interface ColorBuffer {
    val width: Int
    val height: Int
    val contentScale: Double
    val format: ColorFormat
    val type: ColorType

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
        fun create(width: Int, height: Int,
                   contentScale: Double = 1.0,
                   format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8
        ) = Driver.instance.createColorBuffer(width, height, contentScale, format, type)

        fun fromUrl(url: String) = Driver.instance.createColorBufferFromUrl(url)

        fun fromFile(file: File) = Driver.instance.createColorBufferFromFile(file.absolutePath)

        fun fromFile(filename: String) = Driver.instance.createColorBufferFromFile(filename)
    }
}

fun colorBuffer(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8): ColorBuffer {
    return ColorBuffer.create(width, height, contentScale, format, type)
}

fun loadImage(fileOrUrl: String): ColorBuffer {
    return try {
        val url = URL(fileOrUrl)
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
package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.io.File
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer

/**
 * File format used while saving to file
 */
enum class ImageFileFormat(val mimeType: String, val extensions: List<String>) {
    JPG("image/jpeg", listOf("jpg", "jpeg")),
    PNG("image/png", listOf("png")),
    EXR("image/x-exr", listOf("exr"));

    companion object {
        fun guessFromExtension(file: File): ImageFileFormat {
            return when (val extension = file.extension.toLowerCase()) {
                "jpg", "jpeg" -> JPG
                "png" -> PNG
                "exr" -> EXR
                else -> throw IllegalArgumentException("unsupported format: \"$extension\"")
            }
        }
    }
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
    /** nearest neighbour, blocky */
    NEAREST,
    LINEAR
}

/**
 * Buffer multisample options
 */
sealed class BufferMultisample {
    /**
     * Disable multisampling
     */
    object Disabled : BufferMultisample() {
        override fun toString(): String {
            return "Disabled()"
        }
    }

    /**
     * Enable multisampling with a given [sampleCount]
     */
    data class SampleCount(val sampleCount: Int) : BufferMultisample()


}

/**
 * Color Buffer, a GPU resource
 */
interface ColorBuffer {
    val session: Session?

    /** the width of the [ColorBuffer] in device units */
    val width: Int

    /** the height of the [ColorBuffer] in device units */
    val height: Int

    /** the content scale of the [ColorBuffer] */
    val contentScale: Double
    val format: ColorFormat
    val type: ColorType

    /** the number of mipmap levels */
    val levels: Int

    /** the multisampling method used for this [ColorBuffer] */
    val multisample: BufferMultisample

    val bounds: Rectangle get() = Rectangle(Vector2.ZERO, width * 1.0, height * 1.0)

    /** the width of the [ColorBuffer] in pixels */
    val effectiveWidth: Int get() = (width * contentScale).toInt()

    /** the height of the [ColorBuffer] in pixels */
    val effectiveHeight: Int get() = (height * contentScale).toInt()

    /** save the [ColorBuffer] to [File] */
    fun saveToFile(file: File, imageFileFormat: ImageFileFormat = ImageFileFormat.guessFromExtension(file), async: Boolean = true)

    /** returns a base64 data url representation */
    fun toDataUrl(imageFileFormat: ImageFileFormat = ImageFileFormat.JPG): String

    /** destroys the underlying [ColorBuffer] resources */
    fun destroy()

    /** binds the colorbuffer to a texture unit, internal API */
    fun bind(unit: Int)

    fun write(buffer: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type, level: Int = 0)
    fun read(buffer: ByteBuffer, targetFormat: ColorFormat = format, targetType: ColorType = type, level: Int = 0)

    /** generates mipmaps from the top-level mipmap */
    fun generateMipmaps()

    /**
     * resolves contents to a non-multisampled color buffer
     */
    fun resolveTo(target: ColorBuffer, fromLevel: Int = 0, toLevel: Int = 0)

    /**
     * copies contents to a target color buffer
     * @param target the color buffer to which contents will be copied
     * @param fromLevel the mip-map level from which will be copied
     * @param toLevel the mip-map level of [target] to which will be copied
     */
    fun copyTo(target: ColorBuffer, fromLevel: Int = 0, toLevel: Int = 0)

    /**
     * copies contents to a target array texture
     * @param target the color buffer to which contents will be copied
     * @param layer the array layer from which will be copied
     * @param fromLevel the mip-map level from which will be copied
     * @param toLevel the mip-map level of [target] to which will be copied
     */
    fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int = 0, toLevel: Int = 0)

    /**
     * sets every pixel in the color buffer to [color]
     * @param color the color used for filling
     */
    fun fill(color: ColorRGBa)

    /** the wrapping mode to use in the horizontal direction */
    var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    var wrapV: WrapMode

    /** the filter to use when displaying at sizes smaller than the original */
    var filterMin: MinifyingFilter

    /** the filter to use when display at sizes larger than the original */
    var filterMag: MagnifyingFilter

    val shadow: ColorBufferShadow

    /** the (unitless?) degree of anisotropy to be used in filtering */
    var anisotropy: Double

    /**
     * should the v coordinate be flipped because the [ColorBuffer] contents are stored upside-down?
     */
    var flipV: Boolean

    /**
     * sets the [ColorBuffer] filter for minifying and magnification
     */
    fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {
        this.filterMin = filterMin
        this.filterMag = filterMag
    }

    /**
     * Checks if this [ColorBuffer] is equivalent to [other]
     * @param other the [ColorBuffer] to check against
     * @param ignoreLevels ignores [levels] in check when true
     * @param ignoreMultisample ignores [multisample] in check when true
     */
    fun isEquivalentTo(other: ColorBuffer,
                       ignoreWidth: Boolean = false,
                       ignoreHeight: Boolean = false,
                       ignoreContentScale: Boolean = false,
                       ignoreFormat: Boolean = false,
                       ignoreType: Boolean = false,
                       ignoreMultisample: Boolean = false,
                       ignoreLevels: Boolean = false): Boolean {
        return (ignoreWidth || width == other.width) &&
                (ignoreHeight || height == other.height) &&
                (ignoreContentScale || contentScale == other.contentScale) &&
                (ignoreFormat || format == other.format) &&
                (ignoreType || type == other.type) &&
                (ignoreMultisample || multisample == other.multisample) &&
                (ignoreLevels || levels == other.levels)
    }

    /**
     * Create an equivalent [ColorBuffer], with the option to override attributes
     */
    fun createEquivalent(width: Int = this.width,
                         height: Int = this.height,
                         contentScale: Double = this.contentScale,
                         format: ColorFormat = this.format,
                         type: ColorType = this.type,
                         multisample: BufferMultisample = this.multisample,
                         levels: Int = this.levels): ColorBuffer {
        return colorBuffer(width, height, contentScale, format, type, multisample, levels)
    }

    companion object {
        fun fromUrl(url: String, session: Session? = Session.active): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromUrl(url, session)
            return colorBuffer
        }

        fun fromFile(file: File, session: Session? = Session.active): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromFile(file.absolutePath, session)
            return colorBuffer
        }

        fun fromFile(filename: String, session: Session? = Session.active): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromFile(filename, session)
            return colorBuffer
        }

        @Suppress("UNUSED_PARAMETER")
        fun fromStream(stream: InputStream, formatHint: String? = null, session: Session? = Session.active): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromStream(stream, session = session)
            return colorBuffer
        }

        @Suppress("UNUSED_PARAMETER")
        fun fromArray(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size, session: Session? = Session.active): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromArray(bytes, session = session)
            return colorBuffer
        }

        fun fromBuffer(bytes: ByteBuffer, session: Session? = Session.active): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromBuffer(bytes, session = session)
            return colorBuffer
        }
    }


}


/**
 * creates a [ColorBuffer]
 * @param width the width in device units
 * @param height the height in device units
 * @param contentScale content scale used for denoting hi-dpi content
 * @param format the color format
 * @param type the color type
 * @param format the color format
 * @param levels the number of mip-map levels
 * @param session the [Session] that should track this color buffer
 */
fun colorBuffer(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, multisample: BufferMultisample = BufferMultisample.Disabled, levels: Int = 1, session: Session? = Session.active): ColorBuffer {
    return Driver.instance.createColorBuffer(width, height, contentScale, format, type, multisample, levels, session)
}

/**
 * loads an image from a file or url encoded as [String], also accepts base64 encoded dat urls
 */
fun loadImage(fileOrUrl: String, session: Session? = Session.active): ColorBuffer {
    return try {
        if (!fileOrUrl.startsWith("data:")) {
            URL(fileOrUrl)
        }
        ColorBuffer.fromUrl(fileOrUrl, session)
    } catch (e: MalformedURLException) {
        loadImage(File(fileOrUrl), session)
    }
}

/**
 * loads an image from [file]
 */
fun loadImage(file: File, session: Session? = Session.active): ColorBuffer {
    require(file.exists()) {
        "failed to load image: file '${file.absolutePath}' does not exist."
    }
    return ColorBuffer.fromFile(file, session)
}

/**
 * loads an image from an [url]
 */
fun loadImage(url: URL, session: Session? = Session.active): ColorBuffer {
    return ColorBuffer.fromUrl(url.toExternalForm(), session)
}


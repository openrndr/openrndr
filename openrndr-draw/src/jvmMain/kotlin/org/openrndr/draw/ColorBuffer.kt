@file:JvmName("ColorBufferJVM")
package org.openrndr.draw

import kotlinx.coroutines.runBlocking
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer
import java.io.File
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer


/**
 * representation for simple images stored on GPU memory
 *
 * [ColorBuffer] is a unmanaged GPU resource, the user is responsible for destroying a [ColorBuffer] once it is no
 * longer used.
 */
actual abstract class ColorBuffer {
    actual abstract val session: Session?

    /** the width of the [ColorBuffer] in device units */
    actual abstract val width: Int

    /** the height of the [ColorBuffer] in device units */
    actual abstract val height: Int

    /** the content scale of the [ColorBuffer] */
    actual abstract val contentScale: Double

    /**
     * the [ColorFormat] of the image stored in the [ColorBuffer]
     */
    actual abstract val format: ColorFormat

    /**
     * the [ColorType] of the image stored in the [ColorBuffer]
     */
    actual abstract val type: ColorType

    /** the number of mipmap levels */
    actual abstract val levels: Int

    /** the multisampling method used for this [ColorBuffer] */
    actual abstract val multisample: BufferMultisample

    actual val bounds: Rectangle get() = Rectangle(Vector2.ZERO, width * 1.0, height * 1.0)
    actual val effectiveWidth: Int get() = (width * contentScale).toInt()

    actual val effectiveHeight: Int get() = (height * contentScale).toInt()

    /** return the buffer size in bytes */
    fun bufferSize(level: Int = 0): Long {
        val baseSize = ((effectiveWidth * effectiveHeight) shr level).toLong()
        return when (type) {
            ColorType.DXT1 -> (baseSize) / 2
            ColorType.DXT3, ColorType.DXT5 -> baseSize
            ColorType.BPTC_FLOAT -> TODO()
            ColorType.BPTC_UFLOAT -> TODO()
            ColorType.BPTC_UNORM -> TODO()
            else -> baseSize * format.componentCount * type.componentSize
        }
    }

    /** save the [ColorBuffer] to [File] */
    abstract fun saveToFile(
        file: File,
        imageFileFormat: ImageFileFormat = ImageFileFormat.guessFromExtension(file.extension),
        async: Boolean = true
    )

    /** return a base64 data url representation */
    abstract fun toDataUrl(imageFileFormat: ImageFileFormat = ImageFileFormat.JPG): String
    abstract fun write(
        sourceBuffer: ByteBuffer,
        sourceFormat: ColorFormat = format,
        sourceType: ColorType = type,
        level: Int = 0
    )

    /**
     * read the contents of the [ColorBuffer] and write to [targetBuffer], potentially with format and type conversions
     * @param targetBuffer a [ByteBuffer] to which the contents of the [ColorBuffer] will be written
     * @param targetFormat the [ColorFormat] that is used for the image data stored in [targetBuffer], default is [ColorBuffer.format]
     * @param targetType the [ColorType] that is used for the image data stored in [targetBuffer], default is [ColorBuffer.type]
     * @param level the mipmap-level of [ColorBuffer] to read from
     */
    abstract fun read(targetBuffer: ByteBuffer, targetFormat: ColorFormat = format, targetType: ColorType = type, level: Int = 0)

    /**
     * create a cropped copy of the [ColorBuffer]
     * @param sourceRectangle
     */
    fun crop(sourceRectangle: IntRectangle): ColorBuffer {
        val cropped = createEquivalent(width = sourceRectangle.width, height = sourceRectangle.height)
        copyTo(
            cropped,
            0, 0,
            sourceRectangle, IntRectangle(0, 0, sourceRectangle.width, sourceRectangle.height), MagnifyingFilter.NEAREST
        )
        return cropped
    }

    /**
     * copies contents to a target color buffer
     * @param target the color buffer to which contents will be copied
     * @param fromLevel the mip-map level from which will be copied
     * @param toLevel the mip-map level of [target] to which will be copied
     * @param sourceRectangle rectangle in pixel units that specifies where to read from
     * @param targetRectangle rectangle in pixel units that specifies where to write to
     * @param filter filter to use for copying
     */
    actual abstract fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    )

    actual abstract fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        filter: MagnifyingFilter
    )

    actual abstract fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int, toLevel: Int)

    actual abstract fun fill(color: ColorRGBa)

    /** the wrapping mode to use in the horizontal direction */
    actual abstract var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    actual abstract var wrapV: WrapMode

    /** the filter to use when displaying at sizes smaller than the original */
    abstract var filterMin: MinifyingFilter
    /** the filter to use when display at sizes larger than the original */
    abstract var filterMag: MagnifyingFilter

    abstract val shadow: ColorBufferShadow

    /**
     * sets the [ColorBuffer] filter for minifying and magnification
     */
    actual abstract fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter)

    companion object {
        /**
         * create a [ColorBuffer] from a [File] containing a formatted image
         * @param url the location of a formatted image
         * @param formatHint an optional [ImageFileFormat] hint, default is null
         * @param session the [Session] under which the [ColorBuffer] should be created, default is [Session.active]
         * @see loadImage
         */
        fun fromUrl(url: String, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer {
            return runBlocking {
                Driver.instance.createColorBufferFromUrl(url, formatHint, session)
            }
        }

        /**
         * create a [ColorBuffer] from a [File] containing a formatted image
         * @param file a [File] containing a formatted image
         * @param formatHint an optional [ImageFileFormat] hint, default is null
         * @param session the [Session] under which the [ColorBuffer] should be created, default is [Session.active]
         * @see loadImage
         */
        fun fromFile(file: File, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer {
            return runBlocking {
                Driver.instance.createColorBufferFromFile(file.absolutePath, formatHint = formatHint, session)
            }
        }

        /**
         * create a [ColorBuffer] from a file indicated by [filename] containing a formatted image
         * @param filename a file containing a formatted image
         * @param formatHint an optional [ImageFileFormat] hint, default is null
         * @param session the [Session] under which the [ColorBuffer] should be created, default is [Session.active]
         * @see loadImage
         */
        fun fromFile(filename: String, formatHint: ImageFileFormat?, session: Session? = Session.active): ColorBuffer {
            return runBlocking {
                Driver.instance.createColorBufferFromFile(filename, formatHint = formatHint, session)
            }
        }

        /**
         * create a [ColorBuffer] from an [InputStream] containing a formatted image
         * @param stream an [InputStream] holding a formatted image
         * @param formatHint optional [ImageFileFormat] hint, default is null
         * @param session the [Session] under which to create this [ColorBuffer]
         */
        @Suppress("UNUSED_PARAMETER")
        fun fromStream(
            stream: InputStream,
            formatHint: ImageFileFormat? = null,
            session: Session? = Session.active
        ): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromStream(
                stream,
                formatHint = formatHint,
                session = session
            )
            return colorBuffer
        }

        /**
         * create a [ColorBuffer] from a [ByteArray] containing a formatted image (meaning any of the formats in [ImageFileFormat])
         * @param bytes a [ByteArray] containing a formatted image
         * @param offset offset used for reading from [bytes], default is 0
         * @param length number of bytes to be used from [bytes], default is [bytes.size]
         * @param formatHint an optional [ImageFileFormat] hint, default is null
         */
        @Suppress("UNUSED_PARAMETER")
        fun fromArray(
            bytes: ByteArray,
            offset: Int = 0,
            length: Int = bytes.size,
            formatHint: ImageFileFormat?,
            session: Session? = Session.active
        ): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromArray(
                bytes,
                offset,
                length,
                formatHint = formatHint,
                session = session,
                name = null
            )
            return colorBuffer
        }

        /**
         * create a [ColorBuffer] from a [ByteBuffer] holding a formatted image (meaning any of the formats in [ImageFileFormat]
         * @param bytes a [ByteBuffer] containing a formatted image
         * @param formatHint an optional [ImageFileFormat] hint
         * @param session the [Session] under which this [ColorBuffer] should be created, default is [Session.active]
         */
        fun fromBuffer(
            bytes: ByteBuffer,
            formatHint: ImageFileFormat?,
            session: Session? = Session.active
        ): ColorBuffer {
            val colorBuffer = Driver.instance.createColorBufferFromBuffer(
                bytes,
                formatHint = formatHint,
                session = session
            )
            return colorBuffer
        }
    }

    /** permanently destroy the underlying [ColorBuffer] resources, [ColorBuffer] can not be used after it is destroyed */
    actual abstract fun destroy()

    /** bind the colorbuffer to a texture unit, internal API */
    actual abstract fun bind(unit: Int)

    /** generates mipmaps from the top-level mipmap */
    actual abstract fun generateMipmaps()

    /** the (unitless?) degree of anisotropy to be used in filtering */
    actual abstract var anisotropy: Double

    /**
     * should the v coordinate be flipped because the [ColorBuffer] contents are stored upside-down?
     */
    actual abstract var flipV: Boolean


    actual abstract fun write(
        sourceBuffer: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    )
}

/**
 * load an image from a file or url encoded as [String], also accepts base64 encoded data urls
 */
actual fun loadImage(fileOrUrl: String, formatHint: ImageFileFormat?, session: Session?): ColorBuffer {
    return try {
        if (!fileOrUrl.startsWith("data:")) {
            URL(fileOrUrl)
        }
        ColorBuffer.fromUrl(fileOrUrl, formatHint, session)
    } catch (e: MalformedURLException) {
        loadImage(File(fileOrUrl), formatHint, session)
    }
}

/**
 * load an image from [File]
 */
fun loadImage(file: File, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer {
    require(file.exists()) {
        "failed to load image: file '${file.absolutePath}' does not exist."
    }
    try {
        return ColorBuffer.fromFile(file, formatHint, session)
    } catch(e: Throwable) {
        throw RuntimeException("failed to load image: file '${file.absolutePath}'", e)
    }
}

/**
 * load an image from an [url]
 */
fun loadImage(url: URL, formatHint: ImageFileFormat?, session: Session? = Session.active): ColorBuffer {
    try {
        return ColorBuffer.fromUrl(url.toExternalForm(), formatHint, session)
    } catch(e: Throwable) {
        throw RuntimeException("failed to load image: url '${url.toExternalForm()}'", e)
    }
}

actual suspend fun loadImageSuspend(
    fileOrUrl: String,
    formatHint: ImageFileFormat?,
    session: Session?
): ColorBuffer {
    return runBlocking {
        loadImage(fileOrUrl, formatHint, session)
    }
}
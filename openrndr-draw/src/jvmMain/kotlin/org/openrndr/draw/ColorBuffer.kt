@file:JvmName("ColorBufferJVM")

package org.openrndr.draw

import kotlinx.coroutines.runBlocking
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.ImageSaveConfiguration
import org.openrndr.internal.ImageSaveContext
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer

import java.io.File
import java.net.URL
import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.min


/**
 * representation for simple images stored on GPU memory
 *
 * [ColorBuffer] is an unmanaged GPU resource, the user is responsible for destroying a [ColorBuffer] once it is no
 * longer used.
 */
actual abstract class ColorBuffer: AutoCloseable {
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
        imageFileFormat: ImageFileFormat = ImageFileFormat.guessFromExtension(file.extension) ?: ImageFileFormat.PNG,
        async: Boolean = true
    )

    /** save the [ColorBuffer] to [File] */
    abstract fun saveToFile(
        file: File,
        async: Boolean = true,
        configuration: ImageSaveConfiguration
    )

    /** save the [ColorBuffer] to [File] */
    fun saveToFile(
        file: File,
        async: Boolean = true,
        configurator: ImageSaveContext.() -> ImageSaveConfiguration
    ) {
        saveToFile(file, async, ImageSaveContext().configurator())
    }

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
    abstract fun read(
        targetBuffer: ByteBuffer,
        targetFormat: ColorFormat = format,
        targetType: ColorType = type,
        level: Int = 0
    )

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

    actual abstract fun fill(color: ColorRGBa, level: Int)

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

    companion object;

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
actual fun loadImage(
    fileOrUrl: String,
    formatHint: ImageFileFormat?,
    allowSRGB: Boolean,
    loadMipmaps: Boolean,
    session: Session?
): ColorBuffer {
    val data = ImageDriver.instance.loadImage(fileOrUrl, formatHint, allowSRGB)
    return try {
        val size = min(data.width, data.height)
        val levels = if (loadMipmaps) floor(log2(size.toDouble())).toInt() + 1 else 1
        val cb = colorBuffer(data.width, data.height, 1.0, data.format, data.type, levels = levels, session = session)
        cb.write(data.data?.byteBuffer ?: error("no data"))
        if (loadMipmaps) {
            cb.generateMipmaps()
        }
        cb
    } finally {
        data.close()
    }
}


/**
 * load an image from [File]
 */
fun loadImage(
    file: File,
    formatHint: ImageFileFormat? = ImageFileFormat.guessFromExtension(file.extension),
    allowSRGB: Boolean = true,
    loadMipmaps: Boolean = true,
    session: Session? = Session.active
): ColorBuffer {
    return loadImage(file.absolutePath, formatHint, allowSRGB, loadMipmaps, session)
}

/**
 * load an image from an [url]
 */
fun loadImage(
    url: URL,
    formatHint: ImageFileFormat? = ImageFileFormat.guessFromExtension(url.toExternalForm().split(".").lastOrNull()),
    allowSRGB: Boolean = true,
    loadMipmaps: Boolean = true,
    session: Session? = Session.active
): ColorBuffer {
    return loadImage(url.toExternalForm(), formatHint, allowSRGB, loadMipmaps, session)
}

actual suspend fun loadImageSuspend(
    fileOrUrl: String,
    formatHint: ImageFileFormat?,
    allowSRGB: Boolean,
    session: Session?
): ColorBuffer {
    return runBlocking {
        loadImage(fileOrUrl, formatHint, allowSRGB, true, session)
    }
}
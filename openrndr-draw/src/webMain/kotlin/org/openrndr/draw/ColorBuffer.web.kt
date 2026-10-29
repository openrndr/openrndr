package org.openrndr.draw

import js.buffer.ArrayBufferLike
import js.buffer.ArrayBufferView
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.ImageData
import org.openrndr.internal.ImageDriver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.TexImageSource
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max

actual abstract class ColorBuffer : Texture, AutoCloseable {
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

    /** the width of the [ColorBuffer] in pixels */
    actual val effectiveWidth: Int
        get() = (width * contentScale).toInt()

    /** the height of the [ColorBuffer] in pixels */
    actual val effectiveHeight: Int
        get() = (height * contentScale).toInt()

    actual val bounds: Rectangle
        get() = Rectangle(0.0, 0.0, width.toDouble(), height.toDouble())

    /** permanently destroy the underlying [ColorBuffer] resources, [ColorBuffer] can not be used after it is destroyed */
    actual abstract fun destroy()

    /** generates mipmaps from the top-level mipmap */
    actual abstract fun generateMipmaps()

    /** the (unitless?) degree of anisotropy to be used in filtering */
    actual abstract var anisotropy: Double

    /**
     * should the v coordinate be flipped because the [ColorBuffer] contents are stored upside-down?
     */
    actual abstract var flipV: Boolean
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

    /**
     * copies contents to a target array texture
     * @param target the color buffer to which contents will be copied
     * @param layer the array layer from which will be copied
     * @param fromLevel the mip-map level from which will be copied
     * @param toLevel the mip-map level of [target] to which will be copied
     */
    actual abstract fun copyTo(
        target: ArrayTexture,
        layer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    abstract fun write(
        source: TexImageSource,
        x: Int = 0,
        y: Int = 0,
        width: Int = this.effectiveWidth,
        height: Int = this.effectiveHeight,
        level: Int = 0
    )

    abstract fun write(
        source: ArrayBufferView<ArrayBufferLike>,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int = 0,
        y: Int = 0,
        width: Int = this.effectiveWidth,
        height: Int = this.effectiveHeight,
        level: Int = 0
    )

    abstract fun read(
        target: ArrayBufferView<ArrayBufferLike>,
        x: Int,
        y: Int,
        width: Int = this.effectiveWidth,
        height: Int = this.effectiveHeight,
        level: Int = 0
    )

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

    actual abstract fun filter(
        filterMin: MinifyingFilter,
        filterMag: MagnifyingFilter
    )

    /** the wrapping mode to use in the horizontal direction */
    actual abstract var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    actual abstract var wrapV: WrapMode

    /**
     * sets all pixels in the color buffer to [color]
     * @param color the color used for filling
     */
    actual abstract fun fill(color: ColorRGBa, level: Int)

}

/**
 * Load an image from a file or url encoded as [String], also accepts base64 encoded data urls
 * Usage example:
 * ```
 * program {
 *     val image = loadImage("data/images/cheeta.jpg")
 *     extend {
 *         drawer.image(image)
 *     }
 * }
 * ```
 */
actual fun loadImage(
    fileOrUrl: String,
    formatHint: ImageFileFormat?,
    allowSRGB: Boolean,
    loadMipmaps: Boolean,
    session: Session?
): ColorBuffer {
    return colorBufferFromImageData(ImageDriver.instance.loadImage(fileOrUrl, formatHint, allowSRGB, null), loadMipmaps, session)
}

/**
 * Load an image without blocking. In browsers images are decoded asynchronously, which [loadImage] can only do for
 * DDS images.
 */
actual suspend fun loadImageSuspend(
    fileOrUrl: String,
    formatHint: ImageFileFormat?,
    allowSRGB: Boolean,
    session: Session?
): ColorBuffer {
    return colorBufferFromImageData(ImageDriver.instance.loadImageSuspend(fileOrUrl, formatHint, allowSRGB, null), true, session)
}

/**
 * Creates a color buffer holding the image in [data], with mipmaps when [loadMipmaps] is true and [data] has none
 */
private fun colorBufferFromImageData(data: ImageData, loadMipmaps: Boolean, session: Session?): ColorBuffer {
    return try {
        val generateMipmaps = loadMipmaps && data.mipmapData.isEmpty() && !data.type.compressed
        val levels = when {
            data.mipmapData.isNotEmpty() -> data.mipmapData.size + 1
            generateMipmaps -> floor(log2(max(data.width, data.height).toDouble())).toInt() + 1
            else -> 1
        }
        val cb = colorBuffer(data.width, data.height, 1.0, data.format, data.type, BufferMultisample.Disabled, levels, session)
        cb.write(data.data ?: error("no image data"), data.format, data.type)
        for ((index, mipmap) in data.mipmapData.withIndex()) {
            val level = index + 1
            cb.write(mipmap, data.format, data.type, 0, 0, max(1, data.width shr level), max(1, data.height shr level), level)
        }
        if (generateMipmaps) {
            cb.generateMipmaps()
        }
        cb.flipV = data.flipV
        cb
    } finally {
        data.close()
    }
}
package org.openrndr.draw

import js.buffer.ArrayBufferLike
import js.buffer.ArrayBufferView
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.ImageDriver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.TexImageSource

actual abstract class ColorBuffer : AutoCloseable {

    /**
     * write the contents from [sourceBuffer] to the [ColorBuffer], potentially with format and type conversions
     *
     * The [sourceBuffer] should be allocated using [ByteBuffer.allocateDirect] and have an amount of remaining bytes
     * that matches with the dimensions, [sourceFormat] and [sourceType].
     * @param sourceBuffer a [ByteBuffer] holding raw image data
     * @param sourceFormat the [ColorFormat] that is used for the image data stored in [sourceBuffer], default is [ColorBuffer.format]
     * @param sourceType the [ColorType] that is used for the image data stored in [sourceBuffer], default is [ColorBuffer.type]
     * @param level the mipmap-level of [ColorBuffer] to write to
     */
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
 * load an image from a file or url encoded as [String], also accepts base64 encoded data urls
 */
actual fun loadImage(
    fileOrUrl: String,
    formatHint: ImageFileFormat?,
    allowSRGB: Boolean,
    loadMipmaps: Boolean,
    session: Session?
): ColorBuffer {
    val data = ImageDriver.instance.loadImage(fileOrUrl, formatHint, allowSRGB, null)
    return try {
        val cb = colorBuffer(
            data.width,
            data.height,
            1.0,
            data.format,
            data.type,
            BufferMultisample.Disabled,
            data.mipmapData.size + 1,
            session
        )

        cb
    } finally {
        data.close()
    }
}

actual suspend fun loadImageSuspend(
    fileOrUrl: String,
    formatHint: ImageFileFormat?,
    allowSRGB: Boolean,
    session: Session?
): ColorBuffer {
    return loadImage(fileOrUrl, formatHint, allowSRGB, true, session)
}
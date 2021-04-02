package org.openrndr.draw

import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle

actual abstract class ColorBuffer {


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
        get() = TODO("Not yet implemented")

    /** the height of the [ColorBuffer] in pixels */
    actual val effectiveHeight: Int
        get() = TODO("Not yet implemented")
    actual val bounds: Rectangle
        get() = TODO("Not yet implemented")

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


}
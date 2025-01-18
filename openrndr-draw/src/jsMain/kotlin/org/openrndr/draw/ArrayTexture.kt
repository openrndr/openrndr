package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle

actual abstract class ArrayTexture: AutoCloseable {
    actual abstract val session: Session?
    actual abstract val width: Int
    actual abstract val height: Int
    actual abstract val layers: Int
    actual abstract val format: ColorFormat
    actual abstract val type: ColorType
    actual abstract val levels: Int
    actual abstract fun destroy()
    actual abstract fun bind(unit: Int)


    actual abstract fun fill(color: ColorRGBa, layer: Int, level: Int)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the target to copy contents to
     */
    actual abstract fun copyTo(
        layer: Int,
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int
    )

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the layer array texture to copy contents to
     * @param targetLayer the layer of the target array texture to copy contents to
     */
    actual abstract fun copyTo(
        layer: Int,
        target: ArrayTexture,
        targetLayer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    /** generates mipmaps from the top-level mipmap */
    actual abstract fun generateMipmaps()

    /** the wrapping mode to use in the horizontal direction */
    actual abstract var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    actual abstract var wrapV: WrapMode

    /** the filter to use when displaying at sizes smaller than the original */
    actual abstract var filterMin: MinifyingFilter

    /** the filter to use when displaying at sizes larger than the original */
    actual abstract var filterMag: MagnifyingFilter
    actual abstract var flipV: Boolean
    actual val bounds: Rectangle
        get() = TODO("Not yet implemented")


}
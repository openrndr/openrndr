package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.shape.Rectangle

/**
 * Represents a texture with multiple layers, commonly referred to as an array texture.
 * This abstract class defines properties and operations essential for managing the texture,
 * including binding, filling, copying contents, and mipmap generation.
 */
expect abstract class ArrayTexture {
    abstract val session: Session?

    abstract val width: Int
    abstract val height: Int
    abstract val layers: Int
    abstract val format: ColorFormat
    abstract val type: ColorType
    abstract val levels: Int
    abstract fun destroy()

    abstract fun bind(unit: Int)

    abstract fun fill(color: ColorRGBa, layer: Int, level: Int = 0)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the target to copy contents to
     */
    abstract fun copyTo(layer: Int, target: ColorBuffer, fromLevel: Int = 0, toLevel: Int = 0)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the layer array texture to copy contents to
     * @param targetLayer the layer of the target array texture to copy contents to
     */
    abstract fun copyTo(layer: Int, target: ArrayTexture, targetLayer: Int, fromLevel: Int = 0, toLevel: Int = 0)


    /** generates mipmaps from the top-level mipmap */
    abstract fun generateMipmaps()

    /** the wrapping mode to use in the horizontal direction */
    abstract var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    abstract var wrapV: WrapMode

    /** the filter to use when displaying at sizes smaller than the original */
    abstract var filterMin: MinifyingFilter

    /** the filter to use when displaying at sizes larger than the original */
    abstract var filterMag: MagnifyingFilter

    abstract var flipV: Boolean

    val bounds: Rectangle

}


/**
 * Creates an array texture
 * @param width the width of each layer
 * @param height the height of each layer
 * @param layers the number of layers
 * @param format the color format (ColorFormat) to be used in each layer
 * @param type the color type to be used in each layer
 */
fun arrayTexture(
    width: Int,
    height: Int,
    layers: Int,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = defaultColorType(format),
    levels: Int = 1,
    session: Session = Session.active
): ArrayTexture {
    return Driver.instance.createArrayTexture(width, height, layers, format, type, levels).apply {
        session.track(this)
    }
}
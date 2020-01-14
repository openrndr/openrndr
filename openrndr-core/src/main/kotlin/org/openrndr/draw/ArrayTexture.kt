package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer

interface ArrayTexture {
    val session: Session?

    val width: Int
    val height: Int
    val layers: Int
    val format: ColorFormat
    val type: ColorType
    fun destroy()

    fun bind(unit: Int)

    /**
     * Write to a single layer in the array texture
     * @param layer the layer index to write to
     * @param buffer the ByteBuffer from which texture data is read
     * @param sourceFormat the format of the data in buffer
     * @param sourceType the type of the data in buffer
     */
    fun write(layer: Int, buffer: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type, level: Int = 0)

    fun read(layer: Int, buffer: ByteBuffer, level: Int = 0)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the target to copy contents to
     */
    fun copyTo(layer: Int, target: ColorBuffer, fromLevel: Int = 0, toLevel: Int = 0)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the layer array texture to copy contents to
     * @param targetLayer the layer of the target array texture to copy contents to
     */
    fun copyTo(layer: Int, target: ArrayTexture, targetLayer: Int, fromLevel: Int = 0, toLevel: Int = 0)


    /** generates mipmaps from the top-level mipmap */
    fun generateMipmaps()

    /** the wrapping mode to use in the horizontal direction */
    var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    var wrapV: WrapMode

    /** the filter to use when displaying at sizes smaller than the original */
    var filterMin: MinifyingFilter

    /** the filter to use when displaying at sizes larger than the original */
    var filterMag: MagnifyingFilter

    var flipV: Boolean

    val bounds: Rectangle get() = Rectangle(Vector2.ZERO, width * 1.0, height * 1.0)

}

/**
 * Creates an array texture
 * @param width the width of each layer
 * @param height the height of each layer
 * @param layers the number of layers
 * @param format the color format (ColorFormat) to be used in each layer
 * @param type the color type to be used in each layer
 */
fun arrayTexture(width: Int, height: Int, layers: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, levels: Int = 1, session:Session = Session.active): ArrayTexture {
    return Driver.instance.createArrayTexture(width, height, layers, format, type, levels).apply {
        session.track(this)
    }
}
package org.openrndr.draw

import org.openrndr.internal.Driver
import java.nio.ByteBuffer

interface ArrayTexture {
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
    fun write(layer: Int, buffer: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type)
    fun read(layer: Int, buffer: ByteBuffer)

    /** the wrapping mode to use in the horizontal direction */
    var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    var wrapV: WrapMode

    /** the filter to use when displaying at sizes smaller than the original */
    var filterMin: MinifyingFilter

    /** the filter to use when displaying at sizes larger than the original */
    var filterMag: MagnifyingFilter
}

/**
 * Creates an array texture
 * @param width the width of each layer
 * @param height the height of each layer
 * @param layers the number of layers
 * @param format the color format (ColorFormat) to be used in each layer
 * @param type the color type to be used in each layer
 */
fun arrayTexture(width: Int, height: Int, layers: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8): ArrayTexture {
    return Driver.instance.createArrayTexture(width, height, layers, format, type)
}
package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer

actual interface ArrayCubemap {
    actual val session: Session?

    actual val width: Int
    actual val layers: Int
    actual val format: ColorFormat
    actual val type: ColorType
    actual val levels: Int
    actual fun destroy()

    actual fun bind(unit: Int)

    /**
     * Write to a single layer in the array texture
     * @param layer the layer index to write to
     * @param buffer the ByteBuffer from which texture data is read
     * @param sourceFormat the format of the data in buffer
     * @param sourceType the type of the data in buffer
     */
    fun write(side: CubemapSide, layer: Int, buffer: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type, level: Int = 0)

    fun read(layer: Int, buffer: ByteBuffer, level: Int = 0)


    val bounds: Rectangle get() = Rectangle(Vector2.ZERO, width * 1.0, width * 1.0)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the layer array texture to copy contents to
     * @param targetLayer the layer of the target array texture to copy contents to
     */
    actual fun copyTo(
        layer: Int,
        target: ArrayCubemap,
        targetLayer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    /** generates mipmaps from the top-level mipmap */
    actual fun generateMipmaps()

    /** the filter to use when displaying at sizes smaller than the original */
    actual var filterMin: MinifyingFilter

    /** the filter to use when displaying at sizes larger than the original */
    actual var filterMag: MagnifyingFilter
    actual var flipV: Boolean

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the target to copy contents to
     */
    actual fun copyTo(layer: Int, target: Cubemap, fromLevel: Int, toLevel: Int)
}

/**
 * Creates an array cubemap
 * @param width the width of each layer
 * @param layers the number of layers
 * @param format the color format (ColorFormat) to be used in each layer
 * @param type the color type to be used in each layer
 */
fun arrayCubemap(width: Int, layers: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, levels: Int = 1, session: Session = Session.active): ArrayCubemap {
    return Driver.instance.createArrayCubemap(width, layers, format, type, levels).apply {
        session.track(this)
    }
}
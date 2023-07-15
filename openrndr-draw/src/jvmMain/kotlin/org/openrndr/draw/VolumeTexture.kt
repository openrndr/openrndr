package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import java.nio.ByteBuffer

actual interface VolumeTexture {

    companion object {
        fun create(
            width: Int,
            height: Int,
            depth: Int,
            format: ColorFormat = ColorFormat.RGBa,
            type: ColorType = ColorType.UINT8,
            levels: Int = 1,
            session: Session? = Session.active
        ): VolumeTexture {
            return Driver.instance.createVolumeTexture(width, height, depth, format, type, levels, session)
        }
    }

    actual val session: Session?

    actual val width: Int
    actual val height: Int
    actual val depth: Int
    actual val format: ColorFormat
    actual val type: ColorType
    actual val levels: Int

    actual fun copyTo(target: ColorBuffer, layer: Int, fromLevel: Int, toLevel: Int)

    actual fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    actual fun bind(textureUnit: Int)
    actual fun generateMipmaps()
    actual fun destroy()


    fun write(source: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type, level: Int = 0)
    fun read(target: ByteBuffer, targetFormat: ColorFormat = format, targetType: ColorType = type, level: Int = 0)
    fun read(layer: Int, target: ByteBuffer, targetFormat: ColorFormat = format, targetType: ColorType = type, level: Int = 0)


    fun write(layer: Int, source: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type, level: Int = 0)
    fun fill(color: ColorRGBa)
}

fun volumeTexture(
    width: Int,
    height: Int,
    depth: Int,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = ColorType.UINT8,
    levels: Int = 1,
    session: Session? = Session.active
): VolumeTexture {
    return VolumeTexture.create(width, height, depth, format, type, levels, session)
}
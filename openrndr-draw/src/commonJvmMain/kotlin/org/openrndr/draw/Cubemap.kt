package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.internal.ImageDriver
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.ByteBuffer

actual interface Cubemap : Texture, AutoCloseable {

    companion object {
        fun create(
            width: Int,
            format: ColorFormat = ColorFormat.RGBa,
            type: ColorType = defaultColorType(format),
            levels: Int = -1,
            session: Session? = Session.active
        ): Cubemap {
            return Driver.instance.createCubemap(width, format, type, levels, session)
        }
    }

    fun read(
        side: CubemapSide,
        target: ByteBuffer,
        targetFormat: ColorFormat = format,
        targetType: ColorType = type,
        level: Int = 0
    )

    fun write(
        side: CubemapSide,
        source: ByteBuffer,
        sourceFormat: ColorFormat = format,
        sourceType: ColorType = type,
        level: Int = 0
    )

    actual val session: Session?
    actual val width: Int
    actual val format: ColorFormat
    actual val type: ColorType
    actual val levels: Int
    actual fun copyTo(
        target: ArrayCubemap,
        layer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    actual fun copyTo(target: Cubemap, fromLevel: Int, toLevel: Int)
    actual fun copyTo(
        target: ColorBuffer,
        fromSide: CubemapSide,
        fromLevel: Int,
        toLevel: Int
    )

    actual fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    actual fun bind(textureUnit: Int)
    actual fun generateMipmaps()
    actual fun destroy()
    actual fun write(
        side: CubemapSide,
        source: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    )
}

fun loadCubemap(
    fileOrUrl: String,
    formatHint: ImageFileFormat? = ImageFileFormat.DDS,
    session: Session? = Session.active
): Cubemap {
    val data = ImageDriver.instance.loadCubemapImage(fileOrUrl, formatHint)
    try {
        return loadCubemap(data, session)
    } finally {
        data.close()
    }
}
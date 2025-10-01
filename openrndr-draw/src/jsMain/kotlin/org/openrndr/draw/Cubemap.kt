package org.openrndr.draw

import js.buffer.ArrayBufferLike
import js.buffer.ArrayBufferView
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.TexImageSource

actual interface Cubemap: AutoCloseable {
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

    fun write(side: CubemapSide, source: TexImageSource,
              sourceFormat: ColorFormat = this.format,
              sourceType: ColorType = this.type,
              x: Int = 0, y: Int = 0, level: Int = 0)
    fun write(
        side: CubemapSide,
        source: ArrayBufferView<ArrayBufferLike>,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int = 0,
        y: Int = 0,
        width: Int = this.width,
        height: Int = this.width,
        level: Int = 0
    )

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
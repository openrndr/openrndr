package org.openrndr.draw

actual interface VolumeTexture: AutoCloseable {
    actual val session: Session?
    actual val width: Int
    actual val height: Int
    actual val depth: Int
    actual val format: ColorFormat
    actual val type: ColorType
    actual val levels: Int
    actual fun copyTo(
        target: ColorBuffer,
        layer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    actual fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    actual fun bind(textureUnit: Int)
    actual fun generateMipmaps()
    actual fun destroy()
}
package org.openrndr.draw

actual interface Cubemap {
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

}
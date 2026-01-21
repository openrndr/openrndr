package org.openrndr.draw

actual interface ArrayCubemap: Texture, AutoCloseable {
    actual val session: Session?
    actual val width: Int
    actual val layers: Int
    actual val format: ColorFormat
    actual val type: ColorType
    actual val levels: Int
    actual fun destroy()

    actual fun copyTo(layer: Int, target: Cubemap, fromLevel: Int, toLevel: Int)

    actual fun copyTo(
        layer: Int,
        target: ArrayCubemap,
        targetLayer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    actual fun generateMipmaps()

    actual var filterMin: MinifyingFilter

    actual var filterMag: MagnifyingFilter
    actual var flipV: Boolean

}
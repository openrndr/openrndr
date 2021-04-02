package org.openrndr.draw

expect interface VolumeTexture {
    val session: Session?

    val width: Int
    val height: Int
    val depth: Int
    val format: ColorFormat
    val type: ColorType
    val levels: Int

    fun copyTo(target: ColorBuffer, layer: Int, fromLevel: Int = 0, toLevel: Int = 0)

    fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    fun bind(textureUnit: Int = 0)
    fun generateMipmaps()
    fun destroy()
}
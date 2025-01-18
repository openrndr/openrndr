package org.openrndr.draw

/**
 * Represents a 3D texture, also known as a volume texture. A volume texture is a set of texture data
 * stored in a 3D space, identified by its width, height, and depth dimensions. It can be used
 * in various applications, such as 3D data visualization, texture maps for 3D objects, and more.
 **/
expect interface VolumeTexture: AutoCloseable {
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
    /**
     * Generates mipmaps for the 3D texture. Mipmaps are a series of precomputed smaller textures
     * derived from the base level texture, used for efficient rendering at varying distances or levels
     * of detail.
     */
    fun generateMipmaps()
    fun destroy()
}
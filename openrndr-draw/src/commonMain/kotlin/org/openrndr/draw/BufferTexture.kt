package org.openrndr.draw

/**
 * Represents a texture buffer residing in the GPU.
 * Buffer textures provide a mechanism for storing structured data
 * that can be accessed in shaders as a one-dimensional texture.
 */
expect abstract class BufferTexture {
    abstract val session: Session?
    abstract val shadow: BufferTextureShadow
    abstract val format: ColorFormat
    abstract val type: ColorType
    abstract val elementCount: Int

    abstract fun destroy()

    /**
     * bind the BufferTexture to a texture unit
     */
    abstract fun bind(unit: Int)

}
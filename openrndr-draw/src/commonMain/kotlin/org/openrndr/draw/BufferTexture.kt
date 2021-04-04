package org.openrndr.draw

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
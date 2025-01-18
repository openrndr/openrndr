package org.openrndr.draw

actual abstract class BufferTexture: AutoCloseable {
    actual abstract val session: Session?
    actual abstract val shadow: BufferTextureShadow
    actual abstract val format: ColorFormat
    actual abstract val type: ColorType
    actual abstract val elementCount: Int
    actual abstract fun destroy()

    /**
     * bind the BufferTexture to a texture unit
     */
    actual abstract fun bind(unit: Int)

}
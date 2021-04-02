package org.openrndr.draw

actual interface BufferTexture {
    actual val session: Session?
    actual val shadow: BufferTextureShadow
    actual val format: ColorFormat
    actual val type: ColorType
    actual val elementCount: Int
    actual fun destroy()

    /**
     * bind the BufferTexture to a texture unit
     */
    actual fun bind(unit: Int)

}
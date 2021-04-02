package org.openrndr.draw

expect interface BufferTexture {
    val session: Session?
    val shadow: BufferTextureShadow
    val format: ColorFormat
    val type: ColorType
    val elementCount: Int

    fun destroy()

    /**
     * bind the BufferTexture to a texture unit
     */
    fun bind(unit: Int)

}
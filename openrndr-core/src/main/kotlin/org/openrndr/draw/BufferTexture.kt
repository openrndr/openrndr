package org.openrndr.draw

import org.openrndr.internal.Driver

interface BufferTexture {
    val shadow: BufferTextureShadow

    val format: ColorFormat
    val type: ColorType

    val elementCount: Int

    fun destroy()
    fun bind(unit: Int)

    companion object {
        fun create(elementCount: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32): BufferTexture {
            return Driver.instance.createBufferTexture(elementCount, format, type)
        }
    }

    fun put(putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.putter()
        val count = w.positionElements

        shadow.upload(0, w.position)
        w.rewind()
        return count
    }
}

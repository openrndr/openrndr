package org.openrndr.draw

import org.openrndr.internal.Driver
import java.nio.ByteBuffer

interface BufferTexture {
    val session: Session?
    val shadow: BufferTextureShadow
    val format: ColorFormat
    val type: ColorType
    val elementCount: Int

    fun destroy()
    fun bind(unit: Int)

    companion object {
        fun create(elementCount: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32, session: Session? = Session.active): BufferTexture {
            val bufferTexture = Driver.instance.createBufferTexture(elementCount, format, type)
            session?.track(bufferTexture)
            return bufferTexture
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

    fun read(target: ByteBuffer, offset:Int = 0, elementReadCount: Int = this.elementCount)
    fun write(source: ByteBuffer, offset:Int = 0, elementWriteCount: Int = this.elementCount)
}

/**
 * create a [BufferTexture]
 * @param elementCount the number of elements in the buffer texture
 * @param format the format of the elements
 * @param type the type of the elements
 * @param session the session that will track the [BufferTexture] resource
 */
fun bufferTexture(elementCount: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32, session: Session? = Session.active): BufferTexture {
    return BufferTexture.create(elementCount, format, type, session)
}
package org.openrndr.webgl

import org.openrndr.draw.*

class BufferTextureShadowWebGL: BufferTextureShadow() {
    override val bufferTexture: BufferTextureWebGL
        get() = TODO("Not yet implemented")

    override fun upload(offset: Int, sizeInBytes: Int) {
        TODO("Not yet implemented")
    }

    override fun download() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun close() {
        destroy()
    }
}

class BufferTextureWebGL: BufferTexture() {
    override val session: Session
        get() = TODO("Not yet implemented")
    override val shadow: BufferTextureShadowWebGL
        get() = TODO("Not yet implemented")
    override val format: ColorFormat
        get() = TODO("Not yet implemented")
    override val type: ColorType
        get() = TODO("Not yet implemented")
    override val elementCount: Int
        get() = TODO("Not yet implemented")

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun bind(unit: Int) {
        TODO("Not yet implemented")
    }

    override fun close() {
        destroy()
    }
}

package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL33C.*
import org.openrndr.draw.*
import java.nio.ByteBuffer

class ArrayTextureGL3(val target: Int,
                      val texture: Int,
                      override val width: Int,
                      override val height: Int,
                      override val layers: Int,
                      override val format: ColorFormat,
                      override val type: ColorType) : ArrayTexture {

    companion object {
        fun create(width: Int, height: Int, layers: Int, format: ColorFormat, type: ColorType): ArrayTextureGL3 {
            val maximumLayers =  glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS)
            if (layers > maximumLayers) {
                throw IllegalArgumentException("layers ($layers) exceeds maximum of $maximumLayers")
            }
            val texture = glGenTextures()
            glBindTexture(GL_TEXTURE_2D_ARRAY, texture)
            checkGLErrors()
            glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, internalFormat(format, type), width, height, layers, 0, GL_RGB, GL_UNSIGNED_BYTE, null as ByteBuffer?)
            checkGLErrors()
            return ArrayTextureGL3(GL_TEXTURE_2D_ARRAY, texture, width, height, layers, format, type)
        }
    }

    override fun destroy() {
        glDeleteTextures(texture)
        checkGLErrors()
    }

    override fun bind(unit: Int) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(target, texture)
    }

    override fun write(layer: Int, buffer: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType) {
        bound {
            if (sourceType.compressed) {
                glCompressedTexSubImage3D(target, 0, 0, 0, layer, width, height, 1, sourceType.glCompressedType(), buffer)
            } else {
                glTexSubImage3D(target, 0, 0, 0, layer, width, height, 1, sourceFormat.glFormat(), sourceType.glType(), buffer)
            }
            debugGLErrors()
        }
    }


    override fun read(layer: Int, buffer: ByteBuffer) {
        TODO()
    }

    override var wrapU: WrapMode
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_WRAP_S, value.glWrap())
            }
        }

    override var wrapV: WrapMode
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_WRAP_T, value.glWrap())
            }
        }

    override var filterMin: MinifyingFilter
        get() = TODO("not implemented")
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_MIN_FILTER, value.toGLFilter())
            }
        }

    override var filterMag: MagnifyingFilter
        get() = TODO("not implemented")
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_MAG_FILTER, value.toGLFilter())
            }
        }


    private fun bound(f: ArrayTextureGL3.() -> Unit) {
        glActiveTexture(GL_TEXTURE0)
        val current = glGetInteger(GL_TEXTURE_BINDING_2D_ARRAY)
        glBindTexture(target, texture)
        this.f()
        glBindTexture(target, current)
    }
}
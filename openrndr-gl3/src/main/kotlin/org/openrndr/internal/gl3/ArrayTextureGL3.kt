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
            val maximumLayers = glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS)
            if (layers > maximumLayers) {
                throw IllegalArgumentException("layers ($layers) exceeds maximum of $maximumLayers")
            }
            val texture = glGenTextures()
            glBindTexture(GL_TEXTURE_2D_ARRAY, texture)
            checkGLErrors()
            glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, internalFormat(format, type), width, height, layers, 0, GL_RGB, GL_UNSIGNED_BYTE, null as ByteBuffer?)
            checkGLErrors()
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, MinifyingFilter.LINEAR.toGLFilter())
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, MagnifyingFilter.LINEAR.toGLFilter())
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
                glCompressedTexSubImage3D(target, 0, 0, 0, layer, width, height, 1, compressedType(sourceFormat, sourceType), buffer)
                debugGLErrors()
            } else {
                glTexSubImage3D(target, 0, 0, 0, layer, width, height, 1, sourceFormat.glFormat(), sourceType.glType(), buffer)
                debugGLErrors()
            }
            debugGLErrors()
        }
    }

    override fun copyTo(layer: Int, target: ColorBuffer) {
        if (target.multisample == BufferMultisample.Disabled) {
            val readTarget = renderTarget(width, height) {
                arrayTexture(this@ArrayTextureGL3, layer)
            } as RenderTargetGL3

            target as ColorBufferGL3
            readTarget.bind()
            glReadBuffer(GL_COLOR_ATTACHMENT0)
            target.bound {
                glCopyTexSubImage2D(target.target, 0, 0, 0, 0, 0, target.width, target.height)
                debugGLErrors()
            }
            readTarget.unbind()

            readTarget.detachColorBuffers()
            readTarget.destroy()
        } else {
            throw IllegalArgumentException("cannot copy to multisample target")
        }
    }

    override fun copyTo(layer: Int, target: ArrayTexture, targetLayer: Int) {
        val readTarget = renderTarget(width, height) {
            arrayTexture(this@ArrayTextureGL3, layer)
        } as RenderTargetGL3

        target as ArrayTextureGL3
        readTarget.bind()
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        target.bound {
            glCopyTexSubImage3D(target.target, 0, 0, 0, targetLayer, 0, 0, target.width, target.height)
            debugGLErrors()
        }
        readTarget.unbind()

        readTarget.detachColorBuffers()
        readTarget.destroy()
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

    override var flipV: Boolean = false

    override fun generateMipmaps() {
        bound {
            glGenerateMipmap(target)
        }
    }

    internal fun bound(f: ArrayTextureGL3.() -> Unit) {
        glActiveTexture(GL_TEXTURE0)
        val current = glGetInteger(GL_TEXTURE_BINDING_2D_ARRAY)
        glBindTexture(target, texture)
        this.f()
        glBindTexture(target, current)
    }
}
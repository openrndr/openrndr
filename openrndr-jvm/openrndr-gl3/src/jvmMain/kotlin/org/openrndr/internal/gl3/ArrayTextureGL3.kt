package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.opengl.GL42C.glTexStorage3D
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import java.nio.ByteBuffer

class ArrayTextureGL3(val target: Int,
                      val texture: Int,
                      val storageMode: TextureStorageModeGL,
                      override val width: Int,
                      override val height: Int,
                      override val layers: Int,
                      override val format: ColorFormat,
                      override val type: ColorType,
                      override val levels: Int,
                      override val session: Session?) : ArrayTexture() {

    companion object {
        fun create(width: Int, height: Int, layers: Int, format: ColorFormat, type: ColorType, levels: Int, session: Session?): ArrayTextureGL3 {
            val maximumLayers = glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS)
            if (layers > maximumLayers) {
                throw IllegalArgumentException("layers ($layers) exceeds maximum of $maximumLayers")
            }
            val texture = glGenTextures()

            val storageMode = when {
                (Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_3 -> TextureStorageModeGL.STORAGE
                else -> TextureStorageModeGL.IMAGE
            }

            glBindTexture(GL_TEXTURE_2D_ARRAY, texture)
            checkGLErrors()

            when (storageMode) {
                TextureStorageModeGL.IMAGE -> {
                    for (level in 0 until levels) {
                        val div = 1 shr level
                        glTexImage3D(GL_TEXTURE_2D_ARRAY,
                                level, internalFormat(format, type).first,
                                width / div, height / div, layers,
                                0, GL_RGB, GL_UNSIGNED_BYTE, null as ByteBuffer?)

                        checkGLErrors()
                    }
                }
                TextureStorageModeGL.STORAGE -> {
                    glTexStorage3D(GL_TEXTURE_2D_ARRAY, levels, internalFormat(format, type).first, width, height, layers)
                    checkGLErrors()
                }
            }
            if (levels > 1) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels - 1)
                checkGLErrors()
            }

            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, MinifyingFilter.LINEAR.toGLFilter())
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, MagnifyingFilter.LINEAR.toGLFilter())
            checkGLErrors()
            return ArrayTextureGL3(GL_TEXTURE_2D_ARRAY, texture, storageMode, width, height, layers, format, type, levels, session)
        }
    }

    internal fun glFormat(): Int {
        return internalFormat(format, type).first
    }

    override fun destroy() {
        session?.untrack(this)
        glDeleteTextures(texture)
        checkGLErrors()
    }

    override fun bind(unit: Int) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(target, texture)
    }

    override fun write(layer: Int, buffer: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType, level: Int) {
        bound {
            val div = 1 shl level
            if (sourceType.compressed) {
                glCompressedTexSubImage3D(target, level, 0, 0, layer, width / div, height / div, 1, compressedType(sourceFormat, sourceType), buffer)
                glFlush()
                glFinish()
                debugGLErrors()
            } else {
                glTexSubImage3D(target, level, 0, 0, layer, width / div, height / div, 1, sourceFormat.glFormat(), sourceType.glType(), buffer)
                debugGLErrors()
            }
            debugGLErrors()
        }
    }

    override fun copyTo(layer: Int, target: ColorBuffer, fromLevel: Int, toLevel: Int) {
        if (target.multisample == BufferMultisample.Disabled) {
            val fromDiv = 1 shl fromLevel
            val toDiv = 1 shl toLevel
            val readTarget = renderTarget(width / fromDiv, height / fromDiv) {
                arrayTexture(this@ArrayTextureGL3, layer, fromLevel)
            } as RenderTargetGL3

            target as ColorBufferGL3
            readTarget.bind()
            glReadBuffer(GL_COLOR_ATTACHMENT0)
            target.bound {
                glCopyTexSubImage2D(target.target, toLevel, 0, 0, 0, 0, target.width / toDiv, target.height / toDiv)
                debugGLErrors()
            }
            readTarget.unbind()

            readTarget.detachColorAttachments()
            readTarget.destroy()
        } else {
            throw IllegalArgumentException("cannot copy to multisample target")
        }
    }

    override fun copyTo(layer: Int, target: ArrayTexture, targetLayer: Int, fromLevel: Int, toLevel: Int) {
        val fromDiv = 1 shl fromLevel
        val toDiv = 1 shl toLevel
        val readTarget = renderTarget(width / fromDiv, height / fromDiv) {
            arrayTexture(this@ArrayTextureGL3, layer, fromLevel)
        } as RenderTargetGL3

        target as ArrayTextureGL3
        readTarget.bind()
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        target.bound {
            glCopyTexSubImage3D(target.target, toLevel, 0, 0, targetLayer, 0, 0, target.width / toDiv, target.height / toDiv)
            debugGLErrors()
        }
        readTarget.unbind()

        readTarget.detachColorAttachments()
        readTarget.destroy()
    }


    override fun read(layer: Int, buffer: ByteBuffer, level: Int) {
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
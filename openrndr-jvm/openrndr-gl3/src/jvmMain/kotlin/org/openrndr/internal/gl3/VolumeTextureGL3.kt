package org.openrndr.internal.gl3

import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL12C.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL45C.glTextureStorage3D
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver

import java.nio.ByteBuffer


class VolumeTextureGL3(
    val texture: Int,
    val storageMode: TextureStorageModeGL,
    override val width: Int,
    override val height: Int,
    override val depth: Int,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val session: Session?
) : VolumeTexture {

    private var destroyed = false

    internal fun glFormat(): Int {
        return internalFormat(format, type).first
    }


    override fun destroy() {
        if (!destroyed) {
            session?.untrack(this)
            glDeleteTextures(texture)
            destroyed = true
            checkGLErrors()
        }
    }

    private fun bound(f: () -> Unit) {
        bind()
        f()
    }

    override fun write(layer: Int, source: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType, level: Int) {
        bound {
            val div = 1 shl level
            if (sourceType.compressed) {
                GL33C.glCompressedTexSubImage3D(
                    GL33C.GL_TEXTURE_3D,
                    level,
                    0,
                    0,
                    layer,
                    width / div,
                    height / div,
                    1,
                    compressedType(sourceFormat, sourceType),
                    source
                )
                debugGLErrors()
            } else {
                GL33C.glTexSubImage3D(
                    GL33C.GL_TEXTURE_3D,
                    level,
                    0,
                    0,
                    layer,
                    width / div,
                    height / div,
                    1,
                    sourceFormat.glFormat(),
                    sourceType.glType(),
                    source
                )
                debugGLErrors()
            }
            debugGLErrors()
        }
    }

    override fun fill(color: ColorRGBa) {
        (Driver.instance as DriverGL3).version.require(DriverVersionGL.GL_VERSION_4_4)
        require(storageMode == TextureStorageModeGL.STORAGE)
        val floatData = floatArrayOf(color.r.toFloat(), color.g.toFloat(), color.b.toFloat(), color.alpha.toFloat())
        for (level in 0 until levels) {
            GL44C.glClearTexImage(texture, level, ColorFormat.RGBa.glFormat(), ColorType.FLOAT32.glType(), floatData)
            debugGLErrors()
        }
    }


    override fun read(layer: Int, target: ByteBuffer, targetFormat: ColorFormat, targetType: ColorType, level: Int) {
        if (useNamedTexture) {
            GL45.glGetTextureSubImage(
                texture, level, 0, 0, layer, width, height, 1, targetFormat.glFormat(),
                targetType.glType(), target
            )
        } else {
            error("only implemented for opengl 4.5")
        }

        debugGLErrors()
    }

    override fun read(target: ByteBuffer, targetFormat: ColorFormat, targetType: ColorType, level: Int) {
        if (useNamedTexture) {
            GL45C.glGetTextureImage(texture, level, targetFormat.glFormat(), targetType.glType(), target)
        } else {
            glGetTexImage(GL_TEXTURE_3D, level, targetFormat.glFormat(), targetType.glType(), target)
        }
        debugGLErrors()
    }

    override fun write(source: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType, level: Int) {

        if (useNamedTexture) {
            GL45C.glTextureSubImage3D(
                texture,
                level,
                0,
                0,
                0,
                width,
                height,
                depth,
                sourceFormat.glFormat(),
                sourceType.glType(),
                source
            )
        } else {
            glBindTexture(GL_TEXTURE_3D, texture)
            glTexSubImage3D(
                GL_TEXTURE_3D,
                level,
                0,
                0,
                0,
                width,
                height,
                depth,
                sourceFormat.glFormat(),
                sourceType.glType(),
                source
            )
        }
        //GL45C.glGetTextureImage(texture, level, sourceFormat.glFormat(), sourceType.glType(), source)
        checkGLErrors()
    }

    override fun copyTo(target: ColorBuffer, layer: Int, fromLevel: Int, toLevel: Int) {
        if (target.multisample == BufferMultisample.Disabled) {
            val fromDiv = 1 shl fromLevel
            val toDiv = 1 shl toLevel
            val readTarget = renderTarget(width / fromDiv, height / fromDiv) {
                volumeTexture(this@VolumeTextureGL3, layer, fromLevel)
            } as RenderTargetGL3

            target as ColorBufferGL3
            readTarget.bind()
            glReadBuffer(GL33C.GL_COLOR_ATTACHMENT0)
            target.bound {
                glCopyTexSubImage2D(
                    target.target,
                    toLevel,
                    0,
                    0,
                    0,
                    0,
                    target.width / toDiv,
                    target.height / toDiv
                )
                debugGLErrors()
            }
            readTarget.unbind()

            readTarget.detachColorAttachments()
            readTarget.destroy()
        } else {
            throw IllegalArgumentException("cannot copy to multisample target")
        }
    }

    override fun filter(min: MinifyingFilter, mag: MagnifyingFilter) {
        glTexParameteri(GL33C.GL_TEXTURE_3D, GL33C.GL_TEXTURE_MIN_FILTER, min.toGLFilter())
        glTexParameteri(GL33C.GL_TEXTURE_3D, GL33C.GL_TEXTURE_MAG_FILTER, mag.toGLFilter())
    }

    override fun bind(textureUnit: Int) {
        require(!destroyed)
        glActiveTexture(GL33C.GL_TEXTURE0 + textureUnit)
        glBindTexture(GL33C.GL_TEXTURE_3D, texture)
    }

    override fun generateMipmaps() {
        bound {
            glGenerateMipmap(GL33C.GL_TEXTURE_CUBE_MAP)
        }
    }

    companion object {
        val useNamedTexture = Driver.glVersion >= DriverVersionGL.GL_VERSION_4_5 && Driver.glType == DriverTypeGL.GL

        fun create(
            width: Int,
            height: Int,
            depth: Int,
            format: ColorFormat,
            type: ColorType,
            levels: Int,
            session: Session?
        ): VolumeTextureGL3 {
            require(levels >= 1) {
                """should have at least 1 mipmap level (requested $levels)"""
            }
            val (internalFormat, _) = internalFormat(format, type)

            debugGLErrors() { "pre-existing errors"}

            /*
            if (useStorage) {
                glTexStorage3D(
                    GL33C.GL_PROXY_TEXTURE_3D,
                    0,
                    internalFormat,
                    width,
                    height,
                    depth
                )
            } else {
                glTexImage3D(
                    GL33C.GL_PROXY_TEXTURE_3D,
                    0,
                    internalFormat,
                    width,
                    height,
                    depth,
                    0,
                    format.glFormat(),
                    type.glType(), null as ByteBuffer?
                )
            }
            checkGLErrors() { "failure after createing GL_PROXY_TEXTURE_3D texture. ${format}/${type}"}
            if (Driver.glType == DriverTypeGL.GL) {

                val proxyWidth = glGetTexLevelParameteri(
                    GL_PROXY_TEXTURE_3D, 0,
                    GL_TEXTURE_WIDTH
                )
                checkGLErrors() { "failure after glGetTexLevelParameteri"}

                require(proxyWidth == width) {
                    glGetError()
                    "failed to create ${width}x${height}x${depth} volume texture with format ${format} and type ${type}"
                }
            }*/



            val texture = glGenTextures()
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_3D, texture)

            val storageMode = when {
                Driver.capabilities.textureStorage -> TextureStorageModeGL.STORAGE
                else -> TextureStorageModeGL.IMAGE
            }

            checkGLErrors()
            when (storageMode) {
                TextureStorageModeGL.STORAGE -> {
                    if (useNamedTexture) {
                        glTextureStorage3D(texture, levels, internalFormat, width, height, depth)
                    } else {
                        glTexStorage3D(GL_TEXTURE_3D, levels, internalFormat, width, height, depth)
                    }
                    checkGLErrors()
                }

                TextureStorageModeGL.IMAGE -> {
                    for (level in 0 until levels) {
                        val div = 1 shl level
                        val nullBB: ByteBuffer? = null
                        glTexImage3D(
                            GL33C.GL_TEXTURE_3D,
                            level,
                            internalFormat,
                            width / div,
                            height / div,
                            depth / div,
                            0,
                            format.glFormat(),
                            type.glType(),
                            nullBB
                        )
                        checkGLErrors()
                    }
                }
            }
            glTexParameteri(GL33C.GL_TEXTURE_3D, GL33C.GL_TEXTURE_BASE_LEVEL, 0)
            glTexParameteri(GL33C.GL_TEXTURE_3D, GL33C.GL_TEXTURE_MAX_LEVEL, levels - 1)
            return VolumeTextureGL3(texture, storageMode, width, height, depth, format, type, levels, session)
        }
    }
}
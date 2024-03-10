package org.openrndr.internal.gl3

import org.lwjgl.opengl.AMDSeamlessCubemapPerTexture.GL_TEXTURE_CUBE_MAP_SEAMLESS
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.openrndr.draw.*
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

val CubemapSide.glTextureTarget
    get() = when (this) {
        CubemapSide.POSITIVE_X -> GL_TEXTURE_CUBE_MAP_POSITIVE_X
        CubemapSide.POSITIVE_Y -> GL_TEXTURE_CUBE_MAP_POSITIVE_Y
        CubemapSide.POSITIVE_Z -> GL_TEXTURE_CUBE_MAP_POSITIVE_Z
        CubemapSide.NEGATIVE_X -> GL_TEXTURE_CUBE_MAP_NEGATIVE_X
        CubemapSide.NEGATIVE_Y -> GL_TEXTURE_CUBE_MAP_NEGATIVE_Y
        CubemapSide.NEGATIVE_Z -> GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
    }

class CubemapGL3(val texture: Int, override val width: Int, override val type: ColorType, override val format: ColorFormat, levels: Int, override val session: Session?) : Cubemap {

    override var levels = levels
        private set(value) {
            if (field != value) {
                field = value
                bound {
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels - 1)
                }
            }
        }

    private var destroyed = false

    companion object {
        fun create(
            width: Int,
            format: ColorFormat,
            type: ColorType,
            levels: Int,
            session: Session? = Session.active
        ): CubemapGL3 {

            require(levels >= 1) {
                """should have at least 1 level (has $levels)"""
            }


            val texture = glGenTextures()

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_CUBE_MAP, texture)
            glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS)

            val effectiveWidth = width

            val (internalFormat, _) = internalFormat(format, type)

            for (side in CubemapSide.entries) {
                for (level in 0 until levels) {
                    val div = 1 shl level
                    val nullBB: ByteBuffer? = null
                    glTexImage2D(
                        side.glTextureTarget,
                        level,
                        internalFormat,
                        effectiveWidth / div,
                        effectiveWidth / div,
                        0,
                        format.glFormat(),
                        type.glType(),
                        nullBB
                    )
                    checkGLErrors { it ->
                        "width: $width, format: $format, type: $type -> target: ${glEnumName(side.glTextureTarget)}, level: ${level}, internalFormat: ${glEnumName(internalFormat)}" +
                                "format: ${glEnumName(format.glFormat())}, type: ${glEnumName(type.glType())}"

                    }
                }
            }
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_BASE_LEVEL, 0)
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAX_LEVEL, levels - 1)

            return CubemapGL3(texture, width, type, format, levels, session)
        }
    }
    internal fun glFormat(): Int {
        return internalFormat(format, type).first
    }

    override fun generateMipmaps() {
        bound {
            glGenerateMipmap(GL_TEXTURE_CUBE_MAP)
            //levels = ceil(log(width.toDouble(), 2.0)).toInt()
            //glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAX_LEVEL, levels)
        }
    }

    override fun filter(min: MinifyingFilter, mag: MagnifyingFilter) {
        bound {
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, min.toGLFilter())
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, mag.toGLFilter())
        }
    }

    override fun copyTo(target: ArrayCubemap, layer: Int, fromLevel: Int, toLevel: Int) {
        require(!destroyed)
        val fromDiv = 1 shl fromLevel
        val toDiv = 1 shl toLevel
        for (side in CubemapSide.entries) {
            val readTarget = renderTarget(width / fromDiv, width / fromDiv) {
                cubemap(this@CubemapGL3, side, fromLevel)
            } as RenderTargetGL3

            target as ArrayCubemapGL4
            readTarget.bind()
            glReadBuffer(GL_COLOR_ATTACHMENT0)
            target.bound {
                glCopyTexSubImage3D(target.target, toLevel, 0, 0, layer * 6 + side.ordinal, 0, 0, target.width / toDiv, target.width / toDiv)
                debugGLErrors()
            }
            readTarget.unbind()
            readTarget.detachColorAttachments()
            readTarget.destroy()
        }
    }

    override fun copyTo(target: Cubemap, fromLevel: Int, toLevel: Int) {
        debugGLErrors()
        require(!destroyed)
        val fromDiv = 1 shl fromLevel
        val toDiv = 1 shl toLevel
        for (side in CubemapSide.entries) {
            val readTarget = renderTarget(width / fromDiv, width / fromDiv) {
                cubemap(this@CubemapGL3, side, fromLevel)
                debugGLErrors()
            } as RenderTargetGL3

            target as CubemapGL3
            readTarget.bind()
            glReadBuffer(GL_COLOR_ATTACHMENT0)
            debugGLErrors()

            target.bound {
                glCopyTexSubImage2D(side.glTextureTarget, toLevel, 0, 0, 0, 0, target.width / toDiv, target.width / toDiv)
                debugGLErrors()
            }
            readTarget.unbind()
            readTarget.detachColorAttachments()
            readTarget.destroy()
        }
    }

    override fun copyTo(target: ColorBuffer, fromSide: CubemapSide, fromLevel: Int, toLevel: Int) {
        debugGLErrors()
        require(!destroyed)
        val fromDiv = 1 shl fromLevel
        val toDiv = 1 shl toLevel

        val readTarget = renderTarget(width / fromDiv, width / fromDiv) {
            cubemap(this@CubemapGL3, fromSide, fromLevel)
            debugGLErrors()
        } as RenderTargetGL3

        target as ColorBufferGL3
        readTarget.bind()
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        debugGLErrors()

        target.bound {
            glCopyTexSubImage2D(target.target, toLevel, 0, 0, 0, 0, target.width / toDiv, target.width / toDiv)
            debugGLErrors()
        }
        readTarget.unbind()
        readTarget.detachColorAttachments()
        readTarget.destroy()
    }

    override fun read(side: CubemapSide, target: ByteBuffer, targetFormat: ColorFormat, targetType: ColorType, level: Int) {
        bound {
            glPixelStorei(GL_PACK_ALIGNMENT, 1)
            glGetTexImage(side.glTextureTarget, level, targetFormat.glFormat(), targetType.glType(), target)
            debugGLErrors()
        }
    }

    override fun write(side: CubemapSide, source: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType, level: Int) {
        require(!destroyed)
        val div = 1 shl level

        if (!source.isDirect) {
            throw IllegalArgumentException("buffer is not a direct buffer.")
        }

        val effectiveWidth = width
        val effectiveHeight = width
        if (!sourceType.compressed) {
            val bytesNeeded = sourceFormat.componentCount * sourceType.componentSize * (effectiveWidth / div) * (effectiveHeight / div)
            require(bytesNeeded <= source.remaining()) {
                "write of ${width}x${width} of $format/$type pixels to level $level requires $bytesNeeded bytes, buffer only has ${source.remaining()} bytes left, buffer capacity is ${source.capacity()}"
            }
        }
        bound {
            debugGLErrors()

            (source as Buffer).rewind()
            source.order(ByteOrder.nativeOrder())
            val currentPack = intArrayOf(0)
            glGetIntegerv(GL_UNPACK_ALIGNMENT, currentPack)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

            if (sourceType.compressed) {
                glCompressedTexSubImage2D(side.glTextureTarget, level, 0, 0, width / div, width / div, compressedType(sourceFormat, sourceType), source)
                debugGLErrors {
                    when (it) {
                        GL_INVALID_VALUE -> "data size mismatch? ${source.remaining()}"
                        else -> null
                    }
                }
            } else {
                glTexSubImage2D(side.glTextureTarget, level, 0, 0, width / div, width / div, sourceFormat.glFormat(), sourceType.glType(), source)
                debugGLErrors()
            }
            glPixelStorei(GL_UNPACK_ALIGNMENT, currentPack[0])
            debugGLErrors()
            (source as Buffer).rewind()
            (source as Buffer).rewind()
        }

    }

    override fun write(
        side: CubemapSide,
        source: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        write(side, source.byteBuffer, sourceFormat, sourceType, level)
    }

    override fun bind(textureUnit: Int) {
        if (!destroyed) {
            glActiveTexture(GL_TEXTURE0 + textureUnit)
            glBindTexture(GL_TEXTURE_CUBE_MAP, texture)
        } else {
            throw IllegalStateException("attempting to bind destroyed cubemap")
        }
    }

    override fun destroy() {
        if (!destroyed) {
            glDeleteTextures(texture)
            Session.active.untrack(this)
            destroyed = true
        }
    }

    internal fun bound(f: CubemapGL3.() -> Unit) {
        glActiveTexture(GL_TEXTURE0)
        val current = glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP)
        glBindTexture(GL_TEXTURE_CUBE_MAP, texture)
        this.f()
        glBindTexture(GL_TEXTURE_CUBE_MAP, current)
    }
}
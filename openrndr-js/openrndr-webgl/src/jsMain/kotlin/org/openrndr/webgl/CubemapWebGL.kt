package org.openrndr.webgl

import js.buffer.ArrayBufferLike
import js.buffer.ArrayBufferView
import js.typedarrays.Uint8Array
import org.openrndr.draw.*
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.GLenum
import web.gl.TexImageSource
import web.gl.WebGLTexture
import web.gl.WebGL2RenderingContext as GL

class CubemapWebGL(
    val context: GL,
    val target: GLenum,
    val texture: WebGLTexture,
    override val width: Int,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val session: Session?

) : Cubemap {
    companion object {
        fun create(
            context: GL,
            width: Int,
            format: ColorFormat,
            type: ColorType,
            levels: Int,
            session: Session?
        ): CubemapWebGL {
            val texture = context.createTexture()
            context.activeTexture(GL.TEXTURE0)
            context.bindTexture(GL.TEXTURE_CUBE_MAP, texture)
            val (internalFormat, _) = internalFormat(format, type)
            for (side in CubemapSide.entries) {
                for (level in 0 until levels) {
                    val div = 1 shl level
                    context.texImage2D(
                        side.glTextureTarget,
                        level,
                        internalFormat,
                        width / div,
                        width / div,
                        0,
                        format.glFormat(),
                        type.glType(),
                        null
                    )
                }
            }
            return CubemapWebGL(context, GL.TEXTURE_CUBE_MAP, texture, width, format, type, levels, session)
        }
    }

    private var destroyed = false

    override fun copyTo(target: ArrayCubemap, layer: Int, fromLevel: Int, toLevel: Int) {
        error("not supported")
    }

    override fun copyTo(target: Cubemap, fromLevel: Int, toLevel: Int) {
        error("not supported")
    }

    override fun copyTo(target: ColorBuffer, fromSide: CubemapSide, fromLevel: Int, toLevel: Int) {
        error("not supported")
    }

    override fun filter(min: MinifyingFilter, mag: MagnifyingFilter) {
        bind(0)
        context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, min.toGLFilter())
        context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, mag.toGLFilter())
    }

    override fun bind(textureUnit: Int) {
        context.activeTexture(glTextureEnum(textureUnit))
        context.bindTexture(target, texture)
    }

    override fun generateMipmaps() {
        bind(0)
        context.generateMipmap(target)

    }

    override fun destroy() {
        if (!destroyed) {
            context.deleteTexture(texture)
            destroyed = true
        }
    }

    override fun write(
        side: CubemapSide,
        source: TexImageSource,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        level: Int
    ) {
        require(!destroyed)
        bind(0)
        context.texSubImage2D(side.glTextureTarget, level, x, y, sourceFormat.glFormat(), sourceType.glType(), source)
    }

    override fun write(
        side: CubemapSide,
        source: ArrayBufferView<ArrayBufferLike>,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {

        val u8Source = Uint8Array(source.buffer)

        if (!sourceType.compressed) {
            context.texSubImage2D(
                side.glTextureTarget,
                level,
                x,
                y,
                width,
                height,
                sourceFormat.glFormat(),
                sourceType.glType(),
                source
            )
        } else {
            context.compressedTexSubImage2D(
                side.glTextureTarget,
                level,
                x,
                y,
                width,
                height,
                sourceType.glType(),
                source,
                null,
                null
            )

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
        write(side, source, sourceFormat, sourceType, x, y, width, height, level)
    }

    override fun close() {
        destroy()
    }
}

val CubemapSide.glTextureTarget
    get() = when (this) {
        CubemapSide.POSITIVE_X -> GL.TEXTURE_CUBE_MAP_POSITIVE_X
        CubemapSide.POSITIVE_Y -> GL.TEXTURE_CUBE_MAP_POSITIVE_Y
        CubemapSide.POSITIVE_Z -> GL.TEXTURE_CUBE_MAP_POSITIVE_Z
        CubemapSide.NEGATIVE_X -> GL.TEXTURE_CUBE_MAP_NEGATIVE_X
        CubemapSide.NEGATIVE_Y -> GL.TEXTURE_CUBE_MAP_NEGATIVE_Y
        CubemapSide.NEGATIVE_Z -> GL.TEXTURE_CUBE_MAP_NEGATIVE_Z
    }

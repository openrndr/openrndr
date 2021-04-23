package org.openrndr.webgl

import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.TexImageSource
import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.WebGLTexture
import org.openrndr.draw.*

class CubemapWebGL(
    val context: GL,
    val target: Int,
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
            val texture = context.createTexture() ?: error("failed to create texture")
            context.activeTexture(GL.TEXTURE0)
            context.bindTexture(GL.TEXTURE_CUBE_MAP, texture)
            val (internalFormat, _) = internalFormat(format, type)
            for (side in CubemapSide.values()) {
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
        TODO("Not yet implemented")
    }

    override fun bind(textureUnit: Int) {
        context.activeTexture(GL.TEXTURE0 + textureUnit)
        context.bindTexture(target, texture)
    }

    override fun generateMipmaps() {

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
        source: ArrayBufferView,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        context.texSubImage2D(side.glTextureTarget, level, x, y, width, height, sourceFormat.glFormat(), sourceType.glType(), source)
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

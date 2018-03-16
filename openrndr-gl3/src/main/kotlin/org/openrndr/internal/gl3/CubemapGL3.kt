package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL30
import org.openrndr.draw.*
import java.nio.ByteBuffer

class CubemapGL3(val texture: Int, override val width: Int, val sides: List<ColorBuffer>) : Cubemap {


    companion object {
        fun create(width: Int, format: ColorFormat, type: ColorType): CubemapGL3 {

            val textures = IntArray(1)
            glGenTextures(textures)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_CUBE_MAP, textures[0])

            val effectiveWidth = width
            val effectiveHeight = width
            val internalFormat = internalFormat(format, type)
            val sides = mutableListOf<ColorBufferGL3>()
            for (i in 0..5) {
                val nullBB: ByteBuffer? = null
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, internalFormat, effectiveWidth, effectiveHeight, 0, format.glFormat(), type.glType(), nullBB)
                sides.add(ColorBufferGL3(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, textures[0], width, width, 1.0, format, type))
            }
            return CubemapGL3(textures[0], width, sides)
        }


        fun fromUrls(urls: List<String>): CubemapGL3 {
            if (urls.size != 6) {
                throw RuntimeException("6 urls are needed for a cubemap")
            }


            val textures = IntArray(1)
            glGenTextures(textures)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_CUBE_MAP, textures[0])
            val sides = mutableListOf<ColorBufferGL3>()


            urls.forEachIndexed { index, it ->
                val data = ColorBufferDataGL3.fromUrl(it)
                val internalFormat = internalFormat(data.format, data.type)
                val nullBB: ByteBuffer? = null

                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + index, 0, internalFormat, data.width, data.height, 0, data.format.glFormat(), data.type.glType(), nullBB)
                sides.add(ColorBufferGL3(GL_TEXTURE_CUBE_MAP_POSITIVE_X + index, textures[0], data.width, data.width, 1.0, data.format, data.type))
            }
            return CubemapGL3(textures[0], sides[0].width, sides)
        }

    }

    override fun generateMipmaps() {
        bound {
            GL30.glGenerateMipmap(GL_TEXTURE_CUBE_MAP)
        }

    }

    override fun filter(min: MinifyingFilter, mag: MagnifyingFilter) {
        bound {
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, min.toGLFilter())
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, mag.toGLFilter())
        }
    }

    override fun side(side: CubemapSide): ColorBuffer {
        return sides[side.ordinal]
    }

    override fun bind(textureUnit: Int) {
        glActiveTexture(GL_TEXTURE0 + textureUnit)
        glBindTexture(GL_TEXTURE_CUBE_MAP, texture)
    }

    override fun destroy() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun bound(f: CubemapGL3.() -> Unit) {
        glActiveTexture(GL_TEXTURE0)
        val current = glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP)

        glBindTexture(GL_TEXTURE_CUBE_MAP, texture)
        this.f()
        glBindTexture(GL_TEXTURE_CUBE_MAP, current)
    }

}
package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL30.*
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import java.nio.ByteBuffer

class DepthBufferGL3(val texture: Int,
                     override val width: Int,
                     override val height: Int,
                     override val format: DepthFormat) : DepthBuffer {

    companion object {
        fun create(width: Int, height: Int, format: DepthFormat): DepthBufferGL3 {
            val glTexture = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, glTexture)

            val nullBuffer: ByteBuffer? = null
            glTexImage2D(GL_TEXTURE_2D, 0, format.toGLFormat(), width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, nullBuffer)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            return DepthBufferGL3(glTexture, width, height, format)
        }
    }

    override fun destroy() {
        glDeleteTextures(texture)
    }

    override fun bind(textureUnit: Int) {
        glActiveTexture(GL_TEXTURE0 + textureUnit)
        glBindTexture(GL_TEXTURE_2D, texture)
    }
}

private fun DepthFormat.toGLFormat(): Int {
    return when (this) {
        DepthFormat.DEPTH24_STENCIL8 -> GL_DEPTH24_STENCIL8
        DepthFormat.DEPTH24 -> GL_DEPTH_COMPONENT24
        DepthFormat.DEPTH32F -> GL_DEPTH_COMPONENT32F
        DepthFormat.DEPTH32F_STENCIL8 -> GL_DEPTH32F_STENCIL8
    }
}

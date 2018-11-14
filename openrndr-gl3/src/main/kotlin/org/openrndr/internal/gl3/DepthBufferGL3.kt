package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL32C
import org.lwjgl.opengl.GL33C.*
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import java.nio.ByteBuffer

class DepthBufferGL3(val texture: Int,
                     val target: Int,
                     override val width: Int,
                     override val height: Int,
                     override val format: DepthFormat,
                     override val multisample: BufferMultisample) : DepthBuffer {

    companion object {
        fun create(width: Int, height: Int, format: DepthFormat, multisample: BufferMultisample): DepthBufferGL3 {


            println("creating depth buffer $multisample")
            val glTexture = glGenTextures()
            val target = when(multisample) {
                BufferMultisample.DISABLED -> GL_TEXTURE_2D
                is BufferMultisample.SampleCount -> GL_TEXTURE_2D_MULTISAMPLE
            }
            glBindTexture(target, glTexture)
            checkGLErrors()
            val nullBuffer: ByteBuffer? = null

            when (multisample) {
                BufferMultisample.DISABLED -> {
                    glTexImage2D(GL_TEXTURE_2D, 0, format.toGLFormat(), width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, nullBuffer)
                    checkGLErrors()
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                }
                is BufferMultisample.SampleCount -> {
                    glTexImage2DMultisample(target, multisample.sampleCount.coerceAtMost(glGetInteger(GL_MAX_DEPTH_TEXTURE_SAMPLES)), format.toGLFormat(), width, height, true)
                    checkGLErrors()
                }
            }



            return DepthBufferGL3(glTexture, target, width, height, format, multisample)
        }
    }

    override fun destroy() {
        glDeleteTextures(texture)
    }

    override fun bind(textureUnit: Int) {
        glActiveTexture(GL_TEXTURE0 + textureUnit)
        glBindTexture(target, texture)
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

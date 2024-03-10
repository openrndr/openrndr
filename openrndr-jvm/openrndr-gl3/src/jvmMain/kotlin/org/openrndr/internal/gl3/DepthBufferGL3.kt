package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL33C.*
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {  }

class DepthBufferGL3(
    val texture: Int,
    val buffer: Int,
    val target: Int,
    override val width: Int,
    override val height: Int,
    override val format: DepthFormat,
    override val multisample: BufferMultisample,
    override val session: Session?
) : DepthBuffer {

    private var destroyed = false

    companion object {
        fun create(
            width: Int,
            height: Int,
            format: DepthFormat,
            multisample: BufferMultisample,
            session: Session?
        ): DepthBufferGL3 {
            checkGLErrors {
                "pre-existing error"
            }
            val glTexture = glGenTextures()
            val target = when (multisample) {
                BufferMultisample.Disabled -> GL_TEXTURE_2D
                is BufferMultisample.SampleCount -> GL_TEXTURE_2D_MULTISAMPLE
            }
            glBindTexture(target, glTexture)
            checkGLErrors()
            val nullBuffer: ByteBuffer? = null

            return when (Driver.glType) {
                DriverTypeGL.GLES -> {
                    val glFormat = when (format) {
                        DepthFormat.DEPTH16, DepthFormat.DEPTH24, DepthFormat.DEPTH32F -> GL_DEPTH_COMPONENT
                        DepthFormat.DEPTH_STENCIL, DepthFormat.DEPTH24_STENCIL8, DepthFormat.DEPTH32F_STENCIL8 -> GL_DEPTH_STENCIL
                        DepthFormat.STENCIL8 -> {
                            (Driver.instance as DriverGL3).version.require(DriverVersionGL.GL_VERSION_4_4); GL_STENCIL_INDEX
                        }
                    }
                    val type = when (format) {
                        DepthFormat.DEPTH32F -> GL_DEPTH_COMPONENT32F
                        DepthFormat.DEPTH24 -> GL_DEPTH_COMPONENT24
                        DepthFormat.DEPTH32F_STENCIL8 -> GL_DEPTH32F_STENCIL8
                        DepthFormat.DEPTH_STENCIL, DepthFormat.DEPTH24_STENCIL8 -> GL_DEPTH24_STENCIL8
                        else -> error("format $format not supported")
                    }
                    when (multisample) {
                        BufferMultisample.Disabled -> {
                            val buffer = glGenRenderbuffers()
                            glBindRenderbuffer(GL_RENDERBUFFER, buffer)
                            glRenderbufferStorage(
                                GL_RENDERBUFFER,
                                type,
                                width,
                                height
                            )
                            checkGLErrors {
                                logger.error {
                                    """failed to create depth buffer. width: $width, height: $height, format: $format, multisample: $multisample"""
                                }
                                when (it) {
                                    GL_INVALID_ENUM -> "$format not supported?"
                                    else -> null
                                }
                            }

                            glBindRenderbuffer(GL_RENDERBUFFER, 0)
                            DepthBufferGL3(-1, buffer, target, width, height, format, multisample, session)
                        }

                        is BufferMultisample.SampleCount -> {
                            val buffer = glGenRenderbuffers()
                            glBindRenderbuffer(GL_RENDERBUFFER, buffer)
                            glRenderbufferStorageMultisample(
                                GL_RENDERBUFFER,
                                multisample.sampleCount,
                                GL_DEPTH32F_STENCIL8,
                                width,
                                height
                            )
                            checkGLErrors() {
                                logger.error {
                                    """failed to create depth buffer. width: $width, height: $height, format: $format, multisample: $multisample"""
                                }
                                null
                            }
                            glBindRenderbuffer(GL_RENDERBUFFER, 0)
                            DepthBufferGL3(-1, buffer, target, width, height, format, multisample, session)
                        }
                    }
                }

                DriverTypeGL.GL -> {
                    when (multisample) {
                        BufferMultisample.Disabled -> {
                            val glFormat = when (format) {
                                DepthFormat.DEPTH16, DepthFormat.DEPTH24, DepthFormat.DEPTH32F -> GL_DEPTH_COMPONENT
                                DepthFormat.DEPTH_STENCIL, DepthFormat.DEPTH24_STENCIL8, DepthFormat.DEPTH32F_STENCIL8 -> GL_DEPTH_COMPONENT
                                DepthFormat.STENCIL8 -> {
                                    (Driver.instance as DriverGL3).version.require(DriverVersionGL.GL_VERSION_4_4); GL_STENCIL_INDEX
                                }
                            }
                            glTexImage2D(/* target = */ GL_TEXTURE_2D,
                                /* level = */ 0,
                                /* internalformat = */ format.toGLFormat(),
                                /* width = */ width,
                                /* height = */ height,
                                /* border = */ 0,
                                /* format = */ glFormat,
                                /* type = */ GL_UNSIGNED_BYTE,
                                /* pixels = */
                                nullBuffer
                            )
                            checkGLErrors() {
                                logger.error { """glTexImage2D failed. format: $format, multisample: $multisample
                                    |internalformat=${glEnumName(format.toGLFormat())}, format=${glEnumName(glFormat)}
                                """.trimMargin() }
                                null
                            }
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                            DepthBufferGL3(glTexture, -1, target, width, height, format, multisample, session)
                        }

                        is BufferMultisample.SampleCount -> {
                            glTexImage2DMultisample(
                                target,
                                multisample.sampleCount.coerceAtMost(glGetInteger(GL_MAX_DEPTH_TEXTURE_SAMPLES)),
                                format.toGLFormat(),
                                width,
                                height,
                                true
                            )
                            checkGLErrors {
                                logger.error { """glTexImage2D failed. format: $format, multisample: $multisample
                                    |internalformat=${glEnumName(format.toGLFormat())}}
                                """.trimMargin() }
                                null
                            }
                            DepthBufferGL3(glTexture, -1, target, width, height, format, multisample, session)
                        }
                    }
                }
            }
        }
    }

    override fun resolveTo(target: DepthBuffer) {
        val readTarget = renderTarget(width, height, multisample = multisample) {
            depthBuffer(this@DepthBufferGL3)
        } as RenderTargetGL3

        val writeTarget = renderTarget(target.width, target.height, multisample = target.multisample) {
            depthBuffer(target)
        } as RenderTargetGL3

        writeTarget.bind()
        glBindFramebuffer(GL_READ_FRAMEBUFFER, readTarget.framebuffer)
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_DEPTH_BUFFER_BIT, GL_NEAREST)
        writeTarget.unbind()

        writeTarget.detachColorAttachments()
        writeTarget.destroy()

        readTarget.detachColorAttachments()
        readTarget.destroy()

    }

    override fun copyTo(target: DepthBuffer) {
        require(!destroyed)
        val readTarget = renderTarget(width, height) {
            depthBuffer(this@DepthBufferGL3)
        } as RenderTargetGL3

        target as DepthBufferGL3
        readTarget.bind()
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        target.bound {
            glCopyTexSubImage2D(target.target, 0, 0, 0, 0, 0, target.width, target.height)
            debugGLErrors()
        }
        readTarget.unbind()
        readTarget.detachDepthBuffer()
        readTarget.destroy()
    }

    fun bound(f: DepthBufferGL3.() -> Unit) {
        require(!destroyed)
        glActiveTexture(GL_TEXTURE0)
        val current = when (multisample) {
            BufferMultisample.Disabled -> glGetInteger(GL_TEXTURE_BINDING_2D)
            is BufferMultisample.SampleCount -> glGetInteger(GL_TEXTURE_BINDING_2D_MULTISAMPLE)
        }
        glBindTexture(target, texture)
        this.f()
        glBindTexture(target, current)
    }

    override fun destroy() {
        if (!destroyed) {
            destroyed = true
            glDeleteTextures(texture)
        }
    }

    override fun bind(textureUnit: Int) {
        require(!destroyed)
        glActiveTexture(GL_TEXTURE0 + textureUnit)
        glBindTexture(target, texture)
    }
}

private fun DepthFormat.toGLFormat(): Int {
    return when (this) {
        DepthFormat.DEPTH16 -> GL_DEPTH_COMPONENT16
        DepthFormat.DEPTH24_STENCIL8 -> GL_DEPTH24_STENCIL8
        DepthFormat.DEPTH24 -> GL_DEPTH_COMPONENT24
        DepthFormat.DEPTH32F -> GL_DEPTH_COMPONENT32F
        DepthFormat.DEPTH32F_STENCIL8 -> GL_DEPTH32F_STENCIL8
        DepthFormat.STENCIL8 -> GL_STENCIL_INDEX8
        DepthFormat.DEPTH_STENCIL -> GL_DEPTH_STENCIL
    }
}

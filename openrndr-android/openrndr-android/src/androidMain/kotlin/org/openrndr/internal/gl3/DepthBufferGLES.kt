package org.openrndr.internal.gl3

import android.opengl.GLES30
import android.opengl.GLES32
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.BufferMultisample.Disabled
import org.openrndr.draw.BufferMultisample.SampleCount
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.Session

private val logger = KotlinLogging.logger {}

/**
 * GLES implementation of a depth/stencil attachment. Uses a renderbuffer (no texture) on GLES,
 * like the GLES branch of DepthBufferGL3. Thus, bind() and texture-based copy are unsupported.
 */
class DepthBufferGLES(
    /** Always -1 on GLES (we use renderbuffers, not depth textures). */
    val texture: Int,
    /** GL renderbuffer name. */
    val buffer: Int,
    /** Not used for GLES renderbuffers (kept for API shape parity). */
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
        ): DepthBufferGLES {
            // Create a renderbuffer storage for depth (and/or stencil) in GLES
            val rb = IntArray(1)
            GLES30.glGenRenderbuffers(1, rb, 0)
            require(rb[0] != 0) { "glGenRenderbuffers failed" }

            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rb[0])

            val internalFormat = when (format) {
                DepthFormat.DEPTH16 -> GLES30.GL_DEPTH_COMPONENT16
                DepthFormat.DEPTH24 -> GLES30.GL_DEPTH_COMPONENT24    // ES3
                DepthFormat.DEPTH32F -> {
                    // Depth-only 32F renderbuffer is not core in ES3; prefer packed depth-stencil below.
                    // We throw here so the caller can fall back to DEPTH24/DEPTH24_STENCIL8 if needed.
                    throw IllegalArgumentException("DEPTH32F renderbuffer not supported on GLES. Use DEPTH24 or DEPTH24_STENCIL8.")
                }

                DepthFormat.DEPTH_STENCIL -> GLES30.GL_DEPTH24_STENCIL8     // treat as packed
                DepthFormat.DEPTH24_STENCIL8 -> GLES30.GL_DEPTH24_STENCIL8
                DepthFormat.DEPTH32F_STENCIL8 -> {
                    // ES3 supports GL_DEPTH32F_STENCIL8 for renderbufferStorage(MULTISAMPLE) on many devices;
                    // if device lacks it, this will fail at runtime and we’ll log.
                    GLES32.GL_DEPTH32F_STENCIL8
                }

                DepthFormat.STENCIL8 -> GLES30.GL_STENCIL_INDEX8
            }

            when (multisample) {
                Disabled -> {
                    GLES30.glRenderbufferStorage(
                        GLES30.GL_RENDERBUFFER,
                        internalFormat,
                        width,
                        height
                    )
                    val err = GLES30.glGetError()
                    if (err != GLES30.GL_NO_ERROR) {
                        logger.error { "glRenderbufferStorage failed: 0x${err.toString(16)} (format=$format, ${width}x$height)" }
                        require(false) { "Failed to create depth renderbuffer" }
                    }
                }

                is SampleCount -> {
                    // clamp to GL_MAX_SAMPLES
                    val maxSamplesArr = IntArray(1)
                    GLES30.glGetIntegerv(GLES30.GL_MAX_SAMPLES, maxSamplesArr, 0)
                    val samples = multisample.sampleCount.coerceAtMost(maxSamplesArr[0])

                    GLES30.glRenderbufferStorageMultisample(
                        GLES30.GL_RENDERBUFFER,
                        samples,
                        internalFormat,
                        width,
                        height
                    )
                    val err = GLES30.glGetError()
                    if (err != GLES30.GL_NO_ERROR) {
                        logger.error {
                            "glRenderbufferStorageMultisample failed: 0x${err.toString(16)} " +
                                    "(format=$format, samples=$samples, ${width}x$height)"
                        }
                        require(false) { "Failed to create multisample depth renderbuffer" }
                    }
                }
            }

            // Unbind for cleanliness
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0)

            // texture = -1 (no depth texture on GLES path), target is not meaningful; keep 0
            return DepthBufferGLES(
                texture = -1,
                buffer = rb[0],
                target = 0,
                width = width,
                height = height,
                format = format,
                multisample = multisample,
                session = session
            )
        }
    }

    /**
     * Resolve depth from this to another depth buffer.
     * On GLES we do this via FBO blit DEPTH buffer bit.
     */
    override fun resolveTo(target: DepthBuffer) {
        require(!destroyed)
        require(target is DepthBufferGLES) { "resolveTo expects a DepthBufferGLES target" }

        val readFB = IntArray(1)
        val drawFB = IntArray(1)
        GLES30.glGenFramebuffers(1, readFB, 0)
        GLES30.glGenFramebuffers(1, drawFB, 0)

        try {
            // Attach our renderbuffer as READ depth/stencil
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, readFB[0])
            attachDepthStencilRenderbuffer(multisample, format, buffer, forRead = true)

            // Attach target renderbuffer as DRAW depth/stencil
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, drawFB[0])
            attachDepthStencilRenderbuffer(
                target.multisample,
                target.format,
                target.buffer,
                forRead = false
            )

            // Blit depth (and stencil if present) — GLES allows DEPTH_BUFFER_BIT, STENCIL may vary by driver.
            val mask = when (formatHasStencil(format) || formatHasStencil(target.format)) {
                true -> GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_STENCIL_BUFFER_BIT
                false -> GLES30.GL_DEPTH_BUFFER_BIT
            }

            GLES30.glBlitFramebuffer(
                0, 0, width, height,
                0, 0, target.width, target.height,
                mask,
                GLES30.GL_NEAREST
            )
        } finally {
            // restore and delete
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, 0)
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
            GLES30.glDeleteFramebuffers(1, readFB, 0)
            GLES30.glDeleteFramebuffers(1, drawFB, 0)
        }
    }

    /**
     * Copy depth into target. On GLES, we only support renderbuffer→renderbuffer via blit.
     * If the target is expecting a depth *texture*, that isn’t supported on this GLES path.
     */
    override fun copyTo(target: DepthBuffer) {
        // Alias to resolveTo; GLES has no direct glCopyTexSubImage2D for renderbuffer depth.
        resolveTo(target)
    }

    private fun attachDepthStencilRenderbuffer(
        ms: BufferMultisample,
        fmt: DepthFormat,
        rb: Int,
        forRead: Boolean
    ) {
        val fbTarget = if (forRead) GLES30.GL_READ_FRAMEBUFFER else GLES30.GL_DRAW_FRAMEBUFFER
        GLES30.glFramebufferRenderbuffer(
            fbTarget,
            when {
                formatHasDepth(fmt) && formatHasStencil(fmt) -> GLES30.GL_DEPTH_STENCIL_ATTACHMENT
                formatHasDepth(fmt) -> GLES30.GL_DEPTH_ATTACHMENT
                else -> GLES30.GL_STENCIL_ATTACHMENT
            },
            GLES30.GL_RENDERBUFFER,
            rb
        )

        val status = GLES30.glCheckFramebufferStatus(fbTarget)
        require(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            "Depth FBO incomplete for ${if (forRead) "READ" else "DRAW"} (status=0x${
                status.toString(
                    16
                )
            })"
        }
    }

    private fun formatHasDepth(fmt: DepthFormat): Boolean = when (fmt) {
        DepthFormat.DEPTH16, DepthFormat.DEPTH24, DepthFormat.DEPTH32F,
        DepthFormat.DEPTH_STENCIL, DepthFormat.DEPTH24_STENCIL8, DepthFormat.DEPTH32F_STENCIL8 -> true

        DepthFormat.STENCIL8 -> false
    }

    private fun formatHasStencil(fmt: DepthFormat): Boolean = when (fmt) {
        DepthFormat.DEPTH_STENCIL, DepthFormat.DEPTH24_STENCIL8, DepthFormat.DEPTH32F_STENCIL8, DepthFormat.STENCIL8 -> true
        DepthFormat.DEPTH16, DepthFormat.DEPTH24, DepthFormat.DEPTH32F -> false
    }

    /**
     * Binding as a texture is not supported on GLES path (we use renderbuffers).
     */
    override fun bind(textureUnit: Int) {
        error("DepthBufferGLES has no texture target; cannot bind as a texture")
    }

    override fun destroy() {
        if (!destroyed) {
            destroyed = true
            session?.untrack(this)
            if (buffer != 0 && buffer != -1) {
                val rb = intArrayOf(buffer)
                GLES30.glDeleteRenderbuffers(1, rb, 0)
            }
        }
    }

    override fun close() {
        destroy()
    }
}
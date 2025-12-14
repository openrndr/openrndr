package org.openrndr.internal.gl3

import android.opengl.GLES30
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import java.lang.IllegalStateException
import java.util.Stack
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * OpenGL ES 3.0 RenderTarget implementation.
 * - Color attachments: 2D textures only (GLES 3.0).
 * - Depth attachment: depth renderbuffer OR depth texture (if your DepthBufferGLES exposes one).
 * - Draw buffers: supported via glDrawBuffers (GLES 3.0).
 * - Layered/cubemap/volume attachments: NOT supported on GLES 3.0 (throws).
 * - Per-target blend (glBlendFunci/Equationi): NOT supported on GLES 3.0 (uses global blend state).
 * - Multisample: currently not implemented (throws if requested).
 */
open class RenderTargetGLES(
    val framebuffer: Int,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val multisample: BufferMultisample,
    override val session: Session?,
    private val contextID: Long = Driver.instance.contextID
) : RenderTarget {

    var destroyed = false
        private set
    private var ownDepthBuffer = false

    override val colorAttachments: MutableList<ColorAttachment> = mutableListOf()
    override var depthBuffer: DepthBuffer? = null

    private var bound = false

    companion object {
        // Active RT stacks per GL context
        private val active = mutableMapOf<Long, Stack<RenderTargetGLES>>()

        fun create(
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            multisample: BufferMultisample = BufferMultisample.Disabled,
            session: Session?
        ): RenderTargetGLES {
            logger.trace { "created new GLES render target ($width*$height) @ ${contentScale}x $multisample" }
            if (multisample is BufferMultisample.SampleCount) {
                // GLES 3.0 supports multisample renderbuffers, but this implementation doesn’t yet.
                logger.warn { "GLES multisample requested (${multisample.sampleCount}) but RenderTargetGLES has no MSAA path yet; throwing." }
                throw UnsupportedOperationException("RenderTargetGLES multisample not implemented")
            }
            val fb = genFramebuffer()
            return RenderTargetGLES(fb, width, height, contentScale, multisample, session)
        }

        val activeRenderTarget: RenderTargetGLES
            get() {
                val ctx = Driver.instance.contextID
                val stack = active.getOrPut(ctx) {
                    logger.debug { "creating active GLES RT stack for context $ctx" }
                    Stack()
                }
                if (stack.isEmpty()) {
                    logger.error { "empty RT stack for context $ctx" }
                }
                return stack.peek()
            }

        private fun genFramebuffer(): Int {
            val a = IntArray(1)
            GLES30.glGenFramebuffers(1, a, 0)
            return a[0]
        }
    }

    override val hasColorAttachments: Boolean get() = colorAttachments.isNotEmpty()
    override val hasDepthBuffer: Boolean get() = depthBuffer?.hasDepth == true
    override val hasStencilBuffer: Boolean get() = depthBuffer?.hasStencil == true

    override fun colorBuffer(index: Int): ColorBuffer {
        require(!destroyed)
        return (colorAttachments[index] as? ColorBufferAttachment)?.colorBuffer
            ?: error("attachment at $index is not a ColorBuffer")
    }

    override fun bind() {
        require(!destroyed)
        require(Driver.instance.contextID == contextID) {
            "can't bind RT on context ${Driver.instance.contextID}, created on $contextID"
        }
        if (bound) error("already bound")
        val stack = active.getOrPut(contextID) { Stack() }
        stack.push(this)
        bindTarget()
        bound = true
    }

    open fun bindTarget() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)

        if (Driver.instance.contextID != contextID) {
            throw IllegalStateException("RT created by $contextID cannot be bound to ${Driver.instance.contextID}")
        }

        if (colorAttachments.isNotEmpty()) {
            // set draw buffers for active attachments
            val dbufs = IntArray(colorAttachments.size) { i -> GLES30.GL_COLOR_ATTACHMENT0 + i }
            GLES30.glDrawBuffers(dbufs.size, dbufs, 0)
        } else {
            // draw to none
            GLES30.glDrawBuffers(0, IntArray(0), 0)
        }

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()
        logger.trace { "GLES viewport -> (0, 0, $effectiveWidth, $effectiveHeight)" }
        GLES30.glViewport(0, 0, effectiveWidth, effectiveHeight)
        checkGl()
    }

    override fun unbind() {
        if (!bound) {
            val stack = active.getOrPut(contextID) { Stack() }
            stack.pop()
            val previous = stack.peek()
            logger.trace { "restoring to previous GLES RT $previous" }
            Driver.instance.finish()
            previous.bindTarget()
        } else {
            // mirror GL3 behavior: this branch is an error state
            throw RuntimeException("target not bound")
        }
    }

    override fun attach(colorBuffer: ColorBuffer, level: Int, name: String?, ownedByRenderTarget: Boolean) {
        require(!destroyed)
        // GLES cannot render to RGB (3-component) textures as attachments; enforce that.
        require(colorBuffer.format.componentCount != 3) {
            "attachments with format (=${colorBuffer.format}) are not supported in GLES mode"
        }

        val div = 1 shl level
        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(colorBuffer.effectiveWidth / div == effectiveWidth && colorBuffer.effectiveHeight / div == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($width x $height @${colorBuffer.contentScale}x, got: (${colorBuffer.width / div} x ${colorBuffer.height / div} @${colorBuffer.contentScale}x level:$level)")
        }

        // ---- Bind and attach
        bindTarget()
        val cb = colorBuffer as ColorBufferGLES // adapt if your class name differs
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0 + colorAttachments.size,
            GLES30.GL_TEXTURE_2D,        // GLES path: 2D textures only
            cb.textureId,
            level
        )
        checkFramebufferStatus()

        colorAttachments.add(
            ColorBufferAttachment(
                colorAttachments.size,
                name,
                colorBuffer,
                level,
                ownedByRenderTarget
            )
        )

        // re-bind to apply draw buffers/viewport if needed
        bindTarget()
    }

    // ---- Desktop-only attachment types; explicitly unsupported on GLES 3.0

    override fun attach(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int, name: String?) {
        throw UnsupportedOperationException("ArrayCubemap attachments are not supported on OpenGL ES 3.0")
    }

    override fun attach(cubemap: Cubemap, side: CubemapSide, level: Int, name: String?) {
        throw UnsupportedOperationException("Cubemap face attachments are not supported on OpenGL ES 3.0")
    }

    override fun attach(volumeTexture: VolumeTexture, layer: Int, level: Int, name: String?) {
        throw UnsupportedOperationException("VolumeTexture attachments are not supported on OpenGL ES 3.0")
    }

    override fun attachLayered(arrayTexture: ArrayTexture, level: Int, name: String?) {
        throw UnsupportedOperationException("Layered ArrayTexture attachments are not supported on OpenGL ES 3.0")
    }

    override fun attachLayered(arrayCubemap: ArrayCubemap, level: Int, name: String?) {
        throw UnsupportedOperationException("Layered ArrayCubemap attachments are not supported on OpenGL ES 3.0")
    }

    override fun attachLayered(cubemap: Cubemap, level: Int, name: String?) {
        throw UnsupportedOperationException("Layered Cubemap attachments are not supported on OpenGL ES 3.0")
    }

    override fun attachLayered(volumeTexture: VolumeTexture, level: Int, name: String?) {
        throw UnsupportedOperationException("Layered VolumeTexture attachments are not supported on OpenGL ES 3.0")
    }

    override fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int, name: String?) {
        throw UnsupportedOperationException("ArrayTexture layer attachments are not supported on OpenGL ES 3.0")
    }

    // ---- Blending per color attachment is desktop-only pre-ES 3.2; use global blend state

    override fun blendMode(index: Int, blendMode: BlendMode) {
        when (blendMode) {
            BlendMode.OVER -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                // classic alpha blend
                GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
                GLES30.glBlendFuncSeparate(
                    GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA,
                    GLES30.GL_ONE,       GLES30.GL_ONE_MINUS_SRC_ALPHA
                )
            }
            BlendMode.BLEND -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
                GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            }
            BlendMode.REPLACE -> {
                // effectively disable blending
                GLES30.glDisable(GLES30.GL_BLEND)
                GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
                GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ZERO)
            }
            BlendMode.ADD -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
                GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)
            }
            BlendMode.MIN -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendEquation(GLES30.GL_MIN)
                GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)
            }
            BlendMode.MAX -> {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendEquation(GLES30.GL_MAX)
                GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)
            }
            else -> error("unsupported blend mode on GLES: $blendMode")
        }
    }

    private fun bound(function: () -> Unit) {
        bind()
        function()
        unbind()
    }

    override fun clearColor(index: Int, color: ColorRGBa) {
        // GLES3 supports glClearBufferfv; we must bind FBO then clear the requested draw buffer
        val type = (colorAttachments[index] as ColorBufferAttachment).colorBuffer.type
        val targetColor = color.toLinearity(if (type.isSRGB) Linearity.SRGB else Linearity.LINEAR)

        require(!destroyed)
        bound {
            val ca = floatArrayOf(
                targetColor.r.toFloat(),
                targetColor.g.toFloat(),
                targetColor.b.toFloat(),
                targetColor.alpha.toFloat()
            )
            GLES30.glClearBufferfv(GLES30.GL_COLOR, index, ca, 0)
        }
    }

    override fun clearDepth(depth: Double, stencil: Int) {
        require(!destroyed)
        bound {
            // If depth write mask is disabled, temporarily enable to allow clear
            val mask = intArrayOf(0)
            GLES30.glGetIntegerv(GLES30.GL_DEPTH_WRITEMASK, mask, 0)
            val depthWriteWasEnabled = (mask[0] != 0)
            if (!depthWriteWasEnabled) {
                GLES30.glDepthMask(true)
            }

            if (hasDepthBuffer && hasStencilBuffer) {
                // GLES3 has glClearBufferfi for DEPTH_STENCIL
                GLES30.glClearBufferfi(GLES30.GL_DEPTH_STENCIL, 0, depth.toFloat(), stencil)
            } else if (hasDepthBuffer) {
                val fb = floatArrayOf(depth.toFloat())
                GLES30.glClearBufferfv(GLES30.GL_DEPTH, 0, fb, 0)
            } else if (hasStencilBuffer) {
                val ib = intArrayOf(stencil)
                GLES30.glClearBufferiv(GLES30.GL_STENCIL, 0, ib, 0)
            }

            if (!depthWriteWasEnabled) {
                GLES30.glDepthMask(false)
            }
            checkGl()
        }
    }

    override fun attach(depthBuffer: DepthBuffer, ownedByRenderTarget: Boolean) {
        require(!destroyed)
        require(this.depthBuffer == null) { "a depth buffer is already attached" }

        ownDepthBuffer = ownedByRenderTarget

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()
        if (!(depthBuffer.width == effectiveWidth && depthBuffer.height == effectiveHeight)) {
            throw IllegalArgumentException("depth buffer dimension mismatch")
        }
        if (depthBuffer.multisample != multisample) {
            throw IllegalArgumentException("buffer multisample mismatch")
        }

        bound {
            depthBuffer as DepthBufferGLES

            if (depthBuffer.texture != 0) {
                // Depth as texture (if your DepthBufferGLES exposes it)
                if (depthBuffer.hasDepth) {
                    GLES30.glFramebufferTexture2D(
                        GLES30.GL_FRAMEBUFFER,
                        GLES30.GL_DEPTH_ATTACHMENT,
                        GLES30.GL_TEXTURE_2D, // GLES path assumes 2D
                        depthBuffer.texture,
                        0
                    )
                }
                if (depthBuffer.hasStencil) {
                    GLES30.glFramebufferTexture2D(
                        GLES30.GL_FRAMEBUFFER,
                        GLES30.GL_STENCIL_ATTACHMENT,
                        GLES30.GL_TEXTURE_2D,
                        depthBuffer.texture,
                        0
                    )
                }
            } else {
                // Depth/stencil as renderbuffer
                when (Pair(depthBuffer.hasDepth, depthBuffer.hasStencil)) {
                    Pair(true, true) -> GLES30.glFramebufferRenderbuffer(
                        GLES30.GL_FRAMEBUFFER,
                        GLES30.GL_DEPTH_STENCIL_ATTACHMENT,
                        GLES30.GL_RENDERBUFFER,
                        depthBuffer.buffer
                    )
                    Pair(false, true) -> GLES30.glFramebufferRenderbuffer(
                        GLES30.GL_FRAMEBUFFER,
                        GLES30.GL_STENCIL_ATTACHMENT,
                        GLES30.GL_RENDERBUFFER,
                        depthBuffer.buffer
                    )
                    Pair(true, false) -> GLES30.glFramebufferRenderbuffer(
                        GLES30.GL_FRAMEBUFFER,
                        GLES30.GL_DEPTH_ATTACHMENT,
                        GLES30.GL_RENDERBUFFER,
                        depthBuffer.buffer
                    )
                    else -> error("DepthBuffer should have at least depth or stencil components")
                }
            }
            this.depthBuffer = depthBuffer
            checkFramebufferStatus()
        }
    }

    override fun detachDepthBuffer() {
        require(!destroyed)
        this.depthBuffer?.let { db ->
            val gles = db as DepthBufferGLES // adapt
            bound {
                // detach both depth + stencil if present
                GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,  GLES30.GL_TEXTURE_2D, 0, 0)
                GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_STENCIL_ATTACHMENT, GLES30.GL_TEXTURE_2D, 0, 0)
                // If attached as renderbuffer (common case), detach the RBO by setting name 0
                GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,   GLES30.GL_RENDERBUFFER, 0)
                GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_STENCIL_ATTACHMENT, GLES30.GL_RENDERBUFFER, 0)
                checkGl()
            }
        }
        depthBuffer = null
    }

    override fun detachColorAttachments() {
        require(!destroyed)
        bound {
            for ((index, _) in colorAttachments.withIndex()) {
                GLES30.glFramebufferTexture2D(
                    GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_COLOR_ATTACHMENT0 + index,
                    GLES30.GL_TEXTURE_2D,
                    0,
                    0
                )
            }
            checkGl()
        }
        colorAttachments.clear()
    }

    @Deprecated(
        "detachColorBuffer is deprecated, use detachColorAttachments",
        replaceWith = ReplaceWith("detachColorAttachments")
    )
    override fun detachColorBuffers() {
        detachColorAttachments()
    }

    override fun destroy() {
        for (attachment in colorAttachments) {
            if (attachment.ownedByRenderTarget) {
                when (attachment) {
                    is ColorBufferAttachment -> attachment.colorBuffer.destroy()
                    else -> {}
                }
            }
        }
        if (ownDepthBuffer) {
            depthBuffer?.destroy()
        }

        detachColorAttachments()
        detachDepthBuffer()

        session?.untrack(this)
        destroyed = true
        val a = intArrayOf(framebuffer)
        GLES30.glDeleteFramebuffers(1, a, 0)
    }

    override fun toString(): String =
        "RenderTargetGLES(framebuffer=$framebuffer, width=$width, height=$height, contentScale=$contentScale, multisample=$multisample, session=$session, destroyed=$destroyed, colorAttachments=$colorAttachments, depthBuffer=$depthBuffer)"

    override fun close() = destroy()

    // --- internals

    private fun checkFramebufferStatus() {
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            val reason = when (status) {
                GLES30.GL_FRAMEBUFFER_UNDEFINED -> "Framebuffer undefined"
                GLES30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "Attachment incomplete"
                GLES30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "Attachment missing"
                // GLES 3.0 does not define INCOMPLETE_DRAW_BUFFER; we rely on COMPLETE check.
                GLES30.GL_FRAMEBUFFER_UNSUPPORTED -> "Unsupported attachment combination"
                GLES30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "Incomplete multisample"
                else -> "0x${status.toString(16)}"
            }
            throw IllegalStateException("GLES framebuffer not complete: $reason")
        }
        checkGl()
    }

    private fun checkGl() {
        val err = GLES30.glGetError()
        if (err != GLES30.GL_NO_ERROR) {
            logger.warn { "GLES error: 0x${err.toString(16)}" }
        }
    }
}
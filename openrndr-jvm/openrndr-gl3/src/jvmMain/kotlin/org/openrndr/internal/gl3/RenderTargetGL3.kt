package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL32C.GL_MAX_COLOR_TEXTURE_SAMPLES
import org.lwjgl.opengl.GL32C.GL_MAX_DEPTH_TEXTURE_SAMPLES
import org.lwjgl.system.MemoryStack
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import java.util.*
import kotlin.math.min

private val logger = KotlinLogging.logger {}

private val active = mutableMapOf<Long, Stack<RenderTargetGL3>>()

class NullRenderTargetGL3 : RenderTargetGL3(0, 640, 480, 1.0, BufferMultisample.Disabled, Session.root)

class ProgramRenderTargetGL3(override val program: Program) : ProgramRenderTarget,
    RenderTargetGL3(glGetInteger(GL_FRAMEBUFFER_BINDING), 0, 0, 1.0, BufferMultisample.Disabled, Session.root) {
    private var cachedSize = program.window.size
    private var cachedContentScale = program.window.contentScale

    override val width: Int
        get() = cachedSize.x.toInt()

    override val height: Int
        get() = cachedSize.y.toInt()

    override val contentScale: Double
        get() = cachedContentScale

    override val hasColorAttachments = true
    override val hasDepthBuffer = true
    override val hasStencilBuffer = true

    override val multisample: BufferMultisample
        get() = program.window.multisample.bufferEquivalent()

    override fun bindTarget() {
        cachedSize = program.window.size
        cachedContentScale = program.window.contentScale
        super.bindTarget()
    }
}

open class RenderTargetGL3(
    val framebuffer: Int,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val multisample: BufferMultisample,
    override val session: Session?,
    private val contextID: Long = Driver.instance.contextID
) : RenderTarget {
    var destroyed = false
    private var ownDepthBuffer = false
    override val colorAttachments: MutableList<ColorAttachment> = mutableListOf()
    override var depthBuffer: DepthBuffer? = null

    companion object {
        fun create(
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            multisample: BufferMultisample = BufferMultisample.Disabled,
            session: Session?
        ): RenderTargetGL3 {
            logger.trace { "created new render target ($width*$height) @ ${contentScale}x $multisample" }
            val framebuffer = glGenFramebuffers()
            if (multisample is BufferMultisample.SampleCount) {
                val maxSamples = glGetInteger(GL_MAX_SAMPLES)
                if (maxSamples < multisample.sampleCount) {
                    logger.info {
                        "requested multisampling with ${multisample.sampleCount} samples, but only ${maxSamples} are supported"
                    }
                }

                BufferMultisample.SampleCount(min(multisample.sampleCount, maxSamples))
            }

            return RenderTargetGL3(framebuffer, width, height, contentScale, multisample, session)
        }

        val activeRenderTarget: RenderTargetGL3
            get() {
                val stack = active.getOrPut(Driver.instance.contextID) {
                    logger.debug { "creating active render target stack for context ${Driver.instance.contextID}" }
                    Stack()
                }
                if (stack.isEmpty()) {
                    logger.error { "empty stack while looking for active render target for context ${Driver.instance.contextID}" }
                }
                return stack.peek()
            }
    }

    private var bound = false

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
        require(Driver.instance.contextID == contextID) { "can't bind render target on context ${Driver.instance.contextID} it is created on ${contextID}" }
        if (bound) {
            throw RuntimeException("already bound")
        } else {
            val stack = active.getOrPut(Driver.instance.contextID) { Stack() }
            stack.push(this)
            bindTarget()
        }
    }

    open fun bindTarget() {
        debugGLErrors { null }
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

        if (Driver.instance.contextID != contextID) {
            throw IllegalStateException("this render target is created by $contextID and cannot be bound to ${Driver.instance.contextID}")
        }

        debugGLErrors { null }
        if (colorAttachments.isNotEmpty()) {
            val drawBuffers = List(colorAttachments.size) { index -> GL_COLOR_ATTACHMENT0 + index }.toIntArray()
            glDrawBuffers(drawBuffers)
            debugGLErrors {
                when (it) {
                    GL_INVALID_ENUM -> "1. one of the values in bufs is not an accepted value\n2. the API call refers to the default framebuffer and one or more of the values in bufs is one of the GL_COLOR_ATTACHMENTn tokens\n3. the API call refers to a framebuffer object and one or more of the values in bufs is anything other than GL_NONE or one of the GL_COLOR_ATTACHMENTn tokens\n4. n is less than 0"
                    GL_INVALID_OPERATION -> "a symbolic constant other than GL_NONE appears more than once in bufs."
                    GL_INVALID_VALUE -> "1. n is greater than GL_MAX_DRAW_BUFFERS\n 2. any of the entries in bufs (other than GL_NONE ) indicates a color buffer that does not exist in the current GL context\n 3. any value in bufs is GL_BACK, and n is not one"
                    else -> null
                }
            }
        }

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        logger.trace { "setting viewport to (0, 0, $effectiveWidth, $effectiveHeight)" }
        glViewport(0, 0, effectiveWidth, effectiveHeight)
        debugGLErrors { null }
    }

    override fun unbind() {
        if (!bound) {
            val previous = active.getOrPut(Driver.instance.contextID) { Stack() }.let {
                it.pop()
                it.peek()
            }
            previous as RenderTargetGL3
            logger.trace { "restoring to previous render target $previous" }
            previous.bindTarget()
            bound = false
        } else {
            throw RuntimeException("target not bound")
        }
    }

    override fun attach(colorBuffer: ColorBuffer, level: Int, name: String?, ownedByRenderTarget: Boolean) {
        require(!destroyed)

        val context = Driver.instance.contextID
        bindTarget()

        val div = 1 shl level
        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(colorBuffer.effectiveWidth / div == effectiveWidth && colorBuffer.effectiveHeight / div == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($width x $height @${colorBuffer.contentScale}x, got: (${colorBuffer.width / div} x ${colorBuffer.height / div} @${colorBuffer.contentScale}x level:${level})")
        }
        colorBuffer as ColorBufferGL3
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0 + colorAttachments.size,
            colorBuffer.target,
            colorBuffer.texture,
            level
        )
        debugGLErrors { null }

        colorAttachments.add(
            ColorBufferAttachment(
                colorAttachments.size,
                name,
                colorBuffer,
                level,
                ownedByRenderTarget
            )
        )

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attach(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int, name: String?) {
        require(!destroyed)
        val context = Driver.instance.contextID
        bindTarget()
        val effectiveWidth = (width * contentScale).toInt()
        if (!(arrayCubemap.width == effectiveWidth && arrayCubemap.width == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${arrayCubemap.width} x ${arrayCubemap.width}")
        }
        arrayCubemap as ArrayCubemapGL4
        glFramebufferTextureLayer(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0 + colorAttachments.size,
            arrayCubemap.texture,
            level,
            layer * 6 + side.ordinal
        )
        checkGLErrors { null }

        colorAttachments.add(ArrayCubemapAttachment(colorAttachments.size, name, arrayCubemap, side, layer, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attach(cubemap: Cubemap, side: CubemapSide, level: Int, name: String?) {
        val div = 1 shl level
        require(!destroyed)
        val context = Driver.instance.contextID
        bindTarget()
        val effectiveWidth = (width * contentScale).toInt()
        if (!(cubemap.width / div == effectiveWidth && cubemap.width / div == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${cubemap.width} x ${cubemap.width})")
        }
        cubemap as CubemapGL3

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0 + colorAttachments.size,
            side.glTextureTarget,
            cubemap.texture,
            level
        )

        checkGLErrors { null }

        colorAttachments.add(CubemapAttachment(colorAttachments.size, name, cubemap, side, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attach(volumeTexture: VolumeTexture, layer: Int, level: Int, name: String?) {
        require(!destroyed)
        require(level >= 0 && level < volumeTexture.depth)

        val context = Driver.instance.contextID
        bindTarget()

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(volumeTexture.width == effectiveWidth && volumeTexture.height == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${volumeTexture.width} x ${volumeTexture.height}")
        }
        volumeTexture as VolumeTextureGL3
        glFramebufferTextureLayer(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0 + colorAttachments.size,
            volumeTexture.texture,
            level,
            layer
        )
        debugGLErrors { null }

        colorAttachments.add(VolumeTextureAttachment(colorAttachments.size, name, volumeTexture, layer, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attachLayered(arrayTexture: ArrayTexture, level: Int, name: String?) {
        require(!destroyed)
        val context = Driver.instance.contextID
        bindTarget()

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(arrayTexture.width == effectiveWidth && arrayTexture.height == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${arrayTexture.width} x ${arrayTexture.height}")
        }
        arrayTexture as ArrayTextureGL3
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorAttachments.size, arrayTexture.texture, level)
        debugGLErrors { null }

        colorAttachments.add(LayeredArrayTextureAttachment(colorAttachments.size, name, arrayTexture, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attachLayered(arrayCubemap: ArrayCubemap, level: Int, name: String?) {
        require(!destroyed)
        val context = Driver.instance.contextID
        bindTarget()
        val effectiveWidth = (width * contentScale).toInt()
        if (!(arrayCubemap.width == effectiveWidth && arrayCubemap.width == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${arrayCubemap.width} x ${arrayCubemap.width}")
        }
        arrayCubemap as ArrayCubemapGL4
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorAttachments.size, arrayCubemap.texture, level)
        checkGLErrors { null }

        colorAttachments.add(LayeredArrayCubemapAttachment(colorAttachments.size, name, arrayCubemap, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attachLayered(cubemap: Cubemap, level: Int, name: String?) {
        val div = 1 shl level
        require(!destroyed)
        val context = Driver.instance.contextID
        bindTarget()
        val effectiveWidth = (width * contentScale).toInt()
        if (!(cubemap.width / div == effectiveWidth && cubemap.width / div == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${cubemap.width} x ${cubemap.width})")
        }
        cubemap as CubemapGL3

        glFramebufferTexture(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0 + colorAttachments.size,
            cubemap.texture,
            level
        )

        checkGLErrors { null }

        colorAttachments.add(LayeredCubemapAttachment(colorAttachments.size, name, cubemap, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attachLayered(volumeTexture: VolumeTexture, level: Int, name: String?) {
        require(!destroyed)
        require(level >= 0 && level < volumeTexture.depth)

        val context = Driver.instance.contextID
        bindTarget()

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(volumeTexture.width == effectiveWidth && volumeTexture.height == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${volumeTexture.width} x ${volumeTexture.height}")
        }
        volumeTexture as VolumeTextureGL3
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorAttachments.size, volumeTexture.texture, level)
        debugGLErrors { null }

        colorAttachments.add(LayeredVolumeTextureAttachment(colorAttachments.size, name, volumeTexture, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int, name: String?) {
        require(!destroyed)
        val context = Driver.instance.contextID
        bindTarget()

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(arrayTexture.width == effectiveWidth && arrayTexture.height == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${arrayTexture.width} x ${arrayTexture.height}")
        }
        arrayTexture as ArrayTextureGL3
        glFramebufferTextureLayer(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0 + colorAttachments.size,
            arrayTexture.texture,
            level,
            layer
        )
        debugGLErrors { null }

        colorAttachments.add(ArrayTextureAttachment(colorAttachments.size, name, arrayTexture, layer, level))

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun blendMode(index: Int, blendMode: BlendMode) {
        when (blendMode) {
            BlendMode.OVER -> {
                glEnable(GL_BLEND)
                glBlendEquationi(index, GL_FUNC_ADD)
                glBlendFunci(index, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
            }

            BlendMode.BLEND -> {
                glEnable(GL_BLEND)
                glBlendEquationi(index, GL_FUNC_ADD)
                glBlendFunci(index, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            }

            BlendMode.REPLACE -> {
                glEnable(GL_BLEND)
                glBlendEquationi(index, GL_FUNC_ADD)
                glBlendFunci(index, GL_ONE, GL_ZERO)
            }

            BlendMode.ADD -> {
                glEnable(GL_BLEND)
                glBlendEquationi(index, GL_FUNC_ADD)
                glBlendFunci(index, GL_ONE, GL_ONE)
            }

            BlendMode.MIN -> {
                glEnable(GL_BLEND)
                glBlendEquationi(index, GL_MIN)
                glBlendFunci(index, GL_ONE, GL_ONE)
            }

            BlendMode.MAX -> {
                glEnable(GL_BLEND)
                glBlendEquationi(index, GL_MAX)
                glBlendFunci(index, GL_ONE, GL_ONE)
            }

            else -> {
                error("unsupported blend mode: $blendMode")
            }
        }
    }

    private fun bound(function: () -> Unit) {
        bind()
        function()
        unbind()
    }

    override fun clearColor(index: Int, color: ColorRGBa) {
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
            glClearBufferfv(GL_COLOR, index, ca)
        }
    }

    override fun clearDepth(depth: Double, stencil: Int) {
        require(!destroyed)
        bound {
            val mask = glGetBoolean(GL_DEPTH_WRITEMASK)
            if (!mask) {
                glDepthMask(true)
            }
            if (hasDepthBuffer && hasStencilBuffer) {
                glClearBufferfi(GL_DEPTH_STENCIL, 0, depth.toFloat(), stencil)
            } else if (hasDepthBuffer) {
                MemoryStack.stackPush().use { stack ->
                    val fb = stack.mallocFloat(1)
                    fb.put(depth.toFloat())
                    fb.flip()
                    glClearBufferfv(GL_DEPTH, 0, fb)
                }
            } else if (hasStencilBuffer) {
                MemoryStack.stackPush().use { stack ->
                    val ib = stack.mallocInt(1)
                    ib.put(stencil)
                    ib.flip()
                    glClearBufferiv(GL_STENCIL, 0, ib)
                }
            }
            if (!mask) {
                glDepthMask(false)
            }
            checkGLErrors()
        }
    }

    override fun attach(depthBuffer: DepthBuffer, ownedByRenderTarget: Boolean) {
        require(!destroyed)
        require(this.depthBuffer == null) { "a depth buffer is already attached" }

        ownDepthBuffer = ownedByRenderTarget
        if (!(depthBuffer.width == effectiveWidth && depthBuffer.height == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch")
        }

        if (depthBuffer.multisample != multisample) {
            throw IllegalArgumentException("buffer multisample mismatch")
        }
        bound {
            depthBuffer as DepthBufferGL3

            if (depthBuffer.texture != -1) {
                if (depthBuffer.hasDepth) {
                    glFramebufferTexture2D(
                        GL_FRAMEBUFFER,
                        GL_DEPTH_ATTACHMENT,
                        depthBuffer.target,
                        depthBuffer.texture,
                        0
                    )
                    checkGLErrors { null }
                }

                if (depthBuffer.hasStencil) {
                    glFramebufferTexture2D(
                        GL_FRAMEBUFFER,
                        GL_STENCIL_ATTACHMENT,
                        depthBuffer.target,
                        depthBuffer.texture,
                        0
                    )
                    checkGLErrors { null }
                }
            } else {
                when (Pair(depthBuffer.hasDepth, depthBuffer.hasStencil)) {
                    Pair(true, true) -> glFramebufferRenderbuffer(
                        GL_FRAMEBUFFER,
                        GL_DEPTH_STENCIL_ATTACHMENT,
                        GL_RENDERBUFFER,
                        depthBuffer.buffer
                    )

                    Pair(false, true) -> glFramebufferRenderbuffer(
                        GL_FRAMEBUFFER,
                        GL_STENCIL_ATTACHMENT,
                        GL_RENDERBUFFER,
                        depthBuffer.buffer
                    )

                    Pair(true, false) -> glFramebufferRenderbuffer(
                        GL_FRAMEBUFFER,
                        GL_DEPTH_ATTACHMENT,
                        GL_RENDERBUFFER,
                        depthBuffer.buffer
                    )

                    else -> error("DepthBuffer should have at least depth or stencil components")
                }
                checkGLErrors()
            }
            this.depthBuffer = depthBuffer
            checkFramebufferStatus()
        }
    }

    override fun detachDepthBuffer() {
        require(!destroyed)
        this.depthBuffer?.let {
            it as DepthBufferGL3
            bound {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, it.target, 0, 0)
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, it.target, 0, 0)
                checkGLErrors()
            }
        }
    }

    private fun checkFramebufferStatus() {
        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            when (status) {
                GL_FRAMEBUFFER_UNDEFINED -> throw GL3Exception("Framebuffer undefined")
                GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> throw GL3Exception("Attachment incomplete")
                GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> throw GL3Exception("Attachment missing")
                GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> throw GL3Exception("Incomplete draw buffer")
                GL_FRAMEBUFFER_UNSUPPORTED -> throw GL3Exception("the combination of internal formats of the attached images violates an implementation-dependent set of restrictions")
                GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> throw GL3Exception(" the value of GL_RENDERBUFFER_SAMPLES is not the same for all attached renderbuffers; if the value of GL_TEXTURE_SAMPLES is the not same for all attached textures; or, if the attached images are a mix of renderbuffers and textures, the value of GL_RENDERBUFFER_SAMPLES does not match the value of GL_TEXTURE_SAMPLES.")
            }
            throw GL3Exception("error creating framebuffer $status")
        }
        checkGLErrors()
    }

    override fun detachColorAttachments() {
        require(!destroyed)
        bound {
            for ((index, _) in colorAttachments.withIndex()) {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, GL_TEXTURE_2D, 0, 0)
            }
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
        for (colorAttachment in colorAttachments) {
            if (colorAttachment.ownedByRenderTarget) {
                when (colorAttachment) {
                    is ColorBufferAttachment -> colorAttachment.colorBuffer.destroy()
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
        glDeleteFramebuffers(framebuffer)
    }

    override fun toString(): String {
        return "RenderTargetGL3(framebuffer=$framebuffer, width=$width, height=$height, contentScale=$contentScale, multisample=$multisample, session=$session, destroyed=$destroyed, colorAttachments=$colorAttachments, depthBuffer=$depthBuffer)"
    }
}
package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.opengl.GL40C.glBlendEquationi
import org.lwjgl.opengl.GL40C.glBlendFunci
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import java.util.*

private val logger = KotlinLogging.logger {}

private val active = mutableMapOf<Long, Stack<RenderTargetGL3>>()

class NullRenderTargetGL3 : RenderTargetGL3(0, 640, 480, 1.0, BufferMultisample.Disabled, Session.root)

class ProgramRenderTargetGL3(override val program: Program) : ProgramRenderTarget, RenderTargetGL3(glGetInteger(GL_FRAMEBUFFER_BINDING), 0, 0, 1.0, BufferMultisample.Disabled, Session.root) {
    override val width: Int
        get() = program.window.size.x.toInt()

    override val height: Int
        get() = program.window.size.y.toInt()

    override val contentScale: Double
        get() = program.window.scale.x

    override val hasColorBuffer = true
    override val hasDepthBuffer = true

    override fun colorBufferIndex(name: String): Int {
        return 0
    }
}

open class RenderTargetGL3(val framebuffer: Int,
                           override val width: Int,
                           override val height: Int,
                           override val contentScale: Double,
                           override val multisample: BufferMultisample,
                           override val session: Session?,
                           private val thread: Thread = Thread.currentThread()


) : RenderTarget {
    var destroyed = false

    override val colorBuffers: List<ColorBuffer>
        get() = _colorBuffers.map { it }

    override val depthBuffer: DepthBuffer?
        get() = _depthBuffer


    private var attachements = 0

    private val colorBufferIndices = mutableMapOf<String, Int>()
    private val arrayTextureIndices = mutableMapOf<String, Int>()
    private val _arrayTextures = mutableListOf<ArrayTextureGL3>()
    private val _colorBuffers = mutableListOf<ColorBufferGL3>()
    private var _depthBuffer: DepthBuffer? = null


    companion object {
        fun create(width: Int, height: Int, contentScale: Double = 1.0, multisample: BufferMultisample = BufferMultisample.Disabled, session: Session?): RenderTargetGL3 {
            logger.trace { "created new render target ($width*$height) @ ${contentScale}x $multisample" }
            val framebuffer = glGenFramebuffers()
            return RenderTargetGL3(framebuffer, width, height, contentScale, multisample, session)
        }

        val activeRenderTarget: RenderTargetGL3
            get() {
                val stack = active.getOrPut(glfwGetCurrentContext()) { Stack() }
                return stack.peek()
            }
    }

    private var bound = false

    override val hasColorBuffer: Boolean get() = colorBuffers.isNotEmpty()
    override val hasDepthBuffer: Boolean get() = depthBuffer != null


    override fun colorBuffer(index: Int): ColorBuffer {
        require(!destroyed)
        return _colorBuffers[index]
    }

    override fun colorBuffer(name: String): ColorBuffer {
        require(!destroyed)
        return _colorBuffers[colorBufferIndices[name]!!]
    }

    override fun colorBufferIndex(name: String): Int {
        return colorBufferIndices[name]!!
    }

    override fun bind() {
        require(!destroyed)
        if (bound) {
            throw RuntimeException("already bound")
        } else {
            val stack = active.getOrPut(glfwGetCurrentContext()) { Stack() }
            stack.push(this)
            bindTarget()
        }
    }

    private fun bindTarget() {
        debugGLErrors { null }
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

        if (Thread.currentThread() != thread) {
            throw IllegalStateException("this render target is created by $thread and cannot be bound to ${Thread.currentThread()}")
        }

        debugGLErrors { null }
        if (_colorBuffers.size > 0) {
            val drawBuffers = _colorBuffers.mapIndexed { index, _ -> GL_COLOR_ATTACHMENT0 + index }.toIntArray()
            glDrawBuffers(drawBuffers)
            debugGLErrors {
                when (it) {
                    GL_INVALID_ENUM -> "1. one of the values in bufs is not an accepted value\n2. the API call refers to the default framebuffer and one or more of the values in bufs is one of the GL_COLOR_ATTACHMENTn tokens\n3. the API call refers to a framebuffer object and one or more of the values in bufs is anything other than GL_NONE or one of the GL_COLOR_ATTACHMENTn tokens\n4. n is less than 0"
                    GL_INVALID_OPERATION -> "a symbolic constant other than GL_NONE appears more than once in bufs."
                    GL_INVALID_VALUE -> "1. n is greater than GL_MAX_DRAW_BUFFERS\n 2. any of the entries in bufs (other than GL_NONE ) indicates a color buffer that does not exist in the current GL context\n 3. any value in bufs is GL_BACK, and n is not one"
                    else -> null
                }
            }
        } else {
//            if (this !is ProgramRenderTargetGL3) {
//                throw RuntimeException("render target has no attached color buffers")
//            }

        }
        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        logger.trace { "setting viewport to (0, 0, $effectiveWidth, $effectiveHeight)" }
        glViewport(0, 0, effectiveWidth, effectiveHeight)
        debugGLErrors { null }
    }

    override fun unbind() {
        if (!bound) {
            val previous = active.getOrPut(glfwGetCurrentContext()) { Stack() }.let {
                it.pop()
                it.peek()
            }
            previous as RenderTargetGL3
            logger.trace { "restoring to previous render target $previous" }
            previous.bindTarget()
        } else {
            throw RuntimeException("target not bound")
        }
    }

    override fun attach(name: String, colorBuffer: ColorBuffer, level: Int) {
        require(!destroyed)
        colorBufferIndices[name] = _colorBuffers.size
        attach(colorBuffer, level)
    }

    override fun attach(colorBuffer: ColorBuffer, level: Int) {
        require(!destroyed)
        val context = glfwGetCurrentContext()
        bindTarget()

        val div = 1 shl level
        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(colorBuffer.effectiveWidth / div == effectiveWidth && colorBuffer.effectiveHeight / div == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($width x $height @${colorBuffer.contentScale}x, got: (${colorBuffer.width/div} x ${colorBuffer.height/div} @${colorBuffer.contentScale}x level:${level})")
        }
        colorBuffer as ColorBufferGL3
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + attachements, colorBuffer.target, colorBuffer.texture, level)
        attachements++
        debugGLErrors { null }
        _colorBuffers.add(colorBuffer)

        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }

    override fun attach(name: String, arrayTexture: ArrayTexture, layer: Int, level: Int) {
        require(!destroyed)
        arrayTextureIndices[name] = _arrayTextures.size
        attach(arrayTexture, layer, level)
    }

    override fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int) {
        require(!destroyed)
        val context = glfwGetCurrentContext()
        bindTarget()

        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(arrayTexture.width == effectiveWidth && arrayTexture.height == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: ($effectiveWidth x $effectiveHeight), got: (${arrayTexture.width} x ${arrayTexture.height}")
        }
        arrayTexture as ArrayTextureGL3
        //glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorBuffers.size, colorBuffer.target, colorBuffer.texture, 0)
        glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + attachements, arrayTexture.texture, level, layer)
        debugGLErrors { null }
        attachements++

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
            BlendMode.REPLACE -> {
                glEnable(GL_BLEND)
                glBlendEquationi(index, GL_FUNC_ADD)
                glBlendFunci(index, GL_ONE, GL_ZERO)
            }
        }
    }

    private fun bound(function: () -> Unit) {
        bind()
        function()
        unbind()
    }

    override fun clearColor(index: Int, color: ColorRGBa) {
        require(!destroyed)
        bound {
            val ca = floatArrayOf(color.r.toFloat(), color.g.toFloat(), color.b.toFloat(), color.a.toFloat())
            glClearBufferfv(GL_COLOR, index, ca)
        }
    }

    override fun clearDepth(depth: Double, stencil: Int) {
        require(!destroyed)
        bound {
            glClearBufferfi(GL_DEPTH_STENCIL, 0, depth.toFloat(), stencil)
            checkGLErrors()
        }
    }

    override fun attach(depthBuffer: DepthBuffer) {
        require(!destroyed)
        if (!(depthBuffer.width == effectiveWidth && depthBuffer.height == effectiveHeight)) {
            throw IllegalArgumentException("buffer dimension mismatch")
        }

        if (depthBuffer.multisample != multisample) {
            throw IllegalArgumentException("buffer multisample mismatch")
        }
        bound {

            depthBuffer as DepthBufferGL3

            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthBuffer.target, depthBuffer.texture, 0)
            debugGLErrors { null }

            if (depthBuffer.hasStencil) {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, depthBuffer.target, depthBuffer.texture, 0)
                debugGLErrors { null }
            }
            checkGLErrors()

            this._depthBuffer = depthBuffer

            checkFramebufferStatus()
        }
    }

    override fun detachDepthBuffer() {
        require(!destroyed)
        this._depthBuffer?.let {
            it as DepthBufferGL3
            bound {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, it.target, 0, 0)
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, it.target, 0, 0)
                checkGLErrors()
            }
        }
    }

    internal fun checkFramebufferStatus() {
        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            when (status) {
                GL_FRAMEBUFFER_UNDEFINED -> throw GL3Exception("Framebuffer undefined")
                GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> throw GL3Exception("Attachment incomplete")
                GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> throw GL3Exception("Attachment missing")
                GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> throw GL3Exception("Incomplete draw buffer")
            }
            throw GL3Exception("error creating framebuffer $status")
        }
        checkGLErrors()
    }

    override fun detachColorBuffers() {
        require(!destroyed)
        bound {
            _colorBuffers.forEachIndexed { index, it ->
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, it.target, 0, 0)
            }
        }
        _colorBuffers.clear()
    }

    override fun destroy() {
        session?.untrack(this)
        destroyed = true
        glDeleteFramebuffers(framebuffer)
    }
}


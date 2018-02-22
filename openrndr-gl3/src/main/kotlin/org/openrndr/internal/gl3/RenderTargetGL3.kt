package org.openrndr.internal.gl3

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.ProgramRenderTarget
import org.openrndr.draw.RenderTarget
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.opengl.ARBFramebufferObject.glGenFramebuffers
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glDrawBuffers
import org.lwjgl.opengl.GL30.*
import org.openrndr.Program
import java.util.*


private val active = mutableMapOf<Long, Stack<RenderTargetGL3>>()




class ProgramRenderTargetGL3(override val program: Program) : ProgramRenderTarget, RenderTargetGL3(glGetInteger(GL_FRAMEBUFFER_BINDING), 0, 0) {
    override val width: Int
        get() = program.window.size.x.toInt()

    override val height: Int
        get() = program.window.size.y.toInt()



}

open class RenderTargetGL3(val framebuffer: Int, override val width: Int, override val height: Int) : RenderTarget {


    override val colorBuffers: List<ColorBuffer>
        get() = _colorBuffers.map { it }

    private val colorBufferIndices = mutableMapOf<String, Int>()
    val _colorBuffers = mutableListOf<ColorBufferGL3>()
    var depthBuffer: DepthBuffer? = null

    companion object {
        fun create(width: Int, height: Int): RenderTargetGL3 {
            val framebuffer = glGenFramebuffers()
            return RenderTargetGL3(framebuffer, width, height)
        }
    }

    var bound = false


    override fun colorBuffer(index: Int): ColorBuffer {
        return _colorBuffers[index]
    }

    override fun colorBuffer(name: String):ColorBuffer {
       return _colorBuffers[colorBufferIndices[name]!!]
    }

    override fun colorBufferIndex(name:String):Int {
        return colorBufferIndices[name]!!
    }

    override fun bind() {

        glfwGetCurrentContext()

        if (bound) {
            throw RuntimeException("already bound")
        } else {
            val stack = active.getOrPut(glfwGetCurrentContext()) { Stack() }
            stack.push(this)
            bindTarget()
        }
    }



    internal fun bindTarget() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

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
        glViewport(0, 0, width, height)
        debugGLErrors { null }
    }

    override fun unbind() {
        if (!bound) {
            val previous = active.getOrPut(glfwGetCurrentContext()) { Stack() }.let {
                it.pop()
                it.peek()
            }
            previous as RenderTargetGL3
            previous.bindTarget()

        } else {
            throw RuntimeException("target not bound")
        }
    }

    override fun attach(name: String, colorBuffer: ColorBuffer) {
        colorBufferIndices[name] = _colorBuffers.size
        attach(colorBuffer)
    }

    override fun attach(colorBuffer: ColorBuffer) {
        val context = glfwGetCurrentContext()
        bindTarget()

        if (!(colorBuffer.width == width && colorBuffer.height == height)) {
            throw IllegalArgumentException("buffer dimension mismatch. expected: (" + width + " x " + height + "), got: (" + colorBuffer.width + " x " + colorBuffer.height + ")")
        }
        colorBuffer as ColorBufferGL3
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + colorBuffers.size, colorBuffer.target, colorBuffer.texture, 0)
        debugGLErrors { null }
        _colorBuffers.add(colorBuffer)


        if (active[context]?.peek() != null)
            (active[context]?.peek() as RenderTargetGL3).bindTarget()
    }


    internal fun bound(function: () -> Unit) {
        bind()
        function()
        unbind()
    }

    override fun attach(depthBuffer: DepthBuffer) {

        bound {

            if (!(depthBuffer.width == width && depthBuffer.height == height)) {
                throw IllegalArgumentException("buffer dimension mismatch")
            }

            depthBuffer as DepthBufferGL3
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthBuffer.texture, 0)
            debugGLErrors { null }

            if (depthBuffer.hasStencil) {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthBuffer.texture, 0)
                debugGLErrors { null }
            }
            checkGLErrors()

            this.depthBuffer = depthBuffer
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

            throw GL3Exception("error creating framebuffer" + status)
        }
        checkGLErrors()
    }


    override fun detachColorBuffers() {
        bound {
            _colorBuffers.forEachIndexed { index, _ ->
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, GL_TEXTURE_2D, 0, 0)
            }
        }
        _colorBuffers.clear()
    }

    override fun destroy() {
        glDeleteFramebuffers(framebuffer)
    }

}


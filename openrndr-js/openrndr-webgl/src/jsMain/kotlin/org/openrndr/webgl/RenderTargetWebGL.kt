package org.openrndr.webgl

import org.khronos.webgl.WebGLFramebuffer
import org.openrndr.Program
import org.openrndr.collections.pop
import org.openrndr.collections.push
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.khronos.webgl.WebGLRenderingContext as GL

private val active = ArrayDeque<RenderTargetWebGL>()

class ProgramRenderTargetWebGL(context: GL, override val program: Program) : ProgramRenderTarget,
    RenderTargetWebGL(context, null, 0, 0, 1.0, BufferMultisample.Disabled, Session.root) {
    override val width: Int
        get() = program.window.size.x.toInt()

    override val height: Int
        get() = program.window.size.y.toInt()

    override val contentScale: Double
        get() = program.window.contentScale

    override val hasColorAttachments = true
    override val hasDepthBuffer = true
}

open class RenderTargetWebGL(
    val context: GL,
    val framebuffer: WebGLFramebuffer?,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val multisample: BufferMultisample,
    override val session: Session?

) : RenderTarget {
    companion object {
        fun create(
            context: GL,
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            multisample: BufferMultisample = BufferMultisample.Disabled,
            session: Session?
        ): RenderTargetWebGL {
            val framebuffer = context.createFramebuffer() ?: error("framebuffer creation failed")
            return RenderTargetWebGL(context, framebuffer, width, height, contentScale, multisample, session)
        }

        val activeRenderTarget: RenderTargetWebGL
            get() {
                return active.last()
            }
    }

    override val colorAttachments: MutableList<ColorAttachment> = mutableListOf()
    override var depthBuffer: DepthBuffer? = null

    fun bindTarget() {
        context.bindFramebuffer(GL.FRAMEBUFFER, framebuffer)
        context.viewport(0, 0, effectiveWidth, effectiveHeight)
    }

    override fun attach(colorBuffer: ColorBuffer, level: Int, name: String?) {
        bindTarget()
        val caps = (Driver.instance as DriverWebGL).capabilities
        if (colorBuffer.type == ColorType.FLOAT16) {
            require(caps.colorBufferHalfFloat)
        }
        if (colorBuffer.type == ColorType.FLOAT32) {
            require(caps.colorBufferFloat)
        }
        colorBuffer as ColorBufferWebGL
        val div = 1 shl level
        val effectiveWidth = (width * contentScale).toInt()
        val effectiveHeight = (height * contentScale).toInt()

        if (!(colorBuffer.effectiveWidth / div == effectiveWidth && colorBuffer.effectiveHeight / div == effectiveHeight)) {
            error("buffer dimension mismatch. expected: ($width x $height @${colorBuffer.contentScale}x, got: (${colorBuffer.width / div} x ${colorBuffer.height / div} @${colorBuffer.contentScale}x level:${level})")
        }
        context.framebufferTexture2D(
            GL.FRAMEBUFFER,
            GL.COLOR_ATTACHMENT0 + colorAttachments.size,
            colorBuffer.target,
            colorBuffer.texture,
            level
        )
        colorAttachments.add(ColorBufferAttachment(colorAttachments.size, name, colorBuffer, level))

    }

    override fun attach(depthBuffer: DepthBuffer) {
        depthBuffer as DepthBufferWebGL

        val webGlAttachment =
            when (depthBuffer.format) {
                DepthFormat.DEPTH_STENCIL -> GL.DEPTH_STENCIL_ATTACHMENT
                DepthFormat.STENCIL8 -> GL.STENCIL_ATTACHMENT
                DepthFormat.DEPTH16 -> GL.DEPTH_ATTACHMENT
                else -> error("unsupported depth buffer format '${depthBuffer.format}'")
            }

        context.framebufferRenderbuffer(GL.FRAMEBUFFER, webGlAttachment, GL.RENDERBUFFER, depthBuffer.buffer)
        this.depthBuffer = depthBuffer
    }

    override fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int, name: String?) {
    }

    override fun attach(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int, name: String?) {
    }

    override fun attach(cubemap: Cubemap, side: CubemapSide, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attach(volumeTexture: VolumeTexture, layer: Int, level: Int, name: String?) {
    }

    override fun attachLayered(arrayTexture: ArrayTexture, level: Int, name: String?) {
    }

    override fun attachLayered(arrayCubemap: ArrayCubemap, level: Int, name: String?) {
    }

    override fun attachLayered(cubemap: Cubemap, level: Int, name: String?) {

    }

    override fun attachLayered(volumeTexture: VolumeTexture, level: Int, name: String?) {
    }

    override fun detachColorAttachments() {
        bound {
            for ((index, _) in colorAttachments.withIndex()) {
                context.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0 + index, GL.TEXTURE_2D, null, 0)
            }
        }
    }

    override fun detachColorBuffers() {
        TODO("Not yet implemented")
    }

    override fun detachDepthBuffer() {
        bound {
            context.framebufferRenderbuffer(GL.FRAMEBUFFER, GL.DEPTH_ATTACHMENT, GL.RENDERBUFFER, null)
            depthBuffer = null
        }
    }

    override fun destroy() {
        context.deleteFramebuffer(framebuffer)
    }

    override fun colorBuffer(index: Int): ColorBuffer {
        return colorAttachments.filterIsInstance<ColorBufferAttachment>()[index].colorBuffer
    }

    override fun clearColor(index: Int, color: ColorRGBa) {
        TODO("Not yet implemented")
    }

    override fun clearDepth(depth: Double, stencil: Int) {
        TODO("Not yet implemented")
    }

    override fun blendMode(index: Int, blendMode: BlendMode) {
        error("not supported")
    }

    private fun bound(function: () -> Unit) {
        bind()
        function()
        unbind()
    }

    var bound = false
    override fun bind() {
        if (bound) {
            throw RuntimeException("already bound")
        } else {
            active.push(this)
            bindTarget()
        }
    }

    override fun unbind() {
        if (!bound) {
            active.pop()
            val previous = active.last()
            previous.bindTarget()
        } else {
            throw RuntimeException("target not bound")
        }
    }

    override val hasDepthBuffer: Boolean
        get() = depthBuffer != null
    override val hasColorAttachments: Boolean
        get() = colorAttachments.isNotEmpty()

}
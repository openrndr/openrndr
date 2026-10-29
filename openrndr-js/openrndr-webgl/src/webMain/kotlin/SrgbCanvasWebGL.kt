@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.webgl

import web.gl.*
import web.gl.WebGL2RenderingContext as GL

/**
 * Whether the browser can give the canvas an sRGB drawing buffer, with WebGL2RenderingContext.drawingBufferStorage
 */
internal fun canvasSupportsSrgbDrawingBuffer(): Boolean = js(
    "typeof WebGL2RenderingContext !== 'undefined' && typeof WebGL2RenderingContext.prototype.drawingBufferStorage === 'function'"
)

/**
 * Makes the drawing buffer of the canvas sRGB, it keeps its format when the canvas is resized
 * @return true when the drawing buffer is sRGB
 */
internal fun useSrgbDrawingBuffer(context: GL, width: Int, height: Int): Boolean = js(
    """(() => {
        context.drawingBufferStorage(context.SRGB8_ALPHA8, width, height);
        return context.getError() === 0 && context.drawingBufferFormat === context.SRGB8_ALPHA8;
    })()"""
)

private fun renderbufferSamples(context: GL, renderbuffer: WebGLRenderbuffer): Int = js(
    "(() => { context.bindRenderbuffer(context.RENDERBUFFER, renderbuffer); const s = context.getRenderbufferParameter(context.RENDERBUFFER, context.RENDERBUFFER_SAMPLES); context.bindRenderbuffer(context.RENDERBUFFER, null); return s; })()"
)

/**
 * Draws the window into an offscreen, multisampled sRGB target and copies it to the canvas, encoding the colors to
 * sRGB. This is used in browsers in which the drawing buffer of the canvas can not be sRGB: blending happens on linear
 * colors and the canvas holds sRGB colors, like the window on the JVM. Blitting the sRGB target to the canvas would
 * decode the colors to linear.
 */
internal class SrgbCanvasFallbackWebGL(private val context: GL) {
    val framebuffer: WebGLFramebuffer = context.createFramebuffer() ?: error("failed to create framebuffer")
    private val color = context.createRenderbuffer() ?: error("failed to create renderbuffer")
    private val depthStencil = context.createRenderbuffer() ?: error("failed to create renderbuffer")
    private val resolveFramebuffer = context.createFramebuffer() ?: error("failed to create framebuffer")
    private var resolved: WebGLTexture? = null
    private val vao = context.createVertexArray() ?: error("failed to create vertex array")
    private var width = 0
    private var height = 0

    private val program: WebGLProgram = run {
        val vertexShader = VertexShaderWebGL.fromString(
            context, """
            |#version 300 es
            |void main() {
            |    // a triangle that covers the viewport
            |    vec2 position = vec2(float((gl_VertexID & 1) << 2) - 1.0, float((gl_VertexID & 2) << 1) - 1.0);
            |    gl_Position = vec4(position, 0.0, 1.0);
            |}
            """.trimMargin(), "srgb-canvas"
        )
        val fragmentShader = FragmentShaderWebGL.fromString(
            context, """
            |#version 300 es
            |precision highp float;
            |uniform sampler2D image;
            |out vec4 o_color;
            |void main() {
            |    // the sRGB texture is decoded to linear when it is read
            |    vec4 c = texelFetch(image, ivec2(gl_FragCoord.xy), 0);
            |    vec3 encoded = mix(c.rgb * 12.92, 1.055 * pow(c.rgb, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, c.rgb));
            |    o_color = vec4(encoded, c.a);
            |}
            """.trimMargin(), "srgb-canvas"
        )
        val program = context.createProgram() ?: error("failed to create program")
        context.attachShader(program, vertexShader.shaderObject)
        context.attachShader(program, fragmentShader.shaderObject)
        context.linkProgram(program)
        require(context.getProgramParameter(program, LINK_STATUS)?.unsafeCast<JsBoolean>()?.toBoolean() == true) {
            "failed to link the sRGB canvas program: ${context.getProgramInfoLog(program)}"
        }
        context.checkErrors("SrgbCanvasFallbackWebGL program")
        program
    }

    /**
     * Sizes the offscreen target to the drawing buffer of the canvas, the contents are lost when the size changes
     */
    fun resize(width: Int, height: Int) {
        val w = maxOf(1, width)
        val h = maxOf(1, height)
        if (w == this.width && h == this.height) {
            return
        }
        this.width = w
        this.height = h

        context.bindRenderbuffer(RENDERBUFFER, color)
        context.renderbufferStorageMultisample(RENDERBUFFER, SAMPLES, SRGB8_ALPHA8, w, h)
        context.bindRenderbuffer(RENDERBUFFER, depthStencil)
        context.renderbufferStorageMultisample(RENDERBUFFER, SAMPLES, DEPTH24_STENCIL8, w, h)
        context.bindRenderbuffer(RENDERBUFFER, null)
        context.bindFramebuffer(FRAMEBUFFER, framebuffer)
        context.framebufferRenderbuffer(FRAMEBUFFER, COLOR_ATTACHMENT0, RENDERBUFFER, color)
        context.framebufferRenderbuffer(FRAMEBUFFER, DEPTH_STENCIL_ATTACHMENT, RENDERBUFFER, depthStencil)
        val status = context.checkFramebufferStatus(FRAMEBUFFER)
        require(status == FRAMEBUFFER_COMPLETE) {
            "the sRGB canvas target of ${w}x$h is incomplete, status $status, ${renderbufferSamples(context, color)} color and ${renderbufferSamples(context, depthStencil)} depth samples, error ${context.getError()}"
        }

        // texture storage is immutable, a new size needs a new texture
        resolved?.let { context.deleteTexture(it) }
        val texture = context.createTexture() ?: error("failed to create texture")
        context.bindTexture(TEXTURE_2D, texture)
        context.texStorage2D(TEXTURE_2D, 1, SRGB8_ALPHA8, w, h)
        context.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, NEAREST)
        context.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, NEAREST)
        context.bindFramebuffer(FRAMEBUFFER, resolveFramebuffer)
        context.framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, TEXTURE_2D, texture, 0)
        require(context.checkFramebufferStatus(FRAMEBUFFER) == FRAMEBUFFER_COMPLETE) { "the sRGB canvas resolve target is incomplete" }
        resolved = texture

        context.bindFramebuffer(FRAMEBUFFER, framebuffer)
        context.clearBufferfv(COLOR, 0, float32Array(0.0f, 0.0f, 0.0f, 0.0f))
        context.checkErrors("SrgbCanvasFallbackWebGL.resize()")
    }

    /**
     * Copies the offscreen target to the canvas. The resolve copies sRGB to sRGB, which keeps the colors as they are.
     */
    fun present() {
        context.bindFramebuffer(READ_FRAMEBUFFER, framebuffer)
        context.bindFramebuffer(DRAW_FRAMEBUFFER, resolveFramebuffer)
        context.blitFramebuffer(0, 0, width, height, 0, 0, width, height, COLOR_BUFFER_BIT, NEAREST)
        context.checkErrors("SrgbCanvasFallbackWebGL.present(), resolve")

        context.bindFramebuffer(FRAMEBUFFER, null)
        context.viewport(0, 0, width, height)
        // the driver sets its state before drawing, the state is not restored
        context.disable(SCISSOR_TEST)
        context.disable(DEPTH_TEST)
        context.disable(STENCIL_TEST)
        context.disable(BLEND)
        context.disable(CULL_FACE)
        context.disable(SAMPLE_ALPHA_TO_COVERAGE)
        context.colorMask(true, true, true, true)
        context.useProgram(program)
        context.bindVertexArray(vao)
        context.activeTexture(TEXTURE0)
        context.bindTexture(TEXTURE_2D, resolved)
        context.drawArrays(TRIANGLES, 0, 3)
        context.bindVertexArray(null)
        context.useProgram(null)
        context.bindFramebuffer(FRAMEBUFFER, framebuffer)
        context.checkErrors("SrgbCanvasFallbackWebGL.present()")
    }

    private companion object {
        /** WebGL 2 supports at least 4 samples for renderbuffers */
        const val SAMPLES = 4
    }
}

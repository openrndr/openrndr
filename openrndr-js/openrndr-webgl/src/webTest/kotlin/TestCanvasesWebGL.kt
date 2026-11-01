@file:OptIn(ExperimentalWasmJsInterop::class)

import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.Session
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.renderTarget
import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import org.openrndr.webgl.ApplicationBaseWebGL
import org.openrndr.webgl.ApplicationWebGL
import org.openrndr.webgl.ColorBufferWebGL
import org.openrndr.webgl.DriverWebGL
import web.dom.ElementId
import web.dom.document
import web.gl.WebGL2RenderingContext
import web.gl.WebGLTexture
import web.html.HTMLCanvasElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** the RGBA bytes of the bottom left pixel of level 0 of [texture] */
private fun readFirstPixel(context: WebGL2RenderingContext, texture: WebGLTexture): String = js(
    """(() => {
        const framebuffer = context.createFramebuffer();
        const previous = context.getParameter(context.FRAMEBUFFER_BINDING);
        context.bindFramebuffer(context.FRAMEBUFFER, framebuffer);
        context.framebufferTexture2D(context.FRAMEBUFFER, context.COLOR_ATTACHMENT0, context.TEXTURE_2D, texture, 0);
        const pixel = new Uint8Array(4);
        context.readPixels(0, 0, 1, 1, context.RGBA, context.UNSIGNED_BYTE, pixel);
        context.bindFramebuffer(context.FRAMEBUFFER, previous);
        context.deleteFramebuffer(framebuffer);
        return pixel.join(',');
    })()"""
)

/**
 * Programs on a page draw in canvases with their own WebGL contexts, which do not share resources. The resources that
 * openrndr caches, such as the quad of filters and the filter of drawn copies, and the stack of bound render targets
 * are kept per context.
 */
class TestCanvasesWebGL {
    private val canvasIds = listOf("openrndr-canvas-a", "openrndr-canvas-b")

    private fun start(canvasId: String): Pair<ApplicationWebGL, Program> {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.id = ElementId(canvasId)
        canvas.setAttribute("style", "width: 64px; height: 64px")
        document.body.appendChild(canvas)
        // the tests do not use fonts, without a font driver the setup would suspend to load harfbuzzjs
        if (FontDriver.driver == null) {
            FontDriver.driver = object : FontDriver {
                override fun loadFace(fileOrUrl: String) = error("fonts are not loaded in these tests")
            }
        }
        val program = ProgramImplementation()
        val application = ApplicationBaseWebGL().build(program, Configuration().apply { this.canvasId = canvasId }) as ApplicationWebGL
        var error: Throwable? = null
        val setup: suspend () -> Unit = {
            application.setup()
            Driver.instance.enableErrorChecking()
        }
        setup.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext get() = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                result.onFailure { error = it }
            }
        })
        error?.let { throw it }
        return application to program
    }

    /** runs [block] with the context of [application] current, like its frames */
    private fun inContextOf(application: ApplicationWebGL, block: () -> Unit) {
        val driver = Driver.instance as DriverWebGL
        val stored = driver.context
        driver.context = application.context!!
        try {
            block()
        } finally {
            driver.context = stored
        }
    }

    @AfterTest
    fun teardown() {
        Session.root.end()
        Driver.driver = null
        canvasIds.forEach { document.getElementById(ElementId(it))?.remove() }
    }

    @Test
    fun everyCanvasHasItsOwnResources() {
        val applications = canvasIds.map { start(it).first }
        // the second canvas follows the first, and the first is used again after the second
        for (application in applications + applications.first()) {
            inContextOf(application) {
                val source = colorBuffer(8, 8, 1.0)
                source.fill(ColorRGBa.RED)
                val target = colorBuffer(16, 16, 1.0)
                target.fill(ColorRGBa.BLUE)
                // a scaled copy is drawn with a filter
                source.copyTo(target, 0, 0, IntRectangle(0, 0, 8, 8), IntRectangle(0, 0, 16, 16))
                assertEquals("255,0,0,255", readFirstPixel(application.context!!, (target as ColorBufferWebGL).texture))

                // unbinding a render target binds the previous render target of the same canvas
                val rt = renderTarget(4, 4, 1.0) { colorBuffer() }
                rt.bind()
                rt.unbind()
                assertSame<RenderTarget?>(application.defaultRenderTarget, RenderTarget.active)
                rt.destroy()
                source.destroy()
                target.destroy()
            }
        }
    }
}

@file:OptIn(ExperimentalWasmJsInterop::class)

import org.openrndr.Configuration
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.internal.Driver
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer
import org.openrndr.webgl.DriverWebGL
import org.openrndr.webgl.ProgramRenderTargetWebGL
import org.openrndr.webgl.canvasSupportsSrgbDrawingBuffer
import web.dom.ElementId
import web.dom.document
import web.gl.FRAMEBUFFER
import web.gl.WebGL2RenderingContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** the red component of the bottom left pixel of the bound framebuffer */
private fun readRed(context: WebGL2RenderingContext): Int = js(
    "(() => { const b = new Uint8Array(4); context.readPixels(0, 0, 1, 1, context.RGBA, context.UNSIGNED_BYTE, b); return b[0]; })()"
)

private fun hideDrawingBufferStorage(): Unit = js(
    "(() => { const p = WebGL2RenderingContext.prototype; p.hiddenDrawingBufferStorage = p.drawingBufferStorage; delete p.drawingBufferStorage; })()"
)

private fun restoreDrawingBufferStorage(): Unit = js(
    "(() => { const p = WebGL2RenderingContext.prototype; p.drawingBufferStorage = p.hiddenDrawingBufferStorage; delete p.hiddenDrawingBufferStorage; })()"
)

/**
 * A canvas keeps its context and the format of its drawing buffer, a fresh canvas is created by the fixture
 */
private fun removeCanvas() {
    document.getElementById(ElementId(Configuration().canvasId))?.remove()
}

/**
 * Colors are linear in shaders and the window holds sRGB colors, the values are the same as in TestColorEncodingGL3
 */
abstract class AbstractColorEncodingWebGL : AbstractApplicationTestFixture() {
    /** 0.5 as a linear color, encoded to sRGB it is 188 */
    private val linearHalf = ColorRGBa(0.5, 0.5, 0.5, 1.0)

    /** 128 as an sRGB color */
    private val srgbHalf = ColorRGBa.fromHex("#808080")

    private val context get() = (Driver.instance as DriverWebGL).context

    protected val window get() = RenderTarget.active as ProgramRenderTargetWebGL

    private fun measure(target: RenderTarget?): Map<String, Int> {
        val drawer = program.drawer
        val results = mutableMapOf<String, Int>()

        fun read(): Int {
            if (target == null) {
                // the offscreen target of the fallback is multisampled, it is read on the canvas
                window.srgbFallback?.present()
                context.bindFramebuffer(FRAMEBUFFER, null)
                return readRed(context).also { window.bindTarget() }
            }
            return readRed(context)
        }

        fun draw() {
            drawer.ortho(RenderTarget.active)
            // past the edges, which are anti-aliased
            val cover = Rectangle(-1.0, -1.0, RenderTarget.active.width + 2.0, RenderTarget.active.height + 2.0)
            for ((name, color) in listOf("linear" to linearHalf, "srgb" to srgbHalf)) {
                drawer.clear(color)
                results["clear $name"] = read()

                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = color
                drawer.stroke = null
                drawer.rectangle(cover)
                results["fill $name"] = read()

                drawer.clear(ColorRGBa.BLACK)
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = "x_fill = p_color;"
                    parameter("color", color)
                }
                drawer.rectangle(cover)
                drawer.shadeStyle = null
                results["uniform $name"] = read()

                if (target != null) {
                    target.clearColor(0, color)
                    results["clearColor $name"] = read()
                }
            }
            val image = colorBuffer(4, 4, 1.0, ColorFormat.RGBa, ColorType.UINT8_SRGB)
            val pixels = MPPBuffer.allocate(4 * 4 * 4)
            repeat(4 * 4 * 4) { pixels.put(128.toByte()) }
            pixels.rewind()
            image.write(pixels, ColorFormat.RGBa, ColorType.UINT8_SRGB, 0, 0, 4, 4, 0)
            drawer.clear(ColorRGBa.BLACK)
            drawer.image(image, 0.0, 0.0, RenderTarget.active.width.toDouble(), RenderTarget.active.height.toDouble())
            results["image"] = read()
            image.destroy()
        }
        if (target == null) {
            draw()
        } else {
            drawer.isolatedWithTarget(target) { draw() }
        }
        return results
    }

    @Test
    fun theWindowHoldsSrgbColors() {
        assertEquals(windowExpectations, measure(null))
    }

    @Test
    fun srgbRenderTargetsHoldSrgbColors() {
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.UINT8_SRGB) }
        assertEquals(srgbTargetExpectations, measure(target))
        target.destroy()
    }

    @Test
    fun linearRenderTargetsHoldLinearColors() {
        val target = renderTarget(4, 4, 1.0) { colorBuffer(type = ColorType.UINT8) }
        assertEquals(linearTargetExpectations, measure(target))
        target.destroy()
    }

    companion object {
        val windowExpectations = mapOf(
            "clear linear" to 188, "fill linear" to 188, "uniform linear" to 188,
            "clear srgb" to 128, "fill srgb" to 128, "uniform srgb" to 128,
            "image" to 128
        )
        val srgbTargetExpectations = windowExpectations + mapOf("clearColor linear" to 188, "clearColor srgb" to 128)
        val linearTargetExpectations = mapOf(
            "clear linear" to 128, "fill linear" to 128, "uniform linear" to 128, "clearColor linear" to 128,
            "clear srgb" to 55, "fill srgb" to 55, "uniform srgb" to 55, "clearColor srgb" to 55,
            "image" to 55
        )
    }
}

/**
 * The canvas has an sRGB drawing buffer in browsers that support WebGL2RenderingContext.drawingBufferStorage
 */
class TestColorEncodingWebGL : AbstractColorEncodingWebGL() {
    @Test
    fun theCanvasHasAnSrgbDrawingBufferWhenSupported() {
        assertEquals(canvasSupportsSrgbDrawingBuffer(), window.srgbFallback == null)
    }
}

/**
 * The window is drawn into an offscreen sRGB target, as in browsers without WebGL2RenderingContext.drawingBufferStorage
 */
class TestColorEncodingFallbackWebGL : AbstractColorEncodingWebGL() {
    @BeforeTest
    override fun setup() {
        removeCanvas()
        hideDrawingBufferStorage()
        super.setup()
    }

    @AfterTest
    override fun teardown() {
        super.teardown()
        restoreDrawingBufferStorage()
        removeCanvas()
    }

    @Test
    fun theWindowIsDrawnOffscreen() {
        assertNotNull(window.srgbFallback)
    }
}

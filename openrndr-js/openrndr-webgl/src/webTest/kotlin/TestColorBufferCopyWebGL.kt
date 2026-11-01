@file:OptIn(ExperimentalWasmJsInterop::class)

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import org.openrndr.webgl.ColorBufferWebGL
import org.openrndr.webgl.DriverWebGL
import web.gl.WebGL2RenderingContext
import web.gl.WebGLTexture
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/** reads level 0 of [texture] as RGBA, as floats or bytes */
private fun readTexture(context: WebGL2RenderingContext, texture: WebGLTexture, width: Int, height: Int, float: Boolean): JsAny = js(
    """(() => {
        const framebuffer = context.createFramebuffer();
        const previous = context.getParameter(context.FRAMEBUFFER_BINDING);
        context.bindFramebuffer(context.FRAMEBUFFER, framebuffer);
        context.framebufferTexture2D(context.FRAMEBUFFER, context.COLOR_ATTACHMENT0, context.TEXTURE_2D, texture, 0);
        const pixels = float ? new Float32Array(width * height * 4) : new Uint8Array(width * height * 4);
        context.readPixels(0, 0, width, height, context.RGBA, float ? context.FLOAT : context.UNSIGNED_BYTE, pixels);
        context.bindFramebuffer(context.FRAMEBUFFER, previous);
        context.deleteFramebuffer(framebuffer);
        return pixels;
    })()"""
)

private fun valueAt(array: JsAny, index: Int): Double = js("array[index]")

private fun decodeSrgb(x: Double) = if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)

/**
 * Verifies [ColorBuffer.copyTo] with the cases of TestColorBufferCopyGL3: source and target rectangles across formats,
 * types, multisampling and mipmap levels.
 *
 * Sources hold a gradient that encodes the source pixel coordinates, every target pixel is compared to the source
 * pixel it should be copied from (nearest sampling), pixels outside of the target rectangle must keep the
 * background color.
 */
class TestColorBufferCopyWebGL : AbstractApplicationTestFixture() {

    data class Spec(
        val width: Int,
        val height: Int,
        val format: ColorFormat = ColorFormat.RGBa,
        val type: ColorType = ColorType.UINT8,
        val multisample: BufferMultisample = BufferMultisample.Disabled,
        val levels: Int = 1
    )

    data class Case(
        val name: String,
        val source: Spec,
        val target: Spec,
        val sourceRectangle: IntRectangle? = null,
        val targetRectangle: IntRectangle? = null,
        val fromLevel: Int = 0,
        val toLevel: Int = 0,
        /** use copyTo without rectangles, it copies the overlapping region anchored at the bottom-left corner */
        val defaultOverload: Boolean = false
    )

    private val background = ColorRGBa(0.0, 0.0, 1.0, 1.0)

    private val context get() = (Driver.instance as DriverWebGL).context

    private fun create(spec: Spec, multisample: BufferMultisample = spec.multisample) = colorBuffer(
        spec.width, spec.height, 1.0, spec.format, spec.type, multisample, spec.levels
    )

    /** the gradient at pixel ([x], [y]) of a level of size [w] x [h] */
    private fun gradient(x: Int, y: Int, w: Int, h: Int) =
        ColorRGBa((x + 0.5) / w, (y + 0.5) / h, 0.5, 1.0)

    /** a source filled with the gradient, multisampled sources are filled by copying from a single sampled buffer */
    private fun source(spec: Spec): ColorBuffer {
        val single = create(spec, BufferMultisample.Disabled)
        val components = spec.format.componentCount
        val buffer = MPPBuffer.allocate(spec.width * spec.height * components * spec.type.componentSize)
        // the data holds the bottom row first, rows are top-down in the rectangles
        for (row in spec.height - 1 downTo 0) for (x in 0 until spec.width) {
            val color = gradient(x, row, spec.width, spec.height).let {
                if (spec.type == ColorType.UINT8_SRGB) it.toSRGB() else it
            }
            val values = listOf(color.r, color.g, color.b, color.alpha).take(components)
            for (value in values) when (spec.type) {
                ColorType.FLOAT32 -> buffer.putFloat(value.toFloat())
                ColorType.UINT8, ColorType.UINT8_SRGB -> buffer.put((value * 255.0).roundToInt().toByte())
                else -> error("unsupported source type ${spec.type}")
            }
        }
        buffer.rewind()
        single.write(buffer, spec.format, spec.type, 0, 0, spec.width, spec.height, 0)
        if (spec.levels > 1) {
            single.generateMipmaps()
        }
        if (spec.multisample == BufferMultisample.Disabled) {
            return single
        }
        val multisampled = create(spec)
        single.copyTo(multisampled)
        single.destroy()
        return multisampled
    }

    /** reads level 0 of [buffer] as linear colors, multisampled buffers are resolved first */
    private fun read(buffer: ColorBuffer, spec: Spec): List<List<ColorRGBa>> {
        val readable = if (spec.multisample == BufferMultisample.Disabled) buffer else {
            create(spec.copy(levels = 1), BufferMultisample.Disabled).also { buffer.copyTo(it) }
        }
        val float = spec.type.isFloat
        val pixels = readTexture(context, (readable as ColorBufferWebGL).texture, spec.width, spec.height, float)
        val scale = if (float) 1.0 else 255.0
        // readPixels returns the bottom row first
        val colors = (0 until spec.height).map { y ->
            val row = spec.height - 1 - y
            (0 until spec.width).map { x ->
                val i = (row * spec.width + x) * 4
                // sRGB color buffers hold encoded colors
                val decode: (Double) -> Double = if (spec.type == ColorType.UINT8_SRGB) ::decodeSrgb else { v -> v }
                ColorRGBa(
                    decode(valueAt(pixels, i) / scale), decode(valueAt(pixels, i + 1) / scale),
                    decode(valueAt(pixels, i + 2) / scale), valueAt(pixels, i + 3) / scale
                )
            }
        }
        if (readable !== buffer) readable.destroy()
        return colors
    }

    private fun close(a: ColorRGBa, b: ColorRGBa, tolerance: Double = 0.02) =
        abs(a.r - b.r) < tolerance && abs(a.g - b.g) < tolerance && abs(a.b - b.b) < tolerance &&
                abs(a.alpha - b.alpha) < tolerance

    /** runs [case], returns a description of the failure or null */
    private fun run(case: Case): String? {
        var source: ColorBuffer? = null
        var target: ColorBuffer? = null
        try {
            source = source(case.source)
            target = create(case.target)
            target.fill(background)
            val sourceLevel = IntRectangle(0, 0, case.source.width shr case.fromLevel, case.source.height shr case.fromLevel)
            val targetLevel = IntRectangle(0, 0, case.target.width shr case.toLevel, case.target.height shr case.toLevel)
            val sourceRectangle: IntRectangle
            val targetRectangle: IntRectangle
            if (case.defaultOverload) {
                val w = minOf(sourceLevel.width, targetLevel.width)
                val h = minOf(sourceLevel.height, targetLevel.height)
                sourceRectangle = IntRectangle(0, sourceLevel.height - h, w, h)
                targetRectangle = IntRectangle(0, targetLevel.height - h, w, h)
                source.copyTo(target, case.fromLevel, case.toLevel)
            } else {
                sourceRectangle = case.sourceRectangle ?: sourceLevel
                targetRectangle = case.targetRectangle ?: IntRectangle(0, 0, sourceRectangle.width, sourceRectangle.height)
                source.copyTo(target, case.fromLevel, case.toLevel, sourceRectangle, targetRectangle)
            }

            // the copied level is read through a buffer of its size
            val targetLevelSpec = case.target.copy(
                width = case.target.width shr case.toLevel, height = case.target.height shr case.toLevel, levels = 1
            )
            val pixels = if (case.toLevel == 0) read(target, case.target) else {
                val level = create(targetLevelSpec)
                target.copyTo(level, case.toLevel, 0)
                read(level, targetLevelSpec).also { level.destroy() }
            }
            var wrong = 0
            var first: String? = null
            for (y in 0 until targetLevelSpec.height) for (x in 0 until targetLevelSpec.width) {
                val inside = x >= targetRectangle.x && x < targetRectangle.x + targetRectangle.width &&
                        y >= targetRectangle.y && y < targetRectangle.y + targetRectangle.height
                val expected = if (inside) {
                    val sx = sourceRectangle.x + floor((x - targetRectangle.x + 0.5) * sourceRectangle.width / targetRectangle.width).toInt()
                    val sy = sourceRectangle.y + floor((y - targetRectangle.y + 0.5) * sourceRectangle.height / targetRectangle.height).toInt()
                    gradient(sx, sy, sourceLevel.width, sourceLevel.height)
                } else background
                if (!close(pixels[y][x], expected)) {
                    wrong++
                    if (first == null) first = "($x, $y) is ${pixels[y][x]}, expected $expected"
                }
            }
            return if (wrong > 0) "${case.name}: $wrong wrong pixels, $first" else null
        } catch (e: Throwable) {
            return "${case.name}: ${e::class.simpleName}: ${e.message?.lines()?.first()}"
        } finally {
            source?.destroy()
            target?.destroy()
        }
    }

    private val small = Spec(32, 24)
    private val large = Spec(64, 48)

    val cases = listOf(
        Case("full copy", small, small),
        Case("small into the top-left of large (#392)", small, large),
        Case("into an offset", small, large, targetRectangle = IntRectangle(20, 10, 32, 24)),
        Case("crop and offset", large, small, IntRectangle(30, 20, 16, 12), IntRectangle(5, 7, 16, 12)),
        Case("scaled up", small, large, IntRectangle(0, 0, 32, 24), IntRectangle(0, 0, 64, 48)),
        // the scale factors are chosen so that no target pixel samples exactly between two source pixels
        Case("scaled down", large, small, IntRectangle(0, 0, 60, 45), IntRectangle(0, 0, 32, 24)),
        Case("sRGB into linear", small.copy(type = ColorType.UINT8_SRGB), small),
        Case("linear into sRGB with offset", small, large.copy(type = ColorType.UINT8_SRGB), targetRectangle = IntRectangle(8, 4, 32, 24)),
        Case("float into 8 bit", small.copy(type = ColorType.FLOAT32), small),
        Case("float into 8 bit with offset", small.copy(type = ColorType.FLOAT32), large, targetRectangle = IntRectangle(8, 4, 32, 24)),
        Case("8 bit into float, scaled", small, large.copy(type = ColorType.FLOAT16), IntRectangle(0, 0, 32, 24), IntRectangle(0, 0, 64, 48)),
        Case("RGB into RGBa", small.copy(format = ColorFormat.RGB), small),
        Case("RGB into RGBa with offset", small.copy(format = ColorFormat.RGB), large, targetRectangle = IntRectangle(8, 4, 32, 24)),
        Case("multisampled into single sampled", small.copy(multisample = BufferMultisample.SampleCount(4)), small),
        Case("multisampled into an offset", small.copy(multisample = BufferMultisample.SampleCount(4)), large, targetRectangle = IntRectangle(8, 4, 32, 24)),
        Case("single sampled into multisampled", small, small.copy(multisample = BufferMultisample.SampleCount(4))),
        Case("single sampled into multisampled with offset", small, large.copy(multisample = BufferMultisample.SampleCount(4)), targetRectangle = IntRectangle(8, 4, 32, 24)),
        Case("multisampled into multisampled", small.copy(multisample = BufferMultisample.SampleCount(4)), small.copy(multisample = BufferMultisample.SampleCount(4))),
        Case("level 1 into level 0", large.copy(levels = 2), small, fromLevel = 1),
        Case("level 0 into level 1", small, large.copy(levels = 2), toLevel = 1),
        Case("default copy into a smaller target", large, small, defaultOverload = true),
        Case("default copy into a larger target", small, large, defaultOverload = true),
        Case("default copy into a smaller float target", large.copy(type = ColorType.FLOAT32), small.copy(type = ColorType.FLOAT32), defaultOverload = true),
        Case("target rectangle exceeding the target is clipped", small, large, targetRectangle = IntRectangle(48, 40, 32, 24)),
    )

    @Test
    fun copyTo() {
        val failures = cases.mapNotNull { run(it) }
        assertTrue(failures.isEmpty(), "${failures.size} of ${cases.size} cases failed:\n" + failures.joinToString("\n"))
    }
}

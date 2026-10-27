import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.*
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.shape.IntRectangle
import kotlin.math.abs
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies [ColorBuffer.copyTo] with source and target rectangles across formats, types, multisampling and mipmap
 * levels (https://github.com/openrndr/openrndr/issues/392, https://github.com/openrndr/openrndr/issues/425).
 *
 * Sources hold a gradient that encodes the source pixel coordinates, every target pixel is compared to the source
 * pixel it should be copied from (nearest sampling), pixels outside of the target rectangle must keep the
 * background color.
 */
class TestColorBufferCopyGL3 : AbstractApplicationTestFixture() {

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

    private val background = ColorRGBa(0.0, 0.0, 1.0, 1.0, Linearity.LINEAR)

    private fun create(spec: Spec, multisample: BufferMultisample = spec.multisample) = colorBuffer(
        spec.width, spec.height, 1.0, spec.format, spec.type, multisample, spec.levels
    )

    /** the gradient at pixel ([x], [y]) of a level of size [w] x [h] */
    private fun gradient(x: Int, y: Int, w: Int, h: Int) =
        ColorRGBa((x + 0.5) / w, (y + 0.5) / h, 0.5, 1.0, Linearity.LINEAR)

    /** a source filled with the gradient, multisampled sources are filled by copying from a single sampled buffer */
    private fun source(spec: Spec): ColorBuffer {
        val single = create(spec, BufferMultisample.Disabled)
        single.shadow.apply {
            for (y in 0 until spec.height) for (x in 0 until spec.width) {
                write(x, y, gradient(x, y, spec.width, spec.height))
            }
            upload()
        }
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

    /** reads level 0 of [buffer], multisampled buffers are resolved first */
    private fun read(buffer: ColorBuffer, spec: Spec): List<List<ColorRGBa>> {
        val readable = if (spec.multisample == BufferMultisample.Disabled) buffer else {
            create(spec.copy(levels = 1), BufferMultisample.Disabled).also { buffer.copyTo(it) }
        }
        readable.shadow.download()
        val pixels = (0 until spec.height).map { y ->
            (0 until spec.width).map { x -> readable.shadow[x, y].toLinear() }
        }
        if (readable !== buffer) readable.destroy()
        return pixels
    }

    private fun close(a: ColorRGBa, b: ColorRGBa, tolerance: Double = 0.02) =
        abs(a.r - b.r) < tolerance && abs(a.g - b.g) < tolerance && abs(a.b - b.b) < tolerance &&
                abs(a.alpha - b.alpha) < tolerance

    /** runs [case], returns a description of the failure or null */
    private fun run(case: Case): String? {
        val gles = DriverGL3Configuration.driverType == DriverTypeGL.GLES
        val source = source(case.source)
        val target = create(case.target)
        try {
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
            return if (wrong > 0) "${case.name} [${if (gles) "GLES" else "GL"}]: $wrong wrong pixels, $first" else null
        } catch (e: Throwable) {
            return "${case.name} [${if (gles) "GLES" else "GL"}]: ${e::class.simpleName}: ${e.message?.lines()?.first()}"
        } finally {
            source.destroy()
            target.destroy()
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

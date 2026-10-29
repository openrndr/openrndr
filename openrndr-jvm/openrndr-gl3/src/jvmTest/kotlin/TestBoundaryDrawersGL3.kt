import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.boundary.BoundaryMap
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.resourceUrl
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.windfoil.WindfoilDrawer
import org.openrndr.draw.windfoil.WindfoilMap
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBoundaryDrawersGL3 : AbstractApplicationTestFixture() {

    private fun slug() = SlugMap(
        colorBuffer(4096, 4, type = ColorType.FLOAT32, format = ColorFormat.RG),
        colorBuffer(4096, 4, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
    ) to SlugDrawer()

    private fun windfoil() = WindfoilMap(
        colorBuffer(4096, 4, type = ColorType.FLOAT32, format = ColorFormat.RG),
        colorBuffer(4096, 4, type = ColorType.UINT32_INT, format = ColorFormat.RGBa)
    ) to WindfoilDrawer()

    /** adds [shape] to [map], draws it with [boundaryDrawer] in a 32 x 32 target and returns the alpha of its pixels */
    private fun <M : BoundaryMap> coverage(map: M, boundaryDrawer: BoundaryDrawer<M>, shape: Shape, fillRule: FillRule): FloatArray {
        boundaryDrawer.contrast = 0.0
        val index = map.addShape(shape, fillRule)
        val target = renderTarget(32, 32, contentScale = 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
        boundaryDrawer.prepare(map, listOf(BoundaryCommand(index, Matrix44.IDENTITY, ColorRGBa.WHITE, null, 0.0)))
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            boundaryDrawer.draw(this, map)
        }
        val buffer = ByteBuffer.allocateDirect(32 * 32 * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(buffer, ColorFormat.RGBa, ColorType.FLOAT32)
        val colorBuffer = target.colorBuffer(0)
        target.detachColorAttachments()
        target.destroy()
        colorBuffer.destroy()
        boundaryDrawer.close()
        return FloatArray(32 * 32) { buffer.getFloat(it * 16 + 12) }
    }

    private val star = Shape(listOf(ShapeContour.fromPoints((0 until 5).map { i ->
        val a = Math.PI * 2.0 * i * 2 / 5 - Math.PI / 2
        Vector2(16.0, 16.0) + Vector2(cos(a), sin(a)) * 15.0
    }, closed = true)))

    @Test
    fun `slug and windfoil drawers are interchangeable`() {
        val rectangle = Rectangle(4.0, 8.0, 20.0, 12.0).shape
        val (slugMap, slugDrawer) = slug()
        val (windfoilMap, windfoilDrawer) = windfoil()
        for (pixels in listOf(coverage(slugMap, slugDrawer, rectangle, FillRule.NON_ZERO), coverage(windfoilMap, windfoilDrawer, rectangle, FillRule.NON_ZERO))) {
            assertEquals(1.0f, pixels[14 * 32 + 14], 1E-3f)
            assertEquals(0.0f, pixels[2 * 32 + 2], 1E-3f)
            assertEquals(20.0 * 12.0, pixels.sumOf { it.toDouble() }, 0.5)
        }
    }

    @Test
    fun `slug and windfoil follow the fill rule`() {
        val center = 16 * 32 + 16
        for (create in listOf({ slug() }, { windfoil() })) {
            val (evenOddMap, evenOddDrawer) = create()
            val (nonZeroMap, nonZeroDrawer) = create()
            @Suppress("UNCHECKED_CAST")
            val evenOdd = coverage(evenOddMap as BoundaryMap, evenOddDrawer as BoundaryDrawer<BoundaryMap>, star, FillRule.EVEN_ODD)
            @Suppress("UNCHECKED_CAST")
            val nonZero = coverage(nonZeroMap as BoundaryMap, nonZeroDrawer as BoundaryDrawer<BoundaryMap>, star, FillRule.NON_ZERO)
            assertEquals(0.0f, evenOdd[center], 1E-3f, "even-odd center of ${evenOddDrawer::class.simpleName}")
            assertEquals(1.0f, nonZero[center], 1E-3f, "nonzero center of ${nonZeroDrawer::class.simpleName}")
        }
    }

    @Test
    fun `glyph maps work with any boundary map`() {
        val face = org.openrndr.draw.font.loadFace(resourceUrl("/fonts/Roboto-Medium.ttf"))
        val (windfoilMap, _) = windfoil()
        val glyphMap = BoundaryGlyphMap(windfoilMap)
        val a = glyphMap.getGlyph(face, 'a')
        assertEquals(a, glyphMap.getGlyph(face, 'a'))
        assertEquals(1, windfoilMap.shapes.size)
    }
}

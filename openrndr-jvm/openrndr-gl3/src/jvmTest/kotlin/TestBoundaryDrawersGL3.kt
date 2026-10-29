import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.boundary.BoundaryMap
import org.openrndr.draw.boundary.boundaryMap
import org.openrndr.draw.boundary.createBoundaryMap
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.windfoil.WindfoilDrawer
import org.openrndr.draw.windfoil.WindfoilMap
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.resourceUrl
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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestBoundaryDrawersGL3 : AbstractApplicationTestFixture() {

    /** draws [commands] from [map] with [boundaryDrawer] in a [width] x [height] target and returns the alpha of its pixels */
    private fun render(boundaryDrawer: BoundaryDrawer, map: BoundaryMap, commands: List<BoundaryCommand>, width: Int = 32, height: Int = 32): FloatArray {
        val instances = boundaryDrawer.createInstanceBuffer(commands.size)
        val command = boundaryDrawer.prepare(map, commands, instances)
        val target = renderTarget(width, height, contentScale = 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            boundaryDrawer.draw(this, map, command)
        }
        val buffer = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(buffer, ColorFormat.RGBa, ColorType.FLOAT32)
        val colorBuffer = target.colorBuffer(0)
        target.detachColorAttachments()
        target.destroy()
        colorBuffer.destroy()
        instances.destroy()
        return FloatArray(width * height) { buffer.getFloat(it * 16 + 12) }
    }

    /** adds [shape] to a map created by [boundaryDrawer] and returns the alpha of the drawn pixels */
    private fun coverage(boundaryDrawer: BoundaryDrawer, shape: Shape, fillRule: FillRule = FillRule.NON_ZERO): FloatArray {
        boundaryDrawer.contrast = 0.0
        val map = boundaryDrawer.createMap()
        val index = map.addShape(shape, fillRule)
        return render(boundaryDrawer, map, listOf(BoundaryCommand(index, Matrix44.IDENTITY, ColorRGBa.WHITE, null, 0.0)))
    }

    private val star = Shape(listOf(ShapeContour.fromPoints((0 until 5).map { i ->
        val a = Math.PI * 2.0 * i * 2 / 5 - Math.PI / 2
        Vector2(16.0, 16.0) + Vector2(cos(a), sin(a)) * 15.0
    }, closed = true)))

    @Test
    fun `slug and windfoil drawers are interchangeable`() {
        val rectangle = Rectangle(4.0, 8.0, 20.0, 12.0).shape
        for (useSlug in listOf(true, false)) {
            val boundaryDrawer: BoundaryDrawer = if (useSlug) SlugDrawer() else WindfoilDrawer()
            val pixels = coverage(boundaryDrawer, rectangle)
            assertEquals(1.0f, pixels[14 * 32 + 14], 1E-3f)
            assertEquals(0.0f, pixels[2 * 32 + 2], 1E-3f)
            assertEquals(20.0 * 12.0, pixels.sumOf { it.toDouble() }, 0.5)
            boundaryDrawer.close()
        }
    }

    @Test
    fun `slug and windfoil follow the fill rule`() {
        val center = 16 * 32 + 16
        for (boundaryDrawer in listOf(SlugDrawer(), WindfoilDrawer())) {
            assertEquals(0.0f, coverage(boundaryDrawer, star, FillRule.EVEN_ODD)[center], 1E-3f, "even-odd center of ${boundaryDrawer::class.simpleName}")
            assertEquals(1.0f, coverage(boundaryDrawer, star, FillRule.NON_ZERO)[center], 1E-3f, "nonzero center of ${boundaryDrawer::class.simpleName}")
            boundaryDrawer.close()
        }
    }

    @Test
    fun `drawing from another kind of map fails`() {
        val slugDrawer = SlugDrawer()
        val windfoilMap = WindfoilDrawer().createMap()
        val index = windfoilMap.addShape(star)
        val error = assertFailsWith<IllegalArgumentException> {
            slugDrawer.prepare(windfoilMap, listOf(BoundaryCommand(index, Matrix44.IDENTITY, ColorRGBa.WHITE, null, 0.0)))
        }
        assertTrue(error.message!!.contains("SlugMap"))
        slugDrawer.close()
    }

    @Test
    fun `boundary maps are created for a drawer`() {
        assertIs<SlugMap>(SlugDrawer().createBoundaryMap(64, 64))
        assertIs<WindfoilMap>(WindfoilDrawer().createBoundaryMap(64, 64))
        assertIs<WindfoilMap>(program.drawer.boundaryDrawer.createMap())
    }

    @Test
    fun `glyph maps work with any boundary map`() {
        val face = org.openrndr.draw.font.loadFace(resourceUrl("/fonts/Roboto-Medium.ttf"))
        val map = WindfoilDrawer().createMap()
        val glyphMap = BoundaryGlyphMap(map)
        val a = glyphMap.getGlyph(face, 'a')
        assertEquals(a, glyphMap.getGlyph(face, 'a'))
        assertEquals(1, (map as WindfoilMap).shapes.size)
    }

    @Test
    fun `the boundary drawer of the drawer draws in maps it creates`() {
        val boundaryDrawer = program.drawer.boundaryDrawer
        assertIs<WindfoilDrawer>(boundaryDrawer)
        val pixels = coverage(boundaryDrawer, Rectangle(4.0, 8.0, 20.0, 12.0).shape)
        assertEquals(20.0 * 12.0, pixels.sumOf { it.toDouble() }, 0.5)
    }

    /** draws the glyphs of [text] from [map] with [boundaryDrawer] */
    private fun drawText(boundaryDrawer: BoundaryDrawer, map: BoundaryMap, text: String): FloatArray {
        val face = org.openrndr.draw.font.loadFace(resourceUrl("/fonts/Roboto-Medium.ttf"))
        val glyphs = BoundaryGlyphMap(map)
        var x = 2.0
        val commands = text.map { c ->
            val command = BoundaryCommand(glyphs.getGlyph(face, c), buildTransform { translate(x, 24.0) }, ColorRGBa.WHITE, null, 0.0)
            x += face.glyphForCharacter(c).advanceWidth()
            command
        }
        return render(boundaryDrawer, map, commands, 128, 32)
    }

    @Test
    fun `maps of any width draw the same`() {
        val text = "Boundary maps 8&@"
        val slug = SlugDrawer()
        val slugWide = drawText(slug, boundaryMap<SlugMap>(4096, 8), text)
        val slugNarrow = drawText(slug, boundaryMap<SlugMap>(37, 512), text)
        val windfoil = WindfoilDrawer()
        val windfoilWide = drawText(windfoil, boundaryMap<WindfoilMap>(4096, 8), text)
        val windfoilNarrow = drawText(windfoil, windfoil.createBoundaryMap(37, 512), text)
        assertTrue(slugWide.sum() > 100.0f, "the text is drawn")
        assertEquals(slugWide.toList(), slugNarrow.toList())
        assertEquals(windfoilWide.toList(), windfoilNarrow.toList())
        slug.close()
        windfoil.close()
    }
}

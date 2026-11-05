import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.PaintOrder
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.windfoil.WindfoilDrawer
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The stroke is painted over the fill, or the fill over the stroke, like the `paint-order` of SVG
 */
class TestPaintOrderGL3 : AbstractApplicationTestFixture() {
    private val size = 100

    /** draws with a red stroke 10 wide, centered on the outline, and a green fill, returns the color at pixels */
    private fun paint(paintOrder: PaintOrder?, draw: Drawer.() -> Unit): (Int, Int) -> ColorRGBa {
        val target = renderTarget(size, size, 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32); depthBuffer() }
        program.drawer.isolatedWithTarget(target) {
            ortho(target)
            clear(ColorRGBa.TRANSPARENT)
            fill = ColorRGBa.GREEN
            stroke = ColorRGBa.RED
            strokeWeight = 10.0
            if (paintOrder != null) this.paintOrder = paintOrder
            draw()
        }
        val data = ByteBuffer.allocateDirect(size * size * 16).order(ByteOrder.nativeOrder())
        target.colorBuffer(0).read(data, ColorFormat.RGBa, ColorType.FLOAT32)
        target.destroy()
        return { x, y ->
            val i = ((size - 1 - y) * size + x) * 16
            ColorRGBa(data.getFloat(i).toDouble(), data.getFloat(i + 4).toDouble(), data.getFloat(i + 8).toDouble(), data.getFloat(i + 12).toDouble())
        }
    }

    /**
     * asserts that the inner half of the stroke, at pixel [inside], is red when the stroke is painted last and green
     * when the fill is, and that the outer half, at [outside], is red, and the inside of the fill at [center] green
     */
    private fun assertPaintOrder(name: String, inside: Pair<Int, Int>, outside: Pair<Int, Int>, center: Pair<Int, Int>, draw: Drawer.() -> Unit) {
        for (paintOrder in listOf(null, PaintOrder.FILL_STROKE, PaintOrder.STROKE_FILL)) {
            val color = paint(paintOrder, draw)
            val strokeOnTop = paintOrder != PaintOrder.STROKE_FILL
            val expected = mapOf(
                inside to if (strokeOnTop) ColorRGBa.RED else ColorRGBa.GREEN,
                outside to ColorRGBa.RED,
                center to ColorRGBa.GREEN
            )
            for ((pixel, e) in expected) {
                val c = color(pixel.first, pixel.second)
                assertEquals(e.r, c.r, 1E-3, "$name $paintOrder pixel $pixel red")
                assertEquals(e.g, c.g, 1E-3, "$name $paintOrder pixel $pixel green")
                assertEquals(1.0, c.alpha, 1E-3, "$name $paintOrder pixel $pixel alpha")
            }
        }
    }

    // the outline of the geometry is at 20 and 80, the stroke spans 15 to 25 and 75 to 85
    private val rectangle = Rectangle(20.0, 20.0, 60.0, 60.0)

    @Test
    fun shapesAndContours() {
        assertPaintOrder("shape", 22 to 50, 17 to 50, 50 to 50) { shape(rectangle.shape) }
        // a shape with a hole is filled through the stencil
        val holed = Shape(listOf(rectangle.contour, Rectangle(40.0, 40.0, 20.0, 20.0).contour.reversed))
        assertPaintOrder("shape with a hole", 22 to 50, 17 to 50, 30 to 30) { shape(holed) }
        assertPaintOrder("contour", 22 to 50, 17 to 50, 50 to 50) { contour(rectangle.contour) }
    }

    @Test
    fun circlesAndRectangles() {
        assertPaintOrder("rectangle", 22 to 50, 17 to 50, 50 to 50) { rectangle(rectangle) }
        assertPaintOrder("circle", 22 to 50, 17 to 50, 50 to 50) { circle(Circle(50.0, 50.0, 30.0)) }
    }

    @Test
    fun boundaryDrawers() {
        for (boundaryDrawer in listOf<BoundaryDrawer>(SlugDrawer(), WindfoilDrawer())) {
            boundaryDrawer.contrast = 0.0
            val map = boundaryDrawer.createMap()
            val index = map.addShape(rectangle.shape)
            val commands = listOf(BoundaryCommand(index, Matrix44.IDENTITY, ColorRGBa.GREEN, ColorRGBa.RED, 10.0))
            val instances = boundaryDrawer.createInstanceBuffer(commands.size)
            val command = boundaryDrawer.prepare(map, commands, instances)
            assertPaintOrder(boundaryDrawer::class.simpleName!!, 22 to 50, 17 to 50, 50 to 50) {
                boundaryDrawer.draw(this, map, command)
            }
            instances.destroy()
            boundaryDrawer.close()
        }
    }
}

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.windfoil.WindfoilDrawer
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Boundary drawers, which draw text, apply the shade style of the drawer: a style can clip text to a box in screen
 * coordinates by its coverage
 */
class TestBoundaryShadeStylesGL3 : AbstractApplicationTestFixture() {
    private val width = 64
    private val height = 32

    @Test
    fun `boundary drawers apply the shade style of the drawer`() {
        for (boundaryDrawer in listOf<BoundaryDrawer>(SlugDrawer(), WindfoilDrawer())) {
            val name = boundaryDrawer::class.simpleName
            boundaryDrawer.contrast = 0.0
            val map = boundaryDrawer.createMap()
            val index = map.addShape(Rectangle(0.0, 0.0, 64.0, 32.0).shape)
            val commands = listOf(BoundaryCommand(index, Matrix44.IDENTITY, ColorRGBa.WHITE, null, 0.0))
            val instances = boundaryDrawer.createInstanceBuffer(commands.size)
            val command = boundaryDrawer.prepare(map, commands, instances)
            val target = renderTarget(width, height, 2.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
            program.drawer.isolatedWithTarget(target) {
                ortho(target)
                clear(ColorRGBa.TRANSPARENT)
                // keeps what lies in the boxes in points with y pointing down, the second box is selected by an int,
                // the matrix and the fill of the style are passed through
                shadeStyle = shadeStyle {
                    fragmentPreamble = """
                        float inside(vec4 box, vec2 p) {
                            return (p.x >= box.x && p.x < box.x + box.z && p.y >= box.y && p.y < box.y + box.w) ? 1.0 : 0.0;
                        }
                    """.trimIndent()
                    fragmentTransform = """
                        vec2 p = vec2(c_screenPosition.x, c_screenSize.y - c_screenPosition.y);
                        x_fill.a *= max(inside(p_boxes[0], p), inside(p_boxes[p_second], p)) * p_scale[0][0];
                    """.trimIndent()
                    parameter("boxes", arrayOf(Vector4(0.0, 0.0, 16.0, 32.0), Vector4(1000.0, 0.0, 0.0, 0.0), Vector4(48.0, 0.0, 16.0, 32.0)))
                    parameter("second", 2)
                    parameter("scale", Matrix44.IDENTITY)
                }
                boundaryDrawer.draw(this, map, command)
            }
            val data = ByteBuffer.allocateDirect(width * height * 4 * 16).order(ByteOrder.nativeOrder())
            target.colorBuffer(0).read(data, ColorFormat.RGBa, ColorType.FLOAT32)
            val pixels = target.colorBuffer(0).effectiveWidth
            fun alpha(x: Int, y: Int) = data.getFloat(((y * 2) * pixels + x * 2) * 16 + 12)
            // the boxes are kept, the middle is clipped
            assertEquals(1.0f, alpha(8, 16), 1E-3f, "$name inside the first box")
            assertEquals(0.0f, alpha(32, 16), 1E-3f, "$name between the boxes")
            assertEquals(1.0f, alpha(56, 16), 1E-3f, "$name inside the second box")
            target.destroy()
            instances.destroy()
            boundaryDrawer.close()
        }
    }
}

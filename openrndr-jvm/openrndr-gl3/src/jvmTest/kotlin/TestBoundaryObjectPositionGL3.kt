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
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Shade styles of boundary drawers read `v_objectPosition`, the position in the coordinates of the text or shapes:
 * after the transform of a glyph, before the model matrix, like the expansion shaders of shapes
 */
class TestBoundaryObjectPositionGL3 : AbstractApplicationTestFixture() {
    private val width = 128
    private val height = 64

    @Test
    fun `slug and windfoil shade styles read object positions`() {
        for (boundaryDrawer in listOf<BoundaryDrawer>(SlugDrawer(), WindfoilDrawer())) {
            val name = boundaryDrawer::class.simpleName
            boundaryDrawer.contrast = 0.0
            val map = boundaryDrawer.createMap()
            val index = map.addShape(Rectangle(0.0, 0.0, 20.0, 10.0).shape)
            // two glyphs of the same shape, placed and scaled by their transforms: (10, 5)-(50, 25) and (60, 5)-(100, 25)
            val commands = listOf(10.0, 60.0).map { x ->
                BoundaryCommand(index, buildTransform { translate(x, 5.0); scale(2.0) }, ColorRGBa.WHITE, null, 0.0)
            }
            val instances = boundaryDrawer.createInstanceBuffer(commands.size)
            val command = boundaryDrawer.prepare(map, commands, instances)
            val target = renderTarget(width, height, 1.0) { colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32) }
            program.drawer.isolatedWithTarget(target) {
                ortho(target)
                clear(ColorRGBa.TRANSPARENT)
                // the model matrix moves the text, object positions do not move with it
                translate(4.0, 20.0)
                shadeStyle = shadeStyle { fragmentTransform = "x_fill = vec4(v_objectPosition.xy / 128.0, 0.0, 1.0);" }
                boundaryDrawer.draw(this, map, command)
            }
            val data = ByteBuffer.allocateDirect(width * height * 16).order(ByteOrder.nativeOrder())
            target.colorBuffer(0).read(data, ColorFormat.RGBa, ColorType.FLOAT32)

            /** asserts the object position at the center of pixel ([x], [y]) */
            fun assertObjectPosition(x: Int, y: Int) {
                val i = ((height - 1 - y) * width + x) * 16
                val actual = Vector2(data.getFloat(i).toDouble(), data.getFloat(i + 4).toDouble()) * 128.0
                val expected = Vector2(x + 0.5 - 4.0, y + 0.5 - 20.0)
                assertTrue(abs(actual.x - expected.x) < 0.05 && abs(actual.y - expected.y) < 0.05,
                    "$name pixel ($x, $y): expected $expected, was $actual")
            }
            // in both glyphs, near their corners and centers, the value follows object coordinates
            assertObjectPosition(16, 27)
            assertObjectPosition(34, 35)
            assertObjectPosition(52, 43)
            assertObjectPosition(66, 27)
            assertObjectPosition(84, 35)

            target.destroy()
            instances.destroy()
            boundaryDrawer.close()
        }
    }
}

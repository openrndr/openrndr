import org.openrndr.draw.*
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.resourceUrl
import kotlin.test.*

class TestShadeStylesSuppressOutput : AbstractApplicationTestFixture() {
    @BeforeTest
    override fun setup() {
        super.setup()
        program.drawer.shadeStyle = shadeStyle {
            suppressDefaultOutput = true
        }
    }

    @Test
    fun circle() {
        program.drawer.circle(Vector2(100.0, 100.0), 20.0)
    }

    @Test
    fun `rectangle should be able to do shadestyles`() {
        program.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
    }

    @Test
    fun line() {
        program.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
    }

    @Test
    fun `fast line`() {
        program.drawer.drawStyle.quality = DrawQuality.PERFORMANCE
        program.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
        program.drawer.drawStyle.quality = DrawQuality.QUALITY
    }

    @Test
    fun `mesh line`() {
        program.drawer.lineSegment(Vector3(0.0, 0.0, 0.0), Vector3(100.0, 0.0, 0.0))
    }

    @Test
    fun `vertex buffer`() {
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10, null)
        program.drawer.vertexBuffer(vbgl3, DrawPrimitive.TRIANGLES)
        vbgl3.destroy()
    }

    @Test
    fun image() {
        val cb = colorBuffer(640, 640)
        program.drawer.image(cb)
        cb.destroy()
    }

    @Test
    fun `font image maps`() {
        val font = program.loadFont(resourceUrl("/fonts/Roboto-Medium.ttf"), 18.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }
}
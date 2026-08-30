import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import kotlin.test.Test

class TestRenderTargetWebGL : AbstractApplicationTestFixture() {
    @Test
    fun testRenderTargetDraw() {
        val rt = renderTarget(100, 100) {
            colorBuffer()
            depthBuffer()
        }
        val drawer = program.drawer
        drawer.isolatedWithTarget(rt) {
            drawer.clear(ColorRGBa.BLACK)
        }
        rt.destroy()
    }
}
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import kotlin.test.Test
import kotlin.test.assertNotNull

class TestWebGL : AbstractApplicationTestFixture() {
    @Test
    fun testWebGL() {
        assertNotNull(application.context)
        assertNotNull(program.drawer)
        program.drawer.clear(ColorRGBa.RED)
    }

    @Test
    fun testColorBuffer() {
        val cb = colorBuffer(100, 100)
        cb.destroy()
    }
}
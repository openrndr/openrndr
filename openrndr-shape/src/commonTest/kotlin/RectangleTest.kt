import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.test.*

class RectangleTest {

    @Test
    fun createFromAnchor() {
        val r00 = Rectangle.fromAnchor(Vector2.ZERO, Vector2(300.0, 300.0), 100.0, 100.0)
        val r11 = Rectangle.fromAnchor(Vector2.ONE, Vector2(300.0, 300.0), 100.0, 100.0)

        assertEquals(r00.position(Vector2.ZERO), Vector2(300.0, 300.0))
        assertEquals(r11.position(Vector2.ONE), Vector2(300.0, 300.0))

        val rcc = Rectangle.fromAnchor(Vector2(0.5, 0.5), Vector2(300.0, 300.0), 100.0, 200.0)

        assertEquals(rcc, Rectangle(250.0, 200.0, 100.0, 200.0))
        assertEquals(rcc, Rectangle.fromCenter(Vector2(300.0, 300.0), 100.0, 200.0))
    }
}
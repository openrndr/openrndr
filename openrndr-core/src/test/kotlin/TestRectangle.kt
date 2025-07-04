import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Winding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestRectangle {
    @Test
    fun `A rectangle's default height equals its width`() {
        val width = 200.0

        assertEquals(width, Rectangle(50.0, 50.0, width).height)
        assertEquals(width, Rectangle.fromCenter(Vector2.ZERO, width).height)
        assertEquals(width, Rectangle(Vector2.ZERO, width).height)
    }

    val c1 = Rectangle(50.0, 50.0, 200.0, 200.0).contour

    @Test
    fun `A rectangle's contour is closed`() {
        assertTrue(c1.closed)
    }

    @Test
    fun `A rectangle's contour has CCW winding`() {
        assertEquals(Winding.CLOCKWISE, c1.winding)
    }

    @Test
    fun `A rectangle's contour has CC negative y`() {
        assertEquals(YPolarity.CW_NEGATIVE_Y, c1.polarity)
    }

    // testing for issue described in https://github.com/openrndr/openrndr/issues/135
    val c2 = Rectangle(100.0, 100.0, 300.0, 300.0).contour

    @Test
    fun `A specific rectangle's contour can be sampled for adaptive positions`() {
        val aps = c2.adaptivePositions()
        for (p in aps) {
            assertTrue(p.x >= 100.0)
            assertTrue(p.y >= 100.0)
            assertTrue(p.x <= 400.0)
            assertTrue(p.y <= 400.0)
        }
    }

    @Test
    fun `A specific rectangle's contour can be sampled for equidistant positions`() {
        val eps = c2.equidistantPositions(10)

        assertEquals(10, eps.size)

        for (p in eps) {
            assertTrue(p.x >= 100.0 - 1E-6)
            assertTrue(p.y >= 100.0 - 1E-6)
            assertTrue(p.x <= 400.0 + 1E-6)
            assertTrue(p.y <= 400.0 + 1E-6)
        }
    }

    val a = Rectangle.fromCenter(Vector2.ZERO, 50.0)
    val b = Rectangle.fromCenter(Vector2.ZERO, 100.0)
    val c = Rectangle(400.0, 400.0, 50.0)
    val d = Rectangle(410.0, 410.0, 50.0)

    @Test
    fun `A rectangle intersects other rectangles`() {
        assertTrue(a.intersects(b))
        assertTrue(b.intersects(a))
        assertTrue(c.intersects(d))
        assertTrue(a.intersects(a))
    }

    @Test
    fun `A rectangle doesn't intersect other rectangles`() {
        assertFalse(a.intersects(c))
    }
}
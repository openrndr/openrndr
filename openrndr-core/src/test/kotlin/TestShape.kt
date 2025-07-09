import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.Winding
import org.openrndr.shape.shape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestShape {
    @Test
    fun `two equivalent shapes should be equal`() {
        val a = Circle(40.0, 40.0, 100.0).shape
        val b = Circle(40.0, 40.0, 100.0).shape
        assertEquals(a, b)
    }

    @Test
    fun `two non-equivalent shapes should not be equal`() {
        val a = Circle(40.0, 40.0, 100.0).shape
        val b = Circle(40.0, 80.0, 100.0).shape
        assertNotEquals(a, b)
    }

    val width = 640
    val height = 480

    val s = shape {
        contour {
            moveTo(Vector2(width / 2.0 - 150.0, height / 2.0 - 150.00))
            lineTo(cursor + Vector2(300.0, 0.0))
            lineTo(cursor + Vector2(0.0, 300.0))
            lineTo(anchor)
            close()
        }
        contour {
            moveTo(Vector2(width / 2.0 - 80.0, height / 2.0 - 100.0))
            lineTo(cursor + Vector2(200.0, 0.0))
            lineTo(cursor + Vector2(0.0, 200.00))
            lineTo(anchor)
            close()
        }
    }

    @Test
    fun `a shape should have 2 contours`() {
        assertEquals(2, s.contours.size)
    }

    @Test
    fun `all contours in the shape should be closed`() {
        assertTrue { s.contours.all { it.closed } }
    }

    @Test
    fun `a shape all contours have negative polarity`() {
        assertTrue(s.contours.all { it.polarity == YPolarity.CW_NEGATIVE_Y })
    }

    @Test
    fun `a shape first contour should have clockwise winding`() {
        assertEquals(Winding.CLOCKWISE, s.contours.first().winding)
    }

    @Test
    fun `second contour should have counter-clockwise winding`() {
        assertEquals(Winding.COUNTER_CLOCKWISE, s.contours[1].winding)
    }
}
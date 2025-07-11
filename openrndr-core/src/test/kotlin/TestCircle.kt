import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.Winding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCircle {
    @Test
    fun `A circle's contour`() {
        val c = Circle(200.0, 200.0, 200.0).contour

        assertTrue(
            c.closed,
            "should be closed"
        )

        assertEquals(
            Winding.CLOCKWISE, c.winding,
            "should have CW winding"
        )

        assertEquals(
            YPolarity.CW_NEGATIVE_Y, c.polarity,
            "should have CW negative y polarity"
        )
    }
}
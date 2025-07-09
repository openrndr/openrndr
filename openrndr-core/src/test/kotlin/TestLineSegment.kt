import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.intersection
import kotlin.test.Test

class TestLineSegment {
    @Test
    fun `a crossing horizontal and vertical line segment should intersect`() {
        val h = LineSegment(-100.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, -100.0, 0.0, 100.0)
        val i = intersection(h, v, eps = 0.0)
        assertVeryNear(Vector2.ZERO, i)
    }

    @Test
    fun `a ┬ shaped crossing of horizontal and vertical line segments should intersect`() {
        val h = LineSegment(-100.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, 0.0, 0.0, 100.0)
        val i = intersection(h, v, eps = 0.0)
        assertVeryNear(Vector2.ZERO, i)
    }

    @Test
    fun `a ├ shaped crossing of horizontal and vertical line segments should intersect`() {
        val h = LineSegment(0.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, -100.0, 0.0, 100.0)
        val i = intersection(h, v, eps = 0.0)
        assertVeryNear(Vector2.ZERO, i)
    }

    @Test
    fun `a L shaped crossing horizontal and vertical line segment should intersect`() {
        val h = LineSegment(0.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, 0.0, 0.0, 100.0)
        val i = intersection(h, v, eps = 0.0)
        assertVeryNear(Vector2.ZERO, i)
    }

    @Test
    fun `two parallel lines should not intersect`() {
        val h = LineSegment(-100.0, 0.0, 100.0, 0.0)
        val v = LineSegment(-100.0, 1.0, 100.0, 1.0)
        val i = intersection(h, v)
        assertVeryNear(Vector2.INFINITY, i)
    }
}
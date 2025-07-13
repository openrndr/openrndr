import org.openrndr.math.Vector2
import org.openrndr.shape.Triangle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestTriangle {
    @Test
    fun `a triangle should contain inner points`() {
        val t = Triangle(Vector2(0.0, 0.0), Vector2(0.0, 100.0), Vector2(100.0, 100.0))
        assertTrue(Vector2(1.0, 90.0) in t, "should contain inner point")
    }

    @Test
    fun `a triangle should contain edge points`() {
        val t = Triangle(Vector2(0.0, 0.0), Vector2(0.0, 100.0), Vector2(100.0, 100.0))
        assertTrue(Vector2(30.0, 30.0) in t, "should contain edge points")
    }

    @Test
    fun `a triangle should not contain outer points`() {
        val t = Triangle(Vector2(0.0, 0.0), Vector2(0.0, 100.0), Vector2(100.0, 100.0))
        assertFalse(Vector2(31.1, 30.0) in t, "should not contain outer points")
        assertFalse(Vector2(40.0, 30.0) in t, "should not contain outer points")
        assertFalse(Vector2(200.0, 200.0) in t, "should not contain outer points")
    }
}
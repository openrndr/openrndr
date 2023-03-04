import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Triangle
import kotlin.test.Test
import kotlin.test.assertTrue

class TriangleTest {

    @Test
    fun barycentricCenter() {
        val t = Triangle(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0))
        val centroid = t.centroid
        val baryCenter = t.position(Vector3(1.0/3.0, 1.0/3.0, 1.0/3.0))
        assertTrue(centroid.distanceTo(baryCenter) < 1E-6)
    }
}
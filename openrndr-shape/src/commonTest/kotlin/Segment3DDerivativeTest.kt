import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The derivatives and directions of 3D segments point from their start to their end
 */
class Segment3DDerivativeTest {
    private fun assertNear(expected: Vector3, actual: Vector3, tolerance: Double, message: String) =
        assertTrue(expected.distanceTo(actual) <= tolerance, "$message: expected $expected, was $actual")

    @Test
    fun derivativesOfLinearSegmentsPointFromStartToEnd() {
        val segment = Segment3D(Vector3.ZERO, Vector3(1.0, 0.0, 0.0))
        assertEquals(Vector3(1.0, 0.0, 0.0), segment.derivative(0.5))
        assertEquals(segment.direction(), segment.direction(0.5))
    }

    @Test
    fun derivativesAreTheSlopesOfThePositions() {
        val segments = listOf(
            Segment3D(Vector3(1.0, 2.0, 3.0), Vector3(-4.0, 5.0, 9.0)),
            Segment3D(Vector3(0.0, 0.0, 0.0), Vector3(5.0, 10.0, -2.0), Vector3(10.0, 0.0, 4.0)),
            Segment3D(Vector3(0.0, 0.0, 0.0), Vector3(2.0, 18.0, 5.0), Vector3(26.0, -4.0, -3.0), Vector3(30.0, 12.0, 9.0))
        )
        val h = 1E-6
        for (segment in segments) {
            for (t in listOf(0.1, 0.5, 0.9)) {
                val slope = (segment.position(t + h) - segment.position(t - h)) / (2.0 * h)
                assertNear(slope, segment.derivative(t), 1E-4, "derivative of ${segment.control.size}-control segment at $t")
                assertNear(slope.normalized, segment.direction(t), 1E-6, "direction of ${segment.control.size}-control segment at $t")
            }
        }
    }

    @Test
    fun pathDirectionsFollowLinearSegments() {
        val path = Path3D.fromPoints(listOf(Vector3.ZERO, Vector3(10.0, 0.0, 0.0), Vector3(10.0, 0.0, 10.0)), false)
        assertNear(Vector3(1.0, 0.0, 0.0), path.direction(0.25), 1E-9, "first segment")
        assertNear(Vector3(0.0, 0.0, 1.0), path.direction(0.75), 1E-9, "second segment")
    }
}

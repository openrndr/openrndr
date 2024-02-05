import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import kotlin.test.Test
import kotlin.test.assertEquals

class Path3DTest {
    private fun assertEquals(expected: Vector3, actual: Vector3, absoluteTolerance: Double, message: String? = null) {
        assertEquals(expected.x, actual.x, absoluteTolerance, "$message (x differs)")
        assertEquals(expected.y, actual.y, absoluteTolerance, "$message (y differs)")
        assertEquals(expected.z, actual.z, absoluteTolerance, "$message (z differs)")
    }

    @Test
    fun shouldCalculatePosition() {
        val tolerance = 0.0005

        val start = Vector3.UNIT_X * -5.0
        val mid = Vector3.UNIT_Y
        val end = Vector3.UNIT_X * 5.0

        val path3D = Path3D.fromPoints(listOf(start, mid, end), false)

        assertEquals(
            start, path3D.position(0.0), tolerance,
            "path3D.position(0.0) should return path start"
        )
        assertEquals(
            mid, path3D.position(0.5), tolerance,
            "path3D.position(0.5) should return path mid point"
        )
        assertEquals(
            end, path3D.position(1.0), tolerance,
            "path3D.position(1.0) should return path end"
        )
        assertEquals(
            start.mix(mid, 0.2), path3D.position(0.1), tolerance,
            "path3D.position(0.1) should equal point at t=0.2 between start and mid"
        )
    }
}
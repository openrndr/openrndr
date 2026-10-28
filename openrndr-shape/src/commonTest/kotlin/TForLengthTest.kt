import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import kotlin.test.Test
import kotlin.test.assertEquals

class TForLengthTest {

    // an asymmetric cubic, its speed varies along the curve so arc length and t differ
    private val cubic2 = Segment2D(Vector2(0.0, 0.0), Vector2(20.0, 180.0), Vector2(260.0, -40.0), Vector2(300.0, 120.0))
    private val quadratic2 = Segment2D(Vector2(0.0, 0.0), Vector2(250.0, 10.0), Vector2(100.0, 200.0))
    private val cubic3 = Segment3D(Vector3(0.0, 0.0, 0.0), Vector3(20.0, 180.0, 50.0), Vector3(260.0, -40.0, -30.0), Vector3(300.0, 120.0, 90.0))

    /** checks that the curve from 0 to tForLength(length) is [length] long, for lengths that are not on the LUT */
    private fun assertArcLengths(segmentLength: Double, tForLength: (Double) -> Double, subLength: (Double) -> Double) {
        val count = 997
        for (i in 1 until count) {
            val length = segmentLength * i / count
            assertEquals(length, subLength(tForLength(length)), segmentLength * 1E-4, "arc length at t for length $length")
        }
    }

    @Test
    fun tForLengthOfACubicSegment2D() {
        assertArcLengths(cubic2.length, cubic2::tForLength) { cubic2.sub(0.0, it).length }
    }

    @Test
    fun tForLengthOfAQuadraticSegment2D() {
        assertArcLengths(quadratic2.length, quadratic2::tForLength) { quadratic2.sub(0.0, it).length }
    }

    @Test
    fun tForLengthOfACubicSegment3D() {
        assertArcLengths(cubic3.length, cubic3::tForLength) { cubic3.sub(0.0, it).length }
    }

    @Test
    fun evenLengthsGiveEvenSpacing() {
        // positions at even arc lengths along a curve are evenly spaced
        val count = 250
        val positions = (0..count).map { cubic2.position(cubic2.tForLength(cubic2.length * it / count)) }
        val distances = positions.zipWithNext { a, b -> a.distanceTo(b) }
        val expected = cubic2.length / count
        // each position is within the arc length tolerance of its target, spacings within twice that
        for (distance in distances) {
            assertEquals(expected, distance, cubic2.length * 2E-4)
        }
    }
}

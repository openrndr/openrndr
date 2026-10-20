import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArcLengthTest {

    private fun chordLength2(s: Segment2D, steps: Int) =
        (0 until steps).sumOf { s.position(it.toDouble() / steps).distanceTo(s.position((it + 1.0) / steps)) }

    private fun chordLength3(s: Segment3D, steps: Int) =
        (0 until steps).sumOf { s.position(it.toDouble() / steps).distanceTo(s.position((it + 1.0) / steps)) }

    /**
     * The chord length of a smooth curve converges with h², Richardson extrapolation of two chord lengths
     * cancels that term. Keeps the reference cheap enough for the JS test timeouts.
     */
    private fun referenceLength2(s: Segment2D, steps: Int = 4000) =
        (4.0 * chordLength2(s, 2 * steps) - chordLength2(s, steps)) / 3.0

    private fun referenceLength3(s: Segment3D, steps: Int = 4000) =
        (4.0 * chordLength3(s, 2 * steps) - chordLength3(s, steps)) / 3.0

    @Test
    fun circleLengthIsAccurateAtAnyScale() {
        for (r in listOf(0.01, 5.0, 10.0, 80.0, 1000.0, 1E6)) {
            val contour = Circle(Vector2.ZERO, r).contour
            val reference = contour.segments.sumOf { referenceLength2(it) }
            assertEquals(reference, contour.length, reference * 1E-8)
            // the Bézier approximation of a circle is within 0.03% of its true circumference
            assertEquals(2 * PI * r, contour.length, 2 * PI * r * 3E-4)
        }
    }

    @Test
    fun randomSegmentLengths() {
        val random = Random(1234)
        fun r2() = Vector2(random.nextDouble(-500.0, 500.0), random.nextDouble(-500.0, 500.0))
        fun r3() = Vector3(random.nextDouble(-500.0, 500.0), random.nextDouble(-500.0, 500.0), random.nextDouble(-500.0, 500.0))
        repeat(50) {
            for (s in listOf(Segment2D(r2(), r2(), r2(), r2()), Segment2D(r2(), r2(), r2()))) {
                val reference = referenceLength2(s)
                assertEquals(reference, s.length, reference * 1E-8)
            }
            for (s in listOf(Segment3D(r3(), r3(), r3(), r3()), Segment3D(r3(), r3(), r3()))) {
                val reference = referenceLength3(s)
                assertEquals(reference, s.length, reference * 1E-8)
            }
        }
    }

    @Test
    fun cuspAndBacktrackingLengths() {
        val cusp = Segment2D(Vector2(0.0, 0.0), Vector2(300.0, 300.0), Vector2(0.0, 300.0), Vector2(300.0, 0.0))
        assertEquals(chordLength2(cusp, 1_000_000), cusp.length, 1E-6)

        // runs from 0 to ~171.4, back to ~-28.6 and on to 300
        val backtrack = Segment2D(Vector2(0.0, 0.0), Vector2(400.0, 0.0), Vector2(-100.0, 0.0), Vector2(300.0, 0.0))
        assertEquals(chordLength2(backtrack, 1_000_000), backtrack.length, 1E-6)
        assertTrue(backtrack.length > 300.0)

        // x(t) = 800t - 500t², runs from 0 to 320 at t = 0.8 and back to 300
        val quadBacktrack = Segment2D(Vector2(0.0, 0.0), Vector2(400.0, 0.0), Vector2(300.0, 0.0))
        assertEquals(340.0, quadBacktrack.length, 1E-6)
    }

    @Test
    fun degenerateLengths() {
        val p = Vector2(5.0, 5.0)
        assertEquals(0.0, Segment2D(p, p, p, p).length)
        assertEquals(0.0, Segment2D(p, p, p).length)
        assertEquals(0.0, Segment3D(Vector3.ONE, Vector3.ONE, Vector3.ONE, Vector3.ONE).length)
        // a straight cubic with control points on its chord
        val straight = Segment2D(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(200.0, 0.0), Vector2(300.0, 0.0))
        assertTrue(abs(straight.length - 300.0) < 1E-9)
    }
}

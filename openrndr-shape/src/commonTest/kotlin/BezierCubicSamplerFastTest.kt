import org.openrndr.math.EuclideanVector
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.internal.numQuadratics
import org.openrndr.shape.internal.toQuadratics
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Maximum distance between the curve and the polyline through [samples], measured at points
 * between consecutive samples.
 */
internal fun <T : EuclideanVector<T>> maxDeviation(samples: List<Pair<T, Double>>, position: (Double) -> T): Double {
    var result = 0.0
    for ((a, b) in samples.zipWithNext()) {
        val ab = b.first - a.first
        for (k in 1 until 16) {
            val p = position(a.second + (b.second - a.second) * k / 16.0)
            val u = if (ab.squaredLength == 0.0) 0.0 else ((p - a.first).dot(ab) / ab.squaredLength).coerceIn(0.0, 1.0)
            result = maxOf(result, p.distanceTo(a.first + ab * u))
        }
    }
    return result
}

internal fun <T : EuclideanVector<T>> assertValidFlattening(samples: List<Pair<T, Double>>, start: T, end: T) {
    assertTrue(samples.size >= 2)
    assertEquals(0.0, samples.first().second)
    assertEquals(1.0, samples.last().second)
    assertEquals(0.0, samples.first().first.distanceTo(start), 1E-9)
    assertEquals(0.0, samples.last().first.distanceTo(end), 1E-9)
    assertTrue(samples.zipWithNext().all { (a, b) -> a.second <= b.second })
}

class BezierCubicSamplerFastTest {
    private val tolerances = listOf(0.03, 0.1, 0.5, 2.0)

    @Test
    fun testCubicToQuadratics3D() {
        val s = Segment3D(Vector3.ZERO, Vector3.ONE * 100.0, Vector3.ONE * 105.0, Vector3(0.0, 1.0, 1.0) * 100.0)
        val quads = s.toQuadratics(0.01)
        assertEquals(s.numQuadratics(0.01), quads.size)
        assertEquals(s.start, quads.first().start)
        assertEquals(s.end, quads.last().end)
        assertTrue(quads.all { it.control.size == 1 })
    }

    @Test
    fun testGentleCubic3D() {
        // needs only a single quadratic
        val s = Segment3D(Vector3.ZERO, Vector3(10.0, 1.0, 0.0), Vector3(20.0, 1.0, 0.0), Vector3(30.0, 0.0, 0.0))
        assertEquals(1, s.numQuadratics(0.05))
        assertValidFlattening(s.adaptivePositionsWithT(0.5), s.start, s.end)
        assertTrue(s.length > 30.0)
    }

    @Test
    fun testCubicWithinTolerance2D() {
        val random = Random(1234)
        fun r() = Vector2(random.nextDouble(0.0, 1000.0), random.nextDouble(0.0, 1000.0))
        val segments = List(500) { Segment2D(r(), r(), r(), r()) } + listOf(
            Segment2D(Vector2(0.0, 0.0), Vector2(300.0, 300.0), Vector2(0.0, 300.0), Vector2(300.0, 0.0)),
            Segment2D(Vector2(0.0, 0.0), Vector2(0.0, 0.0), Vector2(300.0, 300.0), Vector2(300.0, 0.0)),
            Segment2D(Vector2(0.0, 0.0), Vector2(300.0, 0.0), Vector2(300.0, 300.0), Vector2(0.0, 0.0)),
        )
        for (tolerance in tolerances) {
            for (s in segments) {
                val samples = s.adaptivePositionsWithT(tolerance)
                assertValidFlattening(samples, s.start, s.end)
                assertTrue(maxDeviation(samples, s::position) <= tolerance, "$s exceeds $tolerance")
            }
        }
    }

    @Test
    fun testCubicWithinTolerance3D() {
        val random = Random(1234)
        fun r() = Vector3(random.nextDouble(0.0, 1000.0), random.nextDouble(0.0, 1000.0), random.nextDouble(0.0, 1000.0))
        val segments = List(500) { Segment3D(r(), r(), r(), r()) }
        for (tolerance in tolerances) {
            for (s in segments) {
                val samples = s.adaptivePositionsWithT(tolerance)
                assertValidFlattening(samples, s.start, s.end)
                assertTrue(maxDeviation(samples, s::position) <= tolerance, "$s exceeds $tolerance")
            }
        }
    }

    @Test
    fun testCollinearBacktrackingCubic() {
        val s = Segment2D(Vector2(0.0, 0.0), Vector2(400.0, 0.0), Vector2(-100.0, 0.0), Vector2(300.0, 0.0))
        val samples = s.adaptivePositionsWithT(0.5)
        assertValidFlattening(samples, s.start, s.end)
        assertTrue(maxDeviation(samples, s::position) <= 0.5)
    }

    @Test
    fun testDegenerateCubic() {
        val p = Vector2(5.0, 5.0)
        val s = Segment2D(p, p, p, p)
        assertValidFlattening(s.adaptivePositionsWithT(0.5), p, p)
        assertValidFlattening(s.adaptivePositionsWithT(0.0), p, p)
    }
}

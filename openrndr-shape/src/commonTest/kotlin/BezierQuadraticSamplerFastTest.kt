import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.internal.quadBez3dTo2d
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BezierQuadraticSamplerFastTest {
    private val tolerances = listOf(0.03, 0.1, 0.5, 2.0)

    @Test
    fun testBezier3Dto2D() {
        val s = Segment3D(Vector3.ZERO, Vector3.ONE * 100.0, Vector3(0.0, 1.0, 1.0) * 100.0)
        val s2d = quadBez3dTo2d(s)
        assertEquals(s.length, s2d.length, 1E-8)
    }

    @Test
    fun testCollinearBezier3Dto2D() {
        val s = Segment3D(Vector3.ZERO, Vector3.ONE * 400.0, Vector3.ONE * 300.0)
        val s2d = quadBez3dTo2d(s)
        assertTrue(listOf(s2d.start, s2d.control[0], s2d.end).all { it.y == 0.0 && !it.x.isNaN() })
        assertEquals(s.length, s2d.length, 1E-8)
    }

    @Test
    fun testCuspQuadraticWithinTolerance() {
        // the curvature maximum lies between start and end
        val s = Segment2D(
            Vector2(469.72765408060025, 408.6636166923689),
            Vector2(395.1049812385385, 354.76341257950537),
            Vector2(954.3457578426465, 769.7671241593492)
        )
        for (tolerance in tolerances) {
            val samples = s.adaptivePositionsWithT(tolerance)
            assertValidFlattening(samples, s.start, s.end)
            assertTrue(maxDeviation(samples, s::position) <= tolerance)
        }
    }

    @Test
    fun testQuadraticWithinTolerance2D() {
        val random = Random(1234)
        fun r() = Vector2(random.nextDouble(0.0, 1000.0), random.nextDouble(0.0, 1000.0))
        val segments = List(500) { Segment2D(r(), r(), r()) }
        for (tolerance in tolerances) {
            for (s in segments) {
                val samples = s.adaptivePositionsWithT(tolerance)
                assertValidFlattening(samples, s.start, s.end)
                assertTrue(maxDeviation(samples, s::position) <= tolerance, "$s exceeds $tolerance")
            }
        }
    }

    @Test
    fun testQuadraticWithinTolerance3D() {
        val random = Random(1234)
        fun r() = Vector3(random.nextDouble(0.0, 1000.0), random.nextDouble(0.0, 1000.0), random.nextDouble(0.0, 1000.0))
        val segments = List(500) { Segment3D(r(), r(), r()) }
        for (tolerance in tolerances) {
            for (s in segments) {
                val samples = s.adaptivePositionsWithT(tolerance)
                assertValidFlattening(samples, s.start, s.end)
                assertTrue(maxDeviation(samples, s::position) <= tolerance, "$s exceeds $tolerance")
            }
        }
    }

    @Test
    fun testCollinearBacktrackingQuadratic() {
        val s2 = Segment2D(Vector2(0.0, 0.0), Vector2(400.0, 400.0), Vector2(300.0, 300.0))
        val samples2 = s2.adaptivePositionsWithT(0.5)
        assertValidFlattening(samples2, s2.start, s2.end)
        assertTrue(maxDeviation(samples2, s2::position) <= 0.5)

        val s3 = Segment3D(Vector3.ZERO, Vector3.ONE * 400.0, Vector3.ONE * 300.0)
        val samples3 = s3.adaptivePositionsWithT(0.5)
        assertValidFlattening(samples3, s3.start, s3.end)
        assertTrue(maxDeviation(samples3, s3::position) <= 0.5)
    }
}

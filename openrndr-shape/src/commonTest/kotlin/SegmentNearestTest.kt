import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SegmentNearestTest {

    /** the distance to the closest of [samples] + 1 evenly spaced points on [segment] */
    private fun sampledDistance(segment: Segment2D, point: Vector2, samples: Int = 20000): Double =
        (0..samples).minOf { segment.position(it.toDouble() / samples).distanceTo(point) }

    private fun assertNearest(segment: Segment2D, point: Vector2, samples: Int = 20000) {
        val nearest = segment.nearest(point)
        val distance = nearest.position.distanceTo(point)
        // sampling never finds a point closer than the nearest one, fewer samples keep the test fast on JS
        val sampled = sampledDistance(segment, point, samples)
        assertTrue(distance <= sampled + 1e-9, "$segment $point: nearest at $distance, sampled $sampled")
        assertEquals(segment.position(nearest.segmentT), nearest.position)
    }

    @Test
    fun nearestOnALoopingCubic() {
        val segment = Segment2D(
            Vector2(286.8167847416503, 50.86050659939687),
            Vector2(607.8160896347852, -797.1293681858556),
            Vector2(-680.0347422917145, 164.15297099010718),
            Vector2(84.59934330858259, 243.23927045473815)
        )
        assertNearest(segment, Vector2(91.62016544685645, -165.73752700826546))
        assertNearest(segment, Vector2(17.80933458608436, -221.0234146377565))
        assertNearest(segment, Vector2(108.99741359338736, -57.000288445706985))
    }


    @Test
    fun nearestOnRandomCurves() {
        val random = Random(1)
        fun v(range: Double) = Vector2(random.nextDouble(-range, range), random.nextDouble(-range, range))
        repeat(100) {
            val cubic = Segment2D(v(400.0), v(800.0), v(800.0), v(400.0))
            val quadratic = Segment2D(v(400.0), v(800.0), v(400.0))
            repeat(5) {
                val point = v(500.0)
                assertNearest(cubic, point, 2000)
                assertNearest(quadratic, point, 2000)
            }
        }
    }
}
